package io.github.soloyes.quikbridge.connection;

import io.github.soloyes.quikbridge.api.Status;
import io.github.soloyes.quikbridge.bus.EventBus;
import io.github.soloyes.quikbridge.events.ConnectRequested;
import io.github.soloyes.quikbridge.events.ConnectionStatusChanged;

import java.util.function.Supplier;

/**
 * Periodic check that does not rely on QUIK delivering the status callback.
 * It polls TRANS2QUIK_IS_DLL_CONNECTED / _IS_QUIK_CONNECTED and publishes the difference from the
 * believed state as ordinary {@link ConnectionStatusChanged} events, so the same code path reconnects.
 */
public final class Heartbeat implements Runnable {
	private final Connector connector;
	private final EventBus bus;
	private final Supplier<ConnectionState> believed;
	private final Supplier<Boolean> connecting;

	public Heartbeat(Connector connector, EventBus bus, Supplier<ConnectionState> believed, Supplier<Boolean> connecting) {
		this.connector = connector;
		this.bus = bus;
		this.believed = believed;
		this.connecting = connecting;
	}

	@Override
	public void run() {
		if (connecting.get()) {
			return;
		}
		ConnectionState state = believed.get();
		if (state == ConnectionState.DLL_DISCONNECTED) {
			bus.dispatch(new ConnectRequested("heartbeat: not connected"));
			return;
		}
		if (!connector.isDllConnected()) {
			bus.dispatch(new ConnectionStatusChanged(Status.DLL_DISCONNECTED));
			return;
		}
		boolean server = connector.isServerConnected();
		if (server && state == ConnectionState.DLL_CONNECTED) {
			bus.dispatch(new ConnectionStatusChanged(Status.QUIK_CONNECTED));
		} else if (!server && state == ConnectionState.SERVER_CONNECTED) {
			bus.dispatch(new ConnectionStatusChanged(Status.QUIK_DISCONNECTED));
		}
	}
}
