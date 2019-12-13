package main;

import main.eventbusImpl.CallBackSubscriber;
import main.eventbusImpl.ConnectionEvent;
import main.eventbusImpl.ConnectionSubscriber;
import main.eventbusImpl.Bus;
import org.apache.log4j.Logger;

public class Main {
	private static Logger logger = Logger.getLogger(Main.class);

	public static void main(String[] args) {
		Bus bus = Bus.getInstance();

		bus.register(new CallBackSubscriber());
		bus.register(new ConnectionSubscriber());

		bus.dispatch(new ConnectionEvent());

		try {
			Thread.sleep(Configuration.getLong("main.thread.ti1meout"));
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		} finally {
			logger.info("Graceful stop");
			bus.stop();
		}

	}
}