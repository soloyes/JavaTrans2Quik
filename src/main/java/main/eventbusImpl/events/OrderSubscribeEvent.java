package main.eventbusImpl.events;

public class OrderSubscribeEvent extends SubscribeEvent<SubscriptionList> {

	public OrderSubscribeEvent(SubscriptionList subscriptionList) {
		super(subscriptionList);
	}
}
