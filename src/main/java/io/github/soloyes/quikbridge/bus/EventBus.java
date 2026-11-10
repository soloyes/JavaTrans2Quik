package io.github.soloyes.quikbridge.bus;

import java.util.List;

/**
 * Minimal synchronous publish/subscribe bus.
 */
public interface EventBus {
	void register(Subscriber subscriber);

	/** Delivers the event to every registered subscriber that supports its type, in registration order. */
	void dispatch(Event<?> event);

	/** Snapshot of the registered subscribers. */
	List<Subscriber> getSubscribers();

	/** Stops accepting events and shuts down every subscriber. Idempotent. */
	void stop();
}
