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

public class CallBackSubscriber implements Subscribable, Pool {
	private static ThreadPoolExecutor callBackConnectionExecutor;
	private static Logger logger = Logger.getLogger(CallBackSubscriber.class);

	private Set<Class<?>> supports = new HashSet<Class<?>>() {{
		add(CallBackConnectionEvent.class);
	}};

	static {
		callBackConnectionExecutor = new ThreadPoolExecutor(
			1, 1, 0L,
			TimeUnit.MILLISECONDS,
			new SynchronousQueue<Runnable>(),
			new RejectedExecutionHandler() {
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
					logger.info("callBackConnectionExecutor is singleThreadPool");
				}
			}
		);
	}

	public void handle(Event<?> event) {
		if (event.getClass() == CallBackConnectionEvent.class) {
			callBackConnectionExecutor.execute(Connector.getInstance().setConnectionCallBack());
		}
	}

	public Set<Class<?>> supports() {
		return supports;
	}

	public void shutdown() {
		callBackConnectionExecutor.shutdownNow();
	}
}