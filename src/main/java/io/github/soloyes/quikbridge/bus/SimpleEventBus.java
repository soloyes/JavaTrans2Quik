package io.github.soloyes.quikbridge.bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe {@link EventBus}: subscribers live in a {@link CopyOnWriteArrayList}, so DLL callback threads
 * can dispatch while the main thread registers. A failing subscriber is logged and does not stop delivery
 * to the others.
 */
public final class SimpleEventBus implements EventBus {
	private static final Logger log = LoggerFactory.getLogger(SimpleEventBus.class);

	private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();
	private final AtomicBoolean stopped = new AtomicBoolean();

	@Override
	public void register(Subscriber subscriber) {
		if (stopped.get()) {
			throw new IllegalStateException("Bus is stopped");
		}
		subscribers.add(subscriber);
	}

	@Override
	public void dispatch(Event<?> event) {
		if (stopped.get()) {
			log.debug("Bus stopped, dropping {}", event);
			return;
		}
		log.debug("Dispatch {}", event);
		for (Subscriber s : subscribers) {
			if (supports(s, event)) {
				try {
					s.handle(event);
				} catch (RuntimeException e) {
					log.error("Subscriber {} failed on {}", s.getClass().getSimpleName(), event, e);
				}
			}
		}
	}

	private static boolean supports(Subscriber s, Event<?> event) {
		for (Class<?> type : s.supports()) {
			if (type.isInstance(event)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<Subscriber> getSubscribers() {
		return List.copyOf(subscribers);
	}

	@Override
	public void stop() {
		if (!stopped.compareAndSet(false, true)) {
			return;
		}
		for (Subscriber s : subscribers) {
			try {
				s.shutdown();
			} catch (RuntimeException e) {
				log.warn("Shutdown of {} failed", s.getClass().getSimpleName(), e);
			}
		}
	}
}
