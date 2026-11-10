package io.github.soloyes.quikbridge.events;

import io.github.soloyes.quikbridge.api.TradeUpdate;

/** A trade (fill) of this account. */
public final class TradeEvent extends BaseEvent<TradeUpdate> {
	public TradeEvent(TradeUpdate data) {
		super(data);
	}
}
