package io.github.soloyes.quikbridge.connection;

import io.github.soloyes.quikbridge.api.Status;

/**
 * Where the chain Java -> TRANS2QUIK.dll -> QUIK terminal -> broker server is currently broken.
 */
public enum ConnectionState {
	/** The DLL is not attached to the terminal. Nothing works; the connector keeps retrying. */
	DLL_DISCONNECTED,
	/** The DLL is attached to the terminal, but the terminal is not connected to the broker's server. */
	DLL_CONNECTED,
	/** Full chain is up: transactions and order/trade updates flow. */
	SERVER_CONNECTED;

	/**
	 * Next state after a connection status event. Events that do not apply in the current state
	 * (e.g. QUIK_CONNECTED while the DLL is detached) leave it unchanged.
	 */
	public ConnectionState on(Status event) {
		switch (event) {
			case DLL_CONNECTED:
				return this == DLL_DISCONNECTED ? DLL_CONNECTED : this;
			case DLL_DISCONNECTED:
				return DLL_DISCONNECTED;
			case QUIK_CONNECTED:
				return this == DLL_DISCONNECTED ? this : SERVER_CONNECTED;
			case QUIK_DISCONNECTED:
				return this == SERVER_CONNECTED ? DLL_CONNECTED : this;
			default:
				return this;
		}
	}
}
