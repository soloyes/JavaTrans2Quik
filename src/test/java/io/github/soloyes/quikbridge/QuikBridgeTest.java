package io.github.soloyes.quikbridge;

import io.github.soloyes.quikbridge.api.CallResult;
import io.github.soloyes.quikbridge.api.OrderUpdate;
import io.github.soloyes.quikbridge.api.Status;
import io.github.soloyes.quikbridge.api.TradeUpdate;
import io.github.soloyes.quikbridge.bus.Event;
import io.github.soloyes.quikbridge.bus.SimpleEventBus;
import io.github.soloyes.quikbridge.bus.Subscriber;
import io.github.soloyes.quikbridge.config.BridgeConfig;
import io.github.soloyes.quikbridge.connection.ConnectionState;
import io.github.soloyes.quikbridge.demo.SimulatedQuikApi;
import io.github.soloyes.quikbridge.events.OrderEvent;
import io.github.soloyes.quikbridge.events.TradeEvent;
import io.github.soloyes.quikbridge.subscription.Subscription;
import io.github.soloyes.quikbridge.subscription.SubscriptionPlanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end flow of the service against {@link SimulatedQuikApi}. */
class QuikBridgeTest {
	private final SimulatedQuikApi api = new SimulatedQuikApi();
	private final List<Event<?>> sink = Collections.synchronizedList(new ArrayList<>());
	private QuikBridge bridge;

	@AfterEach
	void tearDown() {
		if (bridge != null) bridge.stop();
	}

	private static BridgeConfig config(String... extra) {
		Properties p = new Properties();
		p.setProperty("quik.path", "C:\\QUIK");
		p.setProperty("reconnect.interval.ms", "0");
		p.setProperty("heartbeat.interval.ms", "0");
		p.setProperty("subscription.names", "shares,futures");
		p.setProperty("subscription.class.code.shares", "TQBR");
		p.setProperty("subscription.sec.codes.shares", "LKOH,SBER");
		p.setProperty("subscription.class.code.futures", "SPBFUT");
		for (int i = 0; i < extra.length; i += 2) p.setProperty(extra[i], extra[i + 1]);
		return BridgeConfig.fromProperties(p);
	}

	private QuikBridge bridge(BridgeConfig cfg) {
		List<Subscription> plan = new SubscriptionPlanner(cfg).plan();
		bridge = new QuikBridge(cfg, plan, api, new SimpleEventBus(), name -> new DirectExecutorService(), ms -> { })
			.with(new Subscriber() {
				@Override public void handle(Event<?> e) { sink.add(e); }
				@Override public Set<Class<? extends Event<?>>> supports() { return Set.of(OrderEvent.class, TradeEvent.class); }
			});
		return bridge;
	}

	private static final List<String> SUBSCRIBE_SEQUENCE = List.of(
		"subscribeOrders TQBR LKOH|SBER", "subscribeTrades TQBR LKOH|SBER",
		"subscribeOrders SPBFUT ", "subscribeTrades SPBFUT ",
		"startOrders", "startTrades");

	@Test
	void startConnectsInstallsCallbackAndSubscribesPerPlan() {
		api.scriptConnect(CallResult.of(Status.QUIK_TERMINAL_NOT_FOUND));
		bridge(config()).start();

		assertEquals(ConnectionState.SERVER_CONNECTED, bridge.state());
		assertEquals(2, api.count("connect"));
		assertTrue(api.calls.contains("setConnectionStatusListener"));
		List<String> subs = new ArrayList<>(api.calls);
		subs.removeIf(c -> !c.startsWith("subscribe") && !c.startsWith("start"));
		assertEquals(SUBSCRIBE_SEQUENCE, subs);
		assertTrue(bridge.isSubscribed());
	}

	@Test
	void ordersOnlyWhenTradesAreDisabled() {
		bridge(config("subscribe.trades", "false")).start();
		assertEquals(0, api.count("subscribeTrades"));
		assertEquals(0, api.count("startTrades"));
		assertEquals(2, api.count("subscribeOrders"));
	}

	@Test
	void ordersAndTradesFromTheDllReachSubscribers() {
		bridge(config()).start();
		OrderUpdate order = new OrderUpdate(0, 17, 123456, "TQBR", "SBER", 300.5, 10, 3005, false, 1);
		TradeUpdate trade = new TradeUpdate(0, 999, 123456, "TQBR", "SBER", 300.5, 10, 3005, false);
		api.fireOrder(order);
		api.fireTrade(trade);

		assertEquals(2, sink.size());
		assertEquals(order, sink.get(0).getData());
		assertEquals(trade, sink.get(1).getData());
		assertTrue(order.isActive());
	}

