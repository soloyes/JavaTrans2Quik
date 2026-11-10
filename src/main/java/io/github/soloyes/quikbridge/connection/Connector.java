package io.github.soloyes.quikbridge.connection;

import io.github.soloyes.quikbridge.api.CallResult;
import io.github.soloyes.quikbridge.api.QuikApi;
import io.github.soloyes.quikbridge.api.QuikException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attaches TRANS2QUIK.dll to the terminal, retrying while the terminal is not up yet.
 */
public final class Connector {
	private static final Logger log = LoggerFactory.getLogger(Connector.class);

	/** What to do with the result of one TRANS2QUIK_CONNECT call. */
	public enum Decision { CONNECTED, RETRY, FATAL }

	private final QuikApi api;
	private final String quikPath;
	private final long retryIntervalMs;
	private final long maxAttempts;
	private final Sleeper sleeper;

	/**
	 * @param maxAttempts 0 = retry until interrupted
	 */
	public Connector(QuikApi api, String quikPath, long retryIntervalMs, long maxAttempts, Sleeper sleeper) {
		this.api = api;
		this.quikPath = quikPath;
		this.retryIntervalMs = retryIntervalMs;
		this.maxAttempts = maxAttempts;
		this.sleeper = sleeper;
	}

	public static Decision decide(CallResult r) {
		switch (r.status()) {
			case SUCCESS:
			case ALREADY_CONNECTED_TO_QUIK:
				return Decision.CONNECTED;
			case FAILED:                    // e.g. terminal still starting; extended code tells why
			case QUIK_TERMINAL_NOT_FOUND:   // info.exe not running or external transactions not enabled yet
				return Decision.RETRY;
			case DLL_VERSION_NOT_SUPPORTED: // retrying with the same DLL cannot help
			default:                        // codes CONNECT is not documented to return
				return Decision.FATAL;
		}
	}

	/**
	 * Blocks until the DLL is attached.
	 *
	 * @return true when connected; false when interrupted or {@code maxAttempts} ran out
	 * @throws QuikException when the result cannot be fixed by retrying
	 */
	public boolean connect() {
		for (long attempt = 1; maxAttempts == 0 || attempt <= maxAttempts; attempt++) {
			if (Thread.currentThread().isInterrupted()) {
				return false;
			}
			CallResult r = api.connect(quikPath);
			switch (decide(r)) {
				case CONNECTED:
					log.info("Trans2QUIK.dll attached to the terminal at {} ({})", quikPath, r.status());
					return true;
				case FATAL:
					throw new QuikException(describeFatal(r));
				case RETRY:
				default:
					boolean last = maxAttempts != 0 && attempt == maxAttempts;
					log.warn("Connect attempt {}{} failed: {}. {}", attempt,
						maxAttempts == 0 ? "" : "/" + maxAttempts, r, last ? "" : hint(r));
					if (last) {
						log.error("Giving up after {} connect attempts", maxAttempts);
						return false;
					}
			}
			try {
				sleeper.sleep(retryIntervalMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return false;
	}

	private String describeFatal(CallResult r) {
		switch (r.status()) {
			case DLL_VERSION_NOT_SUPPORTED:
				return "This TRANS2QUIK.dll version is not supported by the terminal at " + quikPath
					+ ". Use the DLL shipped with this QUIK version.";
			default:
				return "Unexpected TRANS2QUIK_CONNECT result " + r;
		}
	}

	private static String hint(CallResult r) {
		switch (r.status()) {
			case QUIK_TERMINAL_NOT_FOUND:
				return "Check quik.path, that the terminal is running and that external transactions are enabled "
					+ "(Services > External transactions > Start).";
			default:
				return "Retrying.";
		}
	}

	public void disconnect() {
		CallResult r = api.disconnect();
		switch (r.status()) {
			case SUCCESS:
				log.info("Trans2QUIK.dll detached from the terminal");
				break;
			case DLL_NOT_CONNECTED:
				log.info("Disconnect: the DLL was not attached");
				break;
			default:
				log.warn("Disconnect failed: {}", r);
		}
	}

	public boolean isDllConnected() {
		CallResult r = api.isDllConnected();
		switch (r.status()) {
			case DLL_CONNECTED:
				return true;
			case DLL_NOT_CONNECTED:
				return false;
			default:
				log.warn("Unexpected TRANS2QUIK_IS_DLL_CONNECTED result {}", r);
				return false;
		}
	}

	public boolean isServerConnected() {
		CallResult r = api.isQuikConnected();
		switch (r.status()) {
			case QUIK_CONNECTED:
				return true;
			case QUIK_NOT_CONNECTED:
			case DLL_NOT_CONNECTED:
				return false;
			default:
				log.warn("Unexpected TRANS2QUIK_IS_QUIK_CONNECTED result {}", r);
				return false;
		}
	}
}
