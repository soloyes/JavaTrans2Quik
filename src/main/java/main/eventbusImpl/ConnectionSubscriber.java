package main.eventbusImpl;

import main.connection.Connector;
import main.connection.Pool;
import main.eventbus.Event;
import main.eventbus.Subscribable;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RejectedExecutionHandler;
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
			new SynchronousQueue<Runnable>(),
			new RejectedExecutionHandler() {
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
					logger.info("connectionExecutor is singleThreadPool");
				}
			}
		);
	}

	public void shutdown() {
		connectionExecutor.shutdownNow();
	}

	public void handle(Event<?> event) {
		if (event.getClass() == ConnectionEvent.class) {
			connectionExecutor.execute(Connector.getInstance().connectToQuik());
		}
	}

	public Set<Class<?>> supports() {
		return supports;
	}
}
