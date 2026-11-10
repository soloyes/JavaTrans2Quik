package io.github.soloyes.quikbridge.subscribers;

import io.github.soloyes.quikbridge.api.QuikException;
import io.github.soloyes.quikbridge.api.Status;
import io.github.soloyes.quikbridge.bus.Event;
import io.github.soloyes.quikbridge.bus.EventBus;
import io.github.soloyes.quikbridge.connection.ConnectionState;
import io.github.soloyes.quikbridge.connection.Connector;
import io.github.soloyes.quikbridge.events.ConnectRequested;
import io.github.soloyes.quikbridge.events.ConnectionStatusChanged;
import io.github.soloyes.quikbridge.events.DllConnected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Owns the connection state machine.
 * <ul>
 *     <li>{@link ConnectRequested}: run the connect/retry loop on its own thread. Requests that arrive
 *     while a loop is already running are dropped, so a burst of disconnect events gives one reconnect.</li>
 *     <li>{@link ConnectionStatusChanged}: update {@link ConnectionState}; on DLL_DISCONNECTED start reconnecting.</li>
 * </ul>
 * After a successful connect it publishes {@link DllConnected}, which makes the other subscribers
 * (re)install the status callback and the order/trade subscriptions.
 */
public final class ConnectionSubscriber extends ExecutorSubscriber {
	private static final Logger log = LoggerFactory.getLogger(ConnectionSubscriber.class);

	private final Connector connector;
	private final EventBus bus;
	private final String quikPath;
	private final Consumer<String> onFatal;
	private final AtomicBoolean connecting = new AtomicBoolean();
	/** Set when DLL_DISCONNECTED arrives while a connect loop is running: its success may already be stale. */
	private final AtomicBoolean disconnectedWhileConnecting = new AtomicBoolean();
	private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DLL_DISCONNECTED);

	/**
	 * @param onFatal called with a reason when connecting cannot succeed (unsupported DLL, attempts exhausted)
	 */
	public ConnectionSubscriber(Connector connector, EventBus bus, String quikPath,
								ExecutorService executor, Consumer<String> onFatal) {
		super(executor);
		this.connector = connector;
		this.bus = bus;
		this.quikPath = quikPath;
		this.onFatal = onFatal;
	}

	@Override
	public Set<Class<? extends Event<?>>> supports() {
		return Set.of(ConnectRequested.class, ConnectionStatusChanged.class);
	}

	public ConnectionState state() {
		return state.get();
	}

	public boolean isConnecting() {
		return connecting.get();
	}

	@Override
	public void handle(Event<?> event) {
		if (event instanceof ConnectionStatusChanged) {
			onStatus(((ConnectionStatusChanged) event).getData());
		} else if (event instanceof ConnectRequested) {
			requestConnect(((ConnectRequested) event).getData());
		}
	}

	private void onStatus(Status status) {
		ConnectionState before = state.get();
		ConnectionState after = state.updateAndGet(s -> s.on(status));
		if (before != after) {
			log.info("Connection state {} -> {} ({})", before, after, status);
		}
		if (status == Status.DLL_DISCONNECTED) {
			requestConnect("DLL disconnected from the terminal");
		}
	}

	private void requestConnect(String reason) {
		if (state.get() != ConnectionState.DLL_DISCONNECTED) {
			log.debug("Connect request ignored, already {}: {}", state.get(), reason);
			return;
		}
		if (!connecting.compareAndSet(false, true)) {
			disconnectedWhileConnecting.set(true);
			log.debug("Connect already in progress, dropping request: {}", reason);
			return;
		}
		log.info("Connecting to QUIK: {}", reason);
		try {
			executor.execute(this::connectLoop);
		} catch (RejectedExecutionException e) {
			connecting.set(false);
			log.debug("Executor stopped, not connecting");
		}
	}

	private void connectLoop() {
		try {
			while (true) {
				disconnectedWhileConnecting.set(false);
				if (!connector.connect()) {
					if (!Thread.currentThread().isInterrupted()) {
						onFatal.accept("Could not connect to the QUIK terminal");
					}
					return;
				}
				if (disconnectedWhileConnecting.get() && !connector.isDllConnected()) {
					log.warn("Terminal dropped the DLL again while connecting, retrying");
					continue;
				}
				break;
			}
			state.set(connector.isServerConnected() ? ConnectionState.SERVER_CONNECTED : ConnectionState.DLL_CONNECTED);
			log.info("Connection state -> {}", state.get());
		} catch (QuikException e) {
			log.error(e.getMessage());
			onFatal.accept(e.getMessage());
			return;
		} finally {
			connecting.set(false);
		}
		// A DLL_DISCONNECTED that arrived between the stale check above and connecting=false was dropped
		// by requestConnect (a loop was still running). Without this re-check only the heartbeat would notice.
		if (disconnectedWhileConnecting.get() && !connector.isDllConnected()) {
			log.warn("Terminal dropped the DLL right after connecting, retrying");
			state.set(ConnectionState.DLL_DISCONNECTED);
			requestConnect("DLL disconnected right after connect");
			return;
		}
		bus.dispatch(new DllConnected(quikPath));
	}
}
