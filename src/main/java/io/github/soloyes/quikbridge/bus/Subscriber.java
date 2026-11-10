package io.github.soloyes.quikbridge.bus;

import java.util.Set;

/**
 * Receives events whose class is (a subclass of) one of {@link #supports()}.
 * <p>
 * {@link #handle} is called on the publisher's thread, often a TRANS2QUIK.dll callback thread,
 * so it must return quickly: hand real work to an executor.
 */
public interface Subscriber {
	void handle(Event<?> event);

	Set<Class<? extends Event<?>>> supports();

	/** Release threads. Called once by {@link EventBus#stop()}. */
	default void shutdown() {
	}
}
