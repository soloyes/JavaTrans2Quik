package io.github.soloyes.quikbridge.config;

/** Missing or malformed configuration. The application exits with a non-zero status on it. */
public class ConfigException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ConfigException(String message) {
		super(message);
	}

	public ConfigException(String message, Throwable cause) {
		super(message, cause);
	}
}
