package io.github.soloyes.quikbridge.events;

import io.github.soloyes.quikbridge.bus.Event;

/** Immutable event carrying one payload. */
public abstract class BaseEvent<T> implements Event<T> {
	private final T data;

	protected BaseEvent(T data) {
		this.data = data;
	}

	@Override
	public T getData() {
		return data;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + (data == null ? "" : "(" + data + ")");
	}
}
