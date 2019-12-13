package main.eventbus;

import java.util.Set;

/**
 * Description of a generic subscriber
 */
public interface Subscribable {
	/**
	 * Consume the events dispatched by the bus, events passed as parameter are can only be of type
	 * declared by the supports() Set
	 */
	void handle(Event<?> event);

	/**
	 * describes the set of classes the subscribable object intends to handle
	 */
	Set<Class<?>> supports();
}
