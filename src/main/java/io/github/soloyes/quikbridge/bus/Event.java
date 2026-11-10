package io.github.soloyes.quikbridge.bus;

/**
 * Something published on the {@link EventBus}. Events are immutable.
 *
 * @param <T> payload type ({@link Void} for pure signals)
 */
public interface Event<T> {
	T getData();
}
