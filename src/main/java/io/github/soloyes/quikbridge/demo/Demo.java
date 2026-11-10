package io.github.soloyes.quikbridge.demo;

import io.github.soloyes.quikbridge.QuikBridge;
import io.github.soloyes.quikbridge.api.CallResult;
import io.github.soloyes.quikbridge.api.OrderUpdate;
import io.github.soloyes.quikbridge.api.Status;
import io.github.soloyes.quikbridge.api.TradeUpdate;
import io.github.soloyes.quikbridge.bus.SimpleEventBus;
import io.github.soloyes.quikbridge.bus.Subscriber;
import io.github.soloyes.quikbridge.config.BridgeConfig;
import io.github.soloyes.quikbridge.connection.ConnectionState;
import io.github.soloyes.quikbridge.connection.Sleeper;
import io.github.soloyes.quikbridge.subscribers.OrderTradeLogger;
import io.github.soloyes.quikbridge.subscription.SubscriptionPlanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.function.BooleanSupplier;

/**
 * {@code java -jar JavaTrans2Quik-<version>-all.jar --demo}: runs the real {@link QuikBridge} wiring against
 * {@link SimulatedQuikApi} instead of TRANS2QUIK.dll, so the service can be watched on a machine without
 * Windows or a QUIK terminal.
 * <p>
 * Everything the simulated terminal does is scripted here: the first connect attempt fails, the second
 * succeeds, open orders are replayed, a new order is placed and filled, the terminal restarts (DLL_DISCONNECTED,
 * reconnect, resubscribe, replay), a few more orders and fills arrive, then the bridge shuts down. No real
 * order, account or exchange is involved; instrument codes and prices are made up to look like MOEX.
 * <p>
 * The pace between steps is {@code -Dquikbridge.demo.pace.ms} (default 400); the test suite runs it with 0.
 */
public final class Demo {
	public static final String FLAG = "--demo";
	public static final String PACE_PROPERTY = "quikbridge.demo.pace.ms";
	public static final long DEFAULT_PACE_MS = 400;

	/** Simulated terminal folder; it does not exist and is never opened. */
	static final String QUIK_PATH = "C:\\QUIK (simulated)";

	private static final String BANNER =
		"==== DEMO MODE: simulated terminal, no TRANS2QUIK.dll, no QUIK; orders and trades are scripted ====";

	private final Logger log = LoggerFactory.getLogger("demo");
	private final SimulatedQuikApi api;
	private final long paceMs;
	private final Subscriber[] extraSubscribers;

	/**
	 * @param paceMs           pause between scripted steps, 0 = as fast as the threads allow
	 * @param extraSubscribers registered on the bus besides {@link OrderTradeLogger} (tests use it to count events)
	 */
	public Demo(SimulatedQuikApi api, long paceMs, Subscriber... extraSubscribers) {
		this.api = api;
		this.paceMs = paceMs;
		this.extraSubscribers = extraSubscribers;
	}

	/** Entry point used by {@code QuikBridgeApp} for {@code --demo}. Returns the exit code. */
	public static int main(String[] args) throws InterruptedException {
		configureLogging();
		long pace = Long.getLong(PACE_PROPERTY, DEFAULT_PACE_MS);
		return new Demo(new SimulatedQuikApi(), pace).run();
	}

	/**
	 * Console-only, coloured log layout for the demo unless the user chose a Logback config. Old Windows consoles
	 * do not render ANSI colours, so there the plain variant is used (Windows Terminal sets WT_SESSION).
	 */
	private static void configureLogging() {
		if (System.getProperty("logback.configurationFile") != null) {
			return;
		}
		boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
		boolean colour = !windows || System.getenv("WT_SESSION") != null || System.getenv("ANSICON") != null;
		System.setProperty("logback.configurationFile", colour ? "logback-demo.xml" : "logback-demo-plain.xml");
	}

	/** Configuration of the demo run: the example bundles from app.properties.example. */
	public BridgeConfig config() {
		Properties p = new Properties();
		p.setProperty(BridgeConfig.QUIK_PATH, QUIK_PATH);
		p.setProperty(BridgeConfig.RECONNECT_INTERVAL_MS, Long.toString(paceMs));
		p.setProperty(BridgeConfig.HEARTBEAT_INTERVAL_MS, Long.toString(paceMs * 5));
		p.setProperty(SubscriptionPlanner.NAMES, "shares,futures");
		p.setProperty(SubscriptionPlanner.CLASS_CODE_PREFIX + "shares", "TQBR");
		p.setProperty(SubscriptionPlanner.SEC_CODES_PREFIX + "shares", "LKOH,GAZP,SBER");
		p.setProperty(SubscriptionPlanner.CLASS_CODE_PREFIX + "futures", "SPBFUT");
		return BridgeConfig.fromProperties(p);
	}

