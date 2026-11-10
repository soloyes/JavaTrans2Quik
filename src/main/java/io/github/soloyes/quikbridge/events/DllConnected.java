package io.github.soloyes.quikbridge.events;

/** TRANS2QUIK_CONNECT succeeded: callbacks and subscriptions can be (re)installed. Payload: QUIK path. */
public final class DllConnected extends BaseEvent<String> {
	public DllConnected(String data) {
		super(data);
	}
}
