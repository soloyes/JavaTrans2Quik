package io.github.soloyes.quikbridge.subscribers;

import io.github.soloyes.quikbridge.bus.Subscriber;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Base for subscribers that move work off the publisher's thread onto their own executor.
 */
public abstract class ExecutorSubscriber implements Subscriber {
	protected final ExecutorService executor;

	protected ExecutorSubscriber(ExecutorService executor) {
		this.executor = executor;
	}

	@Override
	public void shutdown() {
		executor.shutdownNow();
		try {
			executor.awaitTermination(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
