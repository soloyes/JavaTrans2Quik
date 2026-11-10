package io.github.soloyes.quikbridge.api;

/**
 * A condition retrying will not fix, e.g. a DLL version the terminal does not support.
 */
public class QuikException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public QuikException(String message) {
		super(message);
	}
}