	@Test
	void dllDisconnectTriggersReconnectAndResubscribe() {
		bridge(config()).start();
		api.calls.clear();

		api.dropDll();

		assertEquals(ConnectionState.SERVER_CONNECTED, bridge.state());
		assertEquals(1, api.count("connect"));
		List<String> expected = new ArrayList<>();
		expected.add("connect C:\\QUIK");
		expected.add("setConnectionStatusListener");
		expected.add("unsubscribeOrders");
		expected.add("unsubscribeTrades");
		expected.addAll(SUBSCRIBE_SEQUENCE);
		assertEquals(expected, api.calls);
	}

	@Test
	void serverStatusEventsMoveTheStateWithoutReconnecting() {
		bridge(config()).start();
		api.calls.clear();
		api.fireStatus(Status.QUIK_DISCONNECTED);
		assertEquals(ConnectionState.DLL_CONNECTED, bridge.state());
		api.fireStatus(Status.QUIK_CONNECTED);
		assertEquals(ConnectionState.SERVER_CONNECTED, bridge.state());
		assertEquals(0, api.count("connect"));
	}

	@Test
	void unsupportedDllVersionStopsWithAFatalReason() throws Exception {
		api.scriptConnect(CallResult.of(Status.DLL_VERSION_NOT_SUPPORTED));
		bridge(config()).start();
		assertNotNull(bridge.awaitTermination(1));
		assertTrue(bridge.fatalReason().contains("not supported"));
		assertEquals(ConnectionState.DLL_DISCONNECTED, bridge.state());
		assertEquals(0, api.count("subscribe"));
	}

	@Test
	void exhaustedAttemptsAreFatal() throws Exception {
		api.scriptConnect(CallResult.of(Status.FAILED), CallResult.of(Status.FAILED));
		bridge(config("reconnect.max.attempts", "2")).start();
		assertNotNull(bridge.awaitTermination(1));
		assertEquals(2, api.count("connect"));
	}

	@Test
	void heartbeatNoticesASilentDisconnectAndServerChanges() {
		bridge(config()).start();
		api.calls.clear();

		api.serverConnected(false);
		bridge.heartbeatNow();
		assertEquals(ConnectionState.DLL_CONNECTED, bridge.state());

		api.serverConnected(true);
		bridge.heartbeatNow();
		assertEquals(ConnectionState.SERVER_CONNECTED, bridge.state());
		assertEquals(0, api.count("connect"));

		// DLL detaches but the status callback never arrives
		api.calls.clear();
		api.disconnect();
		bridge.heartbeatNow();
		assertEquals(1, api.count("connect"));
		assertEquals(ConnectionState.SERVER_CONNECTED, bridge.state());
	}

	@Test
	void stopUnsubscribesAndDisconnects() {
		bridge(config()).start();
		api.calls.clear();
		bridge.stop();
		assertEquals(List.of("unsubscribeOrders", "unsubscribeTrades", "disconnect"), api.calls);
		assertFalse(api.dllConnected());
		bridge.stop();
		assertEquals(3, api.calls.size(), "stop is idempotent");
	}

	@Test
	void realThreadsReconnectWithoutDeadlock() throws Exception {
		BridgeConfig cfg = config();
		bridge = new QuikBridge(cfg, new SubscriptionPlanner(cfg).plan(), api, new SimpleEventBus(),
			QuikBridge::namedSingleThread, ms -> Thread.sleep(1));
		api.scriptConnect(CallResult.of(Status.FAILED), CallResult.of(Status.QUIK_TERMINAL_NOT_FOUND));
		bridge.start();
		waitFor(() -> api.count("startTrades") == 1);

		for (int i = 0; i < 5; i++) {
			long before = api.count("startTrades");
			api.dropDll();
			api.dropDll(); // a second drop, possibly while the first reconnect is still running
			waitFor(() -> api.count("startTrades") > before && api.dllConnected()
				&& bridge.state() == ConnectionState.SERVER_CONNECTED);
		}
		assertNull(bridge.fatalReason());
		bridge.stop();
		assertFalse(api.dllConnected());
	}

	private static void waitFor(BooleanSupplier condition) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 5000;
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline) throw new AssertionError("condition not met in 5 s");
			Thread.sleep(5);
		}
	}
}
