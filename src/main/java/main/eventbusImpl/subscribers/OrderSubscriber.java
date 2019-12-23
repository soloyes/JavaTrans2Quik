package main.eventbusImpl.subscribers;

import main.eventbus.Event;
import main.eventbus.Subscribable;
import main.eventbusImpl.events.OrderSubscribeEvent;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class OrderSubscriber implements Subscribable {

	private static Logger logger = Logger.getLogger(OrderSubscriber.class);

	private Set<Class<?>> supports = new HashSet<Class<?>>() {{
		add(OrderSubscribeEvent.class);
	}};

	@Override
	public void handle(Event<?> event) {

	}

	@Override
	public Set<Class<?>> supports() {
		return supports;
	}
}
