package io.github.soloyes.quikbridge.events;

import io.github.soloyes.quikbridge.api.Status;

/** Event from TRANS2QUIK_CONNECTION_STATUS_CALLBACK, or a state the heartbeat observed. */
public final class ConnectionStatusChanged extends BaseEvent<Status> {
	public ConnectionStatusChanged(Status data) {
		super(data);
	}
}
