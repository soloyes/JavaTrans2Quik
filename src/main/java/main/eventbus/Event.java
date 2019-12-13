package main.eventbus;

/**
 * interface describing a generic event, and it's associated meta data, it's this what's going to
 * get sent in the bus to be dispatched to interested Subscribers
 */
public interface Event<T> {
	/**
	 * @returns the stored data associated with the event
	 */
	T getData();
}