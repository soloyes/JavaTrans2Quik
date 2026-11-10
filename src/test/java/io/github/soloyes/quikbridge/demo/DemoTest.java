package io.github.soloyes.quikbridge.demo;

import io.github.soloyes.quikbridge.bus.Event;
import io.github.soloyes.quikbridge.bus.Subscriber;
import io.github.soloyes.quikbridge.events.OrderEvent;
import io.github.soloyes.quikbridge.events.TradeEvent;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The --demo script runs through connect, terminal restart and shutdown on real threads. */
class DemoTest {

	@Test
	void demoScriptCompletesWithExitCodeZero() throws Exception {
		SimulatedQuikApi api = new SimulatedQuikApi();
		AtomicInteger orders = new AtomicInteger();
		AtomicInteger trades = new AtomicInteger();
		Subscriber counter = new Subscriber() {
			@Override public Set<Class<? extends Event<?>>> supports() { return Set.of(OrderEvent.class, TradeEvent.class); }
			@Override public void handle(Event<?> e) { (e instanceof OrderEvent ? orders : trades).incrementAndGet(); }
		};

		int exit = new Demo(api, 0, counter).run();

		assertEquals(0, exit);
		assertEquals(3, api.count("connect"), "failed attempt, connect, reconnect after the simulated restart");
		assertEquals(2, api.count("startOrders"), "subscribed after each connect");
		assertEquals(2, api.count("startTrades"));
		assertEquals(11, orders.get());
		assertEquals(4, trades.get());
		assertTrue(api.calls.indexOf("unsubscribeOrders") < api.calls.lastIndexOf("disconnect"));
		assertFalse(api.dllConnected(), "stopped with TRANS2QUIK_DISCONNECT");
	}

	@Test
	void demoConfigUsesTheExampleBundles() {
		Demo demo = new Demo(new SimulatedQuikApi(), 0);
		assertEquals(Demo.QUIK_PATH, demo.config().quikPath());
		assertEquals(0, demo.config().reconnectIntervalMs());
		assertEquals("TQBR", demo.config().getString(io.github.soloyes.quikbridge.subscription.SubscriptionPlanner.CLASS_CODE_PREFIX + "shares"));
	}
}
