package io.github.soloyes.quikbridge.events;

import io.github.soloyes.quikbridge.api.OrderUpdate;

/** An order of this account was created or changed. */
public final class OrderEvent extends BaseEvent<OrderUpdate> {
	public OrderEvent(OrderUpdate data) {
		super(data);
	}
}
