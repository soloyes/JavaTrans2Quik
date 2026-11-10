package io.github.soloyes.quikbridge.events;

/** Ask the connector to (re)connect to the terminal. Payload: the reason, for the log. */
public final class ConnectRequested extends BaseEvent<String> {
	public ConnectRequested(String data) {
		super(data);
	}
}
