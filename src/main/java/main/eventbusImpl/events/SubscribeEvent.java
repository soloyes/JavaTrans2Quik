package main.eventbusImpl.events;

import main.eventbus.Event;

public class SubscribeEvent<T> implements Event<T> {
	private T t;

	@Override
	public T getData() {
		return t;
	}

	public SubscribeEvent(T t) {
		this.t = t;
	}
}
