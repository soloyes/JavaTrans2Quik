package main.eventbusImpl;

import main.connection.Pool;
import main.eventbus.Event;
import main.eventbus.EventBus;
import main.eventbus.Subscribable;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Bus implements EventBus {
	private Logger logger = Logger.getLogger(Bus.class);
	private static Bus instance;

	private Bus() {
	}

	public static Bus getInstance() {
		if (instance == null) {
			instance = new Bus();
		}
		return instance;
	}

	private List<Subscribable> subscribableList = new ArrayList<Subscribable>();

	public void register(Subscribable subscribable) {
		subscribableList.add(subscribable);
	}

	public void dispatch(Event<?> event) {
		logger.debug("Dispatch event: " + event.getClass().getSimpleName());
		for (Subscribable subscribable : subscribableList) {
			subscribable.handle(event);
		}
	}

	public List<Subscribable> getSubscribers() {
		return subscribableList;
	}

	public void stop() {
		for (Subscribable subscribable : subscribableList) {
			if (subscribable instanceof Pool) {
				((Pool) subscribable).shutdown();
			}
		}
	}
}