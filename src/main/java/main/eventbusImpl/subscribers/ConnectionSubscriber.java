package main.eventbusImpl.subscribers;

import main.connection.Connector;
import main.connection.Pool;
import main.eventbus.Event;
import main.eventbus.Subscribable;
import main.eventbusImpl.events.ConnectionEvent;
import main.eventbusImpl.events.DisconnectionEvent;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConnectionSubscriber implements Subscribable, Pool {

	private static ThreadPoolExecutor connectionExecutor;
	private static Logger logger = Logger.getLogger(ConnectionSubscriber.class);

	private Set<Class<?>> supports = new HashSet<Class<?>>() {{
		add(ConnectionEvent.class);
	}};

	static {
		connectionExecutor = new ThreadPoolExecutor(
			1, 1, 0L,
			TimeUnit.MILLISECONDS,
			new SynchronousQueue<>(),
			(r, executor) -> logger.info("Incoming thread. But connectionExecutor is singleThreadPool. Drop from queue.")
		);
	}

	public void shutdown() {
		connectionExecutor.shutdownNow();
	}

	public void handle(Event<?> event) {
		if (event.getClass() == ConnectionEvent.class) {
			connectionExecutor.execute(Connector.getInstance().connectToQuik());
		} else if (event.getClass() == DisconnectionEvent.class) {
			connectionExecutor.execute(Connector.getInstance().disconnectFromQuik());
		}
	}

	public Set<Class<?>> supports() {
		return supports;
	}
}
