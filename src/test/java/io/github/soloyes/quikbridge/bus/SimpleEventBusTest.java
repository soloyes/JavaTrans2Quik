package io.github.soloyes.quikbridge.bus;

import io.github.soloyes.quikbridge.events.BaseEvent;
import io.github.soloyes.quikbridge.events.ConnectRequested;
import io.github.soloyes.quikbridge.events.DllConnected;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleEventBusTest {

	static class Recorder implements Subscriber {
		final List<Event<?>> seen = Collections.synchronizedList(new ArrayList<>());
		final Set<Class<? extends Event<?>>> types;
		int shutdowns;

		@SafeVarargs
		Recorder(Class<? extends Event<?>>... types) {
			this.types = Set.of(types);
		}

		@Override public void handle(Event<?> event) { seen.add(event); }
		@Override public Set<Class<? extends Event<?>>> supports() { return types; }
		@Override public void shutdown() { shutdowns++; }
	}

	/** Subclass of a supported type: must be routed too. */
	static final class Special extends BaseEvent<String> {
		Special() { super("x"); }
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Class<? extends Event<?>> baseEvent() {
		return (Class) BaseEvent.class;
	}

	@Test
	void routesOnlySupportedTypes() {
		SimpleEventBus bus = new SimpleEventBus();
		Recorder connects = new Recorder(ConnectRequested.class);
		Recorder dll = new Recorder(DllConnected.class);
		bus.register(connects);
		bus.register(dll);

		ConnectRequested c = new ConnectRequested("test");
		DllConnected d = new DllConnected("C:\\QUIK");
		bus.dispatch(c);
		bus.dispatch(d);

		assertEquals(List.of(c), connects.seen);
		assertEquals(List.of(d), dll.seen);
	}

	@Test
	void routesSubclassesOfSupportedTypes() {
		SimpleEventBus bus = new SimpleEventBus();
		Recorder all = new Recorder(baseEvent());
		bus.register(all);
		bus.dispatch(new Special());
		bus.dispatch(new ConnectRequested("x"));
		assertEquals(2, all.seen.size());
	}

	@Test
	void failingSubscriberDoesNotStopDelivery() {
		SimpleEventBus bus = new SimpleEventBus();
		bus.register(new Subscriber() {
			@Override public void handle(Event<?> event) { throw new IllegalStateException("boom"); }
			@Override public Set<Class<? extends Event<?>>> supports() { return Set.of(ConnectRequested.class); }
		});
		Recorder after = new Recorder(ConnectRequested.class);
		bus.register(after);
		bus.dispatch(new ConnectRequested("x"));
		assertEquals(1, after.seen.size());
	}

	@Test
	void stopShutsDownOnceAndDropsLaterEvents() {
		SimpleEventBus bus = new SimpleEventBus();
		Recorder r = new Recorder(ConnectRequested.class);
		bus.register(r);
		bus.stop();
		bus.stop();
		bus.dispatch(new ConnectRequested("late"));
		assertEquals(1, r.shutdowns);
		assertEquals(0, r.seen.size());
		assertThrows(IllegalStateException.class, () -> bus.register(new Recorder()));
	}

	@Test
	void concurrentRegisterAndDispatchIsSafe() throws Exception {
		SimpleEventBus bus = new SimpleEventBus();
		AtomicInteger delivered = new AtomicInteger();
		AtomicReference<Throwable> error = new AtomicReference<>();
		CountDownLatch go = new CountDownLatch(1);
		Thread dispatcher = new Thread(() -> {
			try {
				go.await();
				for (int i = 0; i < 20_000; i++) bus.dispatch(new ConnectRequested("x"));
			} catch (Throwable t) {
				error.set(t);
			}
		});
		dispatcher.start();
		go.countDown();
		for (int i = 0; i < 500; i++) {
			bus.register(new Subscriber() {
				@Override public void handle(Event<?> event) { delivered.incrementAndGet(); }
				@Override public Set<Class<? extends Event<?>>> supports() { return Set.of(ConnectRequested.class); }
			});
		}
		dispatcher.join();
		assertNull(error.get());
		assertEquals(500, bus.getSubscribers().size());
	}
}
