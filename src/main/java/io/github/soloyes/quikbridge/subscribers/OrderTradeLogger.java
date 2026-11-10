package io.github.soloyes.quikbridge.subscribers;

import io.github.soloyes.quikbridge.bus.Event;
import io.github.soloyes.quikbridge.bus.Subscriber;
import io.github.soloyes.quikbridge.events.OrderEvent;
import io.github.soloyes.quikbridge.events.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Example sink: logs every order and trade. Register your own {@link Subscriber} for
 * {@link OrderEvent} / {@link TradeEvent} to feed a database, a strategy or a message queue.
 */
public final class OrderTradeLogger implements Subscriber {
	private static final Logger log = LoggerFactory.getLogger("quikbridge.orders");

	@Override
	public Set<Class<? extends Event<?>>> supports() {
		return Set.of(OrderEvent.class, TradeEvent.class);
	}

	@Override
	public void handle(Event<?> event) {
		log.info("{}", event.getData());
	}
}