	/** Runs the whole script. Returns the process exit code: 0 when the bridge stopped cleanly. */
	public int run() throws InterruptedException {
		log.warn(BANNER);
		log.info("Terminal simulated by {}; the first TRANS2QUIK_CONNECT fails on purpose",
			SimulatedQuikApi.class.getSimpleName());
		api.scriptConnect(CallResult.of(Status.QUIK_TERMINAL_NOT_FOUND));

		BridgeConfig config = config();
		QuikBridge bridge = new QuikBridge(config, new SubscriptionPlanner(config).plan(), api, new SimpleEventBus(),
			QuikBridge::namedSingleThread, Sleeper.SYSTEM)
			.with(new OrderTradeLogger());
		for (Subscriber s : extraSubscribers) {
			bridge.with(s);
		}
		Thread hook = new Thread(bridge::stop, "quik-shutdown");
		Runtime.getRuntime().addShutdownHook(hook);
		try {
			bridge.start();
			waitFor(() -> api.count("startTrades") == 1 && bridge.isSubscribed()
				&& bridge.state() == ConnectionState.SERVER_CONNECTED);
			pause();

			step("terminal replays the account's open orders (mode 1, last one mode 2)");
			api.fireOrder(order(1, 101, 28831004512L, "TQBR", "SBER", 301.25, 20, 60250, false, ACTIVE));
			api.fireOrder(order(2, 102, 28831004519L, "TQBR", "LKOH", 7015.0, 3, 21045, false, ACTIVE));
			pause();

			step("a new sell order for GAZP is placed in the terminal");
			api.fireOrder(order(0, 103, 28831004731L, "TQBR", "GAZP", 128.40, 50, 64200, true, ACTIVE));
			pause();
			step("it fills in two trades");
			api.fireTrade(trade(9120034001L, 28831004731L, "TQBR", "GAZP", 128.40, 30, 38520, true));
			api.fireOrder(order(0, 103, 28831004731L, "TQBR", "GAZP", 128.40, 20, 64200, true, ACTIVE));
			pause();
			api.fireTrade(trade(9120034009L, 28831004731L, "TQBR", "GAZP", 128.40, 20, 25680, true));
			api.fireOrder(order(0, 103, 28831004731L, "TQBR", "GAZP", 128.40, 0, 64200, true, FILLED));
			pause();

			step("the terminal process restarts: the DLL reports DLL_DISCONNECTED");
			api.dropDll();
			waitFor(() -> api.count("startTrades") == 2 && bridge.isSubscribed()
				&& bridge.state() == ConnectionState.SERVER_CONNECTED);
			pause();

			step("after the reconnect the terminal replays the open orders again");
			api.fireOrder(order(1, 101, 28831004512L, "TQBR", "SBER", 301.25, 20, 60250, false, ACTIVE));
			api.fireOrder(order(2, 102, 28831004519L, "TQBR", "LKOH", 7015.0, 3, 21045, false, ACTIVE));
			pause();

			step("LKOH fills, SBER is cancelled");
			api.fireTrade(trade(9120034120L, 28831004519L, "TQBR", "LKOH", 7015.0, 3, 21045, false));
			api.fireOrder(order(0, 102, 28831004519L, "TQBR", "LKOH", 7015.0, 0, 21045, false, FILLED));
			pause();
			api.fireOrder(order(0, 101, 28831004512L, "TQBR", "SBER", 301.25, 20, 60250, false, CANCELLED));
			pause();

			step("a futures order on SPBFUT comes through the second subscription and fills at once");
			api.fireOrder(order(0, 104, 28831005002L, "SPBFUT", "SiZ6", 91850.0, 2, 183700, false, ACTIVE));
			api.fireTrade(trade(9120034188L, 28831005002L, "SPBFUT", "SiZ6", 91850.0, 2, 183700, false));
			api.fireOrder(order(0, 104, 28831005002L, "SPBFUT", "SiZ6", 91850.0, 0, 183700, false, FILLED));
			pause();

			step("script finished, stopping the bridge (unsubscribe, TRANS2QUIK_DISCONNECT)");
		} finally {
			bridge.stop();
			try {
				Runtime.getRuntime().removeShutdownHook(hook);
			} catch (IllegalStateException ignored) {
				// JVM already shutting down
			}
		}
		String fatal = bridge.fatalReason();
		if (fatal != null) {
			log.error("Demo ended with a fatal error: {}", fatal);
		}
		log.warn(BANNER);
		return fatal == null ? 0 : 1;
	}

	private static final long ACTIVE = 1;
	private static final long CANCELLED = 2;
	private static final long FILLED = 3;

	private static OrderUpdate order(long mode, long transId, long number, String classCode, String secCode,
									 double price, long balance, double value, boolean sell, long status) {
		return new OrderUpdate(mode, transId, number, classCode, secCode, price, balance, value, sell, status);
	}

	private static TradeUpdate trade(long number, long orderNumber, String classCode, String secCode,
									 double price, long quantity, double value, boolean sell) {
		return new TradeUpdate(0, number, orderNumber, classCode, secCode, price, quantity, value, sell);
	}

	private void step(String what) {
		log.info("[simulation] {}", what);
	}

	private void pause() throws InterruptedException {
		if (paceMs > 0) {
			Thread.sleep(paceMs);
		}
	}

	private static void waitFor(BooleanSupplier condition) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 10_000;
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline) {
				throw new IllegalStateException("The simulated bridge did not reach the expected state in 10 s");
			}
			Thread.sleep(5);
		}
	}
}
