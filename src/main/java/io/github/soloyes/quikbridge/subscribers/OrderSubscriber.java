package io.github.soloyes.quikbridge.subscribers;

import io.github.soloyes.quikbridge.api.CallResult;
import io.github.soloyes.quikbridge.api.QuikApi;
import io.github.soloyes.quikbridge.api.Status;
import io.github.soloyes.quikbridge.bus.Event;
import io.github.soloyes.quikbridge.bus.EventBus;
import io.github.soloyes.quikbridge.events.DllConnected;
import io.github.soloyes.quikbridge.events.OrderEvent;
import io.github.soloyes.quikbridge.events.TradeEvent;
import io.github.soloyes.quikbridge.subscription.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Orders &amp; trades subscription. On every {@link DllConnected} (first connect and each reconnect):
 * <ol>
 *     <li>drops old subscriptions (TRANS2QUIK_UNSUBSCRIBE_ORDERS / _TRADES) if there were any;</li>
 *     <li>calls TRANS2QUIK_SUBSCRIBE_ORDERS / _TRADES once per planned {@link Subscription};</li>
 *     <li>calls TRANS2QUIK_START_ORDERS / _TRADES with listeners that publish {@link OrderEvent} / {@link TradeEvent}.</li>
 * </ol>
 * The DLL then replays the existing orders/trades of the subscribed classes (mode 1, last one mode 2)
 * and pushes new ones as they happen (mode 0). These are the account's own orders and fills,
 * not market data: Trans2Quik has no order book or quotes API.
 */
public final class OrderSubscriber extends ExecutorSubscriber {
	private static final Logger log = LoggerFactory.getLogger(OrderSubscriber.class);

	private final QuikApi api;
	private final EventBus bus;
	private final List<Subscription> plan;
	private final boolean orders;
	private final boolean trades;
	private volatile boolean subscribed;

	public OrderSubscriber(QuikApi api, EventBus bus, List<Subscription> plan,
						   boolean orders, boolean trades, ExecutorService executor) {
		super(executor);
		this.api = api;
		this.bus = bus;
		this.plan = new ArrayList<>(plan);
		this.orders = orders;
		this.trades = trades;
	}

	@Override
	public Set<Class<? extends Event<?>>> supports() {
		return Set.of(DllConnected.class);
	}

	@Override
	public void handle(Event<?> event) {
		executor.execute(this::subscribe);
	}

	public boolean isSubscribed() {
		return subscribed;
	}

	private void subscribe() {
		if (plan.isEmpty() || (!orders && !trades)) {
			log.warn("Nothing to subscribe to");
			return;
		}
		if (subscribed) {
			unsubscribe();
		}
		boolean ok = true;
		for (Subscription s : plan) {
			if (orders) {
				ok &= check("SUBSCRIBE_ORDERS " + s, api.subscribeOrders(s.classCode(), s.secCodesParam()));
			}
			if (trades) {
				ok &= check("SUBSCRIBE_TRADES " + s, api.subscribeTrades(s.classCode(), s.secCodesParam()));
			}
		}
		if (orders) {
			ok &= check("START_ORDERS", api.startOrders(o -> bus.dispatch(new OrderEvent(o))));
		}
		if (trades) {
			ok &= check("START_TRADES", api.startTrades(t -> bus.dispatch(new TradeEvent(t))));
		}
		subscribed = true;
		if (ok) {
			log.info("Receiving {}{}{} for {} subscription(s)", orders ? "orders" : "",
				orders && trades ? " and " : "", trades ? "trades" : "", plan.size());
		} else {
			log.error("Some subscribe calls failed, see above; they are repeated on the next reconnect");
		}
	}

	/** Drops all order and trade subscriptions. Safe to call when nothing is subscribed. */
	public void unsubscribe() {
		if (orders) {
			check("UNSUBSCRIBE_ORDERS", api.unsubscribeOrders());
		}
		if (trades) {
			check("UNSUBSCRIBE_TRADES", api.unsubscribeTrades());
		}
		subscribed = false;
	}

	private static boolean check(String what, CallResult r) {
		if (r.is(Status.SUCCESS)) {
			log.debug("{}: {}", what, r);
			return true;
		}
		log.error("{} failed: {}", what, r);
		return false;
	}
}
