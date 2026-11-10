package io.github.soloyes.quikbridge.api;

import java.util.Objects;

/**
 * Outcome of one TRANS2QUIK_* call: decoded status, raw return code, extended error code and error text.
 */
public final class CallResult {
	private final long code;
	private final Status status;
	private final long extendedErrorCode;
	private final String message;

	public CallResult(long code, long extendedErrorCode, String message) {
		this.code = code;
		this.status = Status.decode(code);
		this.extendedErrorCode = extendedErrorCode;
		this.message = message == null ? "" : message;
	}

	public static CallResult of(Status status) {
		return new CallResult(status.code(), 0, "");
	}

	public static CallResult failed(long extendedErrorCode, String message) {
		return new CallResult(Status.FAILED.code(), extendedErrorCode, message);
	}

	public long code() {
		return code;
	}

	public Status status() {
		return status;
	}

	public long extendedErrorCode() {
		return extendedErrorCode;
	}

	public String message() {
		return message;
	}

	public boolean is(Status expected) {
		return status == expected;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof CallResult)) return false;
		CallResult that = (CallResult) o;
		return code == that.code && extendedErrorCode == that.extendedErrorCode && message.equals(that.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, extendedErrorCode, message);
	}

	@Override
	public String toString() {
		String s = status == Status.UNKNOWN ? "UNKNOWN(" + code + ")" : status.name();
		if (extendedErrorCode != 0 || !message.isEmpty()) {
			s += " [ext=" + extendedErrorCode + (message.isEmpty() ? "" : ", " + message) + "]";
		}
		return s;
	}
}
