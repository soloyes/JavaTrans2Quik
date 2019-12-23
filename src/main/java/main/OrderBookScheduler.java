package main;

import main.eventbusImpl.events.OrderSubscribeEvent;
import main.eventbusImpl.events.SubscriptionList;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OrderBookScheduler {
	private static OrderBookScheduler instance;
	private static Logger logger = Logger.getLogger(OrderBookScheduler.class);

	private OrderBookScheduler() {
	}

	public static OrderBookScheduler getInstance() {
		if (instance == null) {
			instance = new OrderBookScheduler();
		}
		return instance;
	}

	public List<OrderSubscribeEvent> scheduleAll() {
		List<OrderSubscribeEvent> events = new ArrayList<>();
		if (Configuration.getBoolean("order.book.subscribe.all.class.codes")) {
			logger.warn("Subscribe to whole available classes. order.book.subscribe.all is true");
			String[] secCodes = new String[]{};
			if (Configuration.getBoolean("order.book.subscribe.all.sec.codes")) {
				logger.warn("Subscribe to whole available codes. order.book.subscribe.all.codes is true");
			} else {
				if (Configuration.getString("order.book.sec.codes").isEmpty()) {
					logger.warn("Subscribe to all available sec codes. order.book.sec.codes is empty");
				} else {
					secCodes = Configuration.getString("order.book.sec.codes").split(",");
				}
			}
			SubscriptionList subscriptionList = new SubscriptionList("", secCodes, "All Classes");
			events.add(new OrderSubscribeEvent(subscriptionList));
			logger.info("Preparing subscription: " + subscriptionList);
		} else {
			Set<String> subscriptions = new HashSet<>();
			Collections.addAll(subscriptions, Configuration.getString("order.book.subscription.name").split(","));
			subscriptions.remove("");
			for (String b : subscriptions) {
				String classCode = Configuration.getString("order.book.class.code." + b);
				String[] secCodes;
				if (Configuration.getString("order.book.sec.codes").isEmpty()) {
					secCodes = new String[]{};
				} else {
					secCodes = Configuration.getString("order.book.sec.codes." + b).split(",");
				}
				SubscriptionList subscriptionList = new SubscriptionList(classCode, secCodes, b);
				events.add(new OrderSubscribeEvent(subscriptionList));
				logger.info("Preparing subscription: " + subscriptionList);
			}
		}
		if (events.size() == 0){
			logger.warn("Nothing to subscribe for order book");
		}
		return events;
	}
}