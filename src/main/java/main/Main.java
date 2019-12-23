package main;

import main.eventbusImpl.events.DisconnectionEvent;
import main.eventbusImpl.subscribers.CallBackSubscriber;
import main.eventbusImpl.events.ConnectionEvent;
import main.eventbusImpl.subscribers.ConnectionSubscriber;
import main.eventbusImpl.Bus;
import org.apache.log4j.Logger;

public class Main {
	private static Logger logger = Logger.getLogger(Main.class);

	public static void main(String[] args) {
		Bus bus = Bus.getInstance();

//		bus.register(new CallBackSubscriber());
//		bus.register(new ConnectionSubscriber());
//
//		bus.dispatch(new ConnectionEvent());
//
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException e) {
//			logger.error("", e);
//		}
//
//		bus.dispatch(new DisconnectionEvent());
//
//		try {
//			Thread.sleep(Configuration.getLong("main.thread.timeout"));
//		} catch (InterruptedException e) {
//			logger.error("", e);
//		} finally {
//			logger.info("Graceful stop");
//
//			bus.stop();
//		}

		OrderBookScheduler.getInstance().scheduleAll();
	}
}