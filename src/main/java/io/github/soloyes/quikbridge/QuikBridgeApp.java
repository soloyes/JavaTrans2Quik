package io.github.soloyes.quikbridge;

import io.github.soloyes.quikbridge.api.QuikApi;
import io.github.soloyes.quikbridge.bus.SimpleEventBus;
import io.github.soloyes.quikbridge.config.BridgeConfig;
import io.github.soloyes.quikbridge.config.ConfigException;
import io.github.soloyes.quikbridge.connection.Sleeper;
import io.github.soloyes.quikbridge.demo.Demo;
import io.github.soloyes.quikbridge.jna.JnaQuikApi;
import io.github.soloyes.quikbridge.subscribers.OrderTradeLogger;
import io.github.soloyes.quikbridge.subscription.Subscription;
import io.github.soloyes.quikbridge.subscription.SubscriptionPlanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Entry point: {@code java -jar JavaTrans2Quik-<version>-all.jar [path/to/app.properties]}.
 * {@code --demo} as the only argument runs {@link Demo} against a simulated terminal instead.
 * <p>
 * Exit codes: 0 normal stop, 1 fatal connection error, 2 configuration error, 3 TRANS2QUIK.dll not loadable.
 */
public final class QuikBridgeApp {

	private QuikBridgeApp() {
	}

	/** Created on first use so that {@link Demo} can pick its Logback config before logging starts. */
	private static Logger log() {
		return LoggerFactory.getLogger(QuikBridgeApp.class);
	}

	public static void main(String[] args) throws InterruptedException {
		if (args.length == 1 && Demo.FLAG.equals(args[0])) {
			System.exit(Demo.main(args));
			return;
		}

		BridgeConfig config;
		List<Subscription> plan;
		try {
			config = BridgeConfig.load(args.length > 0 ? args[0] : null);
			config.quikPath();
			plan = new SubscriptionPlanner(config).plan();
		} catch (ConfigException e) {
			log().error("Configuration error: {}", e.getMessage());
			System.exit(2);
			return;
		}

		QuikApi api;
		try {
			api = new JnaQuikApi();
		} catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
			log().error("Cannot load TRANS2QUIK.dll ({}). It ships with the QUIK terminal (Windows only); "
				+ "put it on PATH or pass -Djna.library.path=<folder>.", e.getMessage());
			System.exit(3);
			return;
		}

		QuikBridge bridge = new QuikBridge(config, plan, api, new SimpleEventBus(),
			QuikBridge::namedSingleThread, Sleeper.SYSTEM)
			.with(new OrderTradeLogger());
		Runtime.getRuntime().addShutdownHook(new Thread(bridge::stop, "quik-shutdown"));

		bridge.start();
		String fatal = bridge.awaitTermination(config.runDurationMs());
		bridge.stop();
		System.exit(fatal == null ? 0 : 1);
	}
}
