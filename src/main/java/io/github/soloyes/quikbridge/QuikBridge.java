package io.github.soloyes.quikbridge;

import io.github.soloyes.quikbridge.api.QuikApi;
import io.github.soloyes.quikbridge.bus.EventBus;
import io.github.soloyes.quikbridge.bus.Subscriber;
import io.github.soloyes.quikbridge.config.BridgeConfig;
import io.github.soloyes.quikbridge.connection.ConnectionState;
import io.github.soloyes.quikbridge.connection.Connector;
import io.github.soloyes.quikbridge.connection.Heartbeat;
import io.github.soloyes.quikbridge.connection.Sleeper;
import io.github.soloyes.quikbridge.events.ConnectRequested;
import io.github.soloyes.quikbridge.subscribers.CallbackSubscriber;
import io.github.soloyes.quikbridge.subscribers.ConnectionSubscriber;
import io.github.soloyes.quikbridge.subscribers.OrderSubscriber;
import io.github.soloyes.quikbridge.subscription.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Wires the service together and owns its lifecycle. Plain constructor injection: the native API,
 * the bus, the thread factory and the sleeper are passed in, so tests run the whole flow on a fake API.
 *
 * <pre>
 * start():  register subscribers -> ConnectRequested -> connect loop -> DllConnected
 *           -> status callback + orders/trades subscription; heartbeat every heartbeat.interval.ms
 * stop():   heartbeat off -> bus stopped (connect loop interrupted) -> unsubscribe -> TRANS2QUIK_DISCONNECT
 * </pre>
 */
public final class QuikBridge {
	private static final Logger log = LoggerFactory.getLogger(QuikBridge.class);

	private final BridgeConfig config;
	private final QuikApi api;
	private final EventBus bus;
	private final Connector connector;
	private final ConnectionSubscriber connection;
	private final CallbackSubscriber callbacks;
	private final OrderSubscriber orders;
	private final ScheduledExecutorService heartbeatExecutor;
	private final Heartbeat heartbeat;

	private final AtomicBoolean started = new AtomicBoolean();
	private final AtomicBoolean stopped = new AtomicBoolean();
	private final CountDownLatch terminated = new CountDownLatch(1);
	private final AtomicReference<String> fatal = new AtomicReference<>();

	/**
	 * @param executors creates the single-thread executor of each subscriber by name
	 */
	public QuikBridge(BridgeConfig config, List<Subscription> plan, QuikApi api, EventBus bus,
					  Function<String, ExecutorService> executors, Sleeper sleeper) {
		this.config = config;
		this.api = api;
		this.bus = bus;
		String quikPath = config.quikPath();
		this.connector = new Connector(api, quikPath, config.reconnectIntervalMs(), config.reconnectMaxAttempts(), sleeper);
		this.connection = new ConnectionSubscriber(connector, bus, quikPath, executors.apply("quik-connect"), this::fail);
		this.callbacks = new CallbackSubscriber(api, bus, executors.apply("quik-callbacks"));
		this.orders = new OrderSubscriber(api, bus, plan, config.subscribeOrders(), config.subscribeTrades(),
			executors.apply("quik-subscribe"));
		this.heartbeat = new Heartbeat(connector, bus, connection::state, connection::isConnecting);
		this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "quik-heartbeat");
			t.setDaemon(true);
			return t;
		});
	}

	/** Production executors: one named non-daemon thread per subscriber. */
	public static ExecutorService namedSingleThread(String name) {
		return Executors.newSingleThreadExecutor(r -> new Thread(r, name));
	}

	/** Registers an extra subscriber, e.g. a sink for order and trade events. */
	public QuikBridge with(Subscriber subscriber) {
		bus.register(subscriber);
		return this;
	}

	public void start() {
		if (!started.compareAndSet(false, true)) {
			throw new IllegalStateException("Already started");
		}
		bus.register(connection);
		bus.register(callbacks);
		bus.register(orders);
		bus.dispatch(new ConnectRequested("startup"));
		long hb = config.heartbeatIntervalMs();
		if (hb > 0) {
			heartbeatExecutor.scheduleWithFixedDelay(this::safeHeartbeat, hb, hb, TimeUnit.MILLISECONDS);
		}
	}

	private void safeHeartbeat() {
		try {
			heartbeat.run();
		} catch (RuntimeException e) {
			log.warn("Heartbeat failed", e);
		}
	}

	/** Runs one heartbeat check now (used by tests and for diagnostics). */
	public void heartbeatNow() {
		heartbeat.run();
	}

	private void fail(String reason) {
		if (fatal.compareAndSet(null, reason)) {
			log.error("Fatal: {}", reason);
		}
		terminated.countDown();
	}

	/**
	 * Waits until a fatal error, {@link #stop()} or the timeout.
	 *
	 * @param timeoutMs 0 = no timeout
	 * @return the fatal reason, or null if none happened
	 */
	public String awaitTermination(long timeoutMs) throws InterruptedException {
		if (timeoutMs == 0) {
			terminated.await();
		} else {
			terminated.await(timeoutMs, TimeUnit.MILLISECONDS);
		}
		return fatal.get();
	}

	public ConnectionState state() {
		return connection.state();
	}

	public boolean isSubscribed() {
		return orders.isSubscribed();
	}

	public String fatalReason() {
		return fatal.get();
	}

	/** Graceful shutdown. Idempotent, safe from a shutdown hook. */
	public void stop() {
		if (!stopped.compareAndSet(false, true)) {
			return;
		}
		log.info("Stopping");
		heartbeatExecutor.shutdownNow();
		bus.stop();
		try {
			if (connector.isDllConnected()) {
				if (orders.isSubscribed()) {
					orders.unsubscribe();
				}
				connector.disconnect();
			}
		} catch (RuntimeException | Error e) {
			log.warn("Error while disconnecting", e);
		}
		terminated.countDown();
		log.info("Stopped");
	}
}
