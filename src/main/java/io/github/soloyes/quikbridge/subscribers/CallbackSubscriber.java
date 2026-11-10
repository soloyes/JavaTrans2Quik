package io.github.soloyes.quikbridge.subscribers;

import io.github.soloyes.quikbridge.api.CallResult;
import io.github.soloyes.quikbridge.api.QuikApi;
import io.github.soloyes.quikbridge.api.Status;
import io.github.soloyes.quikbridge.bus.Event;
import io.github.soloyes.quikbridge.bus.EventBus;
import io.github.soloyes.quikbridge.events.ConnectionStatusChanged;
import io.github.soloyes.quikbridge.events.DllConnected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * On {@link DllConnected}, installs TRANS2QUIK_CONNECTION_STATUS_CALLBACK. Every status event from the DLL
 * is republished as {@link ConnectionStatusChanged}, which drives reconnects in {@link ConnectionSubscriber}.
 */
public final class CallbackSubscriber extends ExecutorSubscriber {
	private static final Logger log = LoggerFactory.getLogger(CallbackSubscriber.class);

	private final QuikApi api;
	private final EventBus bus;

	public CallbackSubscriber(QuikApi api, EventBus bus, ExecutorService executor) {
		super(executor);
		this.api = api;
		this.bus = bus;
	}

	@Override
	public Set<Class<? extends Event<?>>> supports() {
		return Set.of(DllConnected.class);
	}

	@Override
	public void handle(Event<?> event) {
		executor.execute(this::install);
	}

	private void install() {
		CallResult r = api.setConnectionStatusListener(this::onStatus);
		if (r.is(Status.SUCCESS)) {
			log.info("Connection status callback installed");
		} else {
			log.error("Could not install connection status callback: {}", r);
		}
	}

	private void onStatus(Status event, long extendedErrorCode, String message) {
		switch (event) {
			case QUIK_CONNECTED:
				log.info("Terminal connected to the server");
				break;
			case QUIK_DISCONNECTED:
				log.warn("Terminal lost the server connection");
				break;
			case DLL_CONNECTED:
				log.info("DLL attached to the terminal");
				break;
			case DLL_DISCONNECTED:
				log.error("DLL lost the terminal (ext={}, {})", extendedErrorCode, message);
				break;
			default:
				log.warn("Unexpected connection status {} (ext={}, {})", event, extendedErrorCode, message);
				return;
		}
		bus.dispatch(new ConnectionStatusChanged(event));
	}
}
