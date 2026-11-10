package io.github.soloyes.quikbridge.api;

/**
 * Return and event codes of TRANS2QUIK.dll (TRANS2QUIK_* constants from trans2quik_api.h).
 */
public enum Status {
	SUCCESS(0),
	FAILED(1),
	QUIK_TERMINAL_NOT_FOUND(2),
	DLL_VERSION_NOT_SUPPORTED(3),
	ALREADY_CONNECTED_TO_QUIK(4),
	WRONG_SYNTAX(5),
	QUIK_NOT_CONNECTED(6),
	DLL_NOT_CONNECTED(7),
	QUIK_CONNECTED(8),
	QUIK_DISCONNECTED(9),
	DLL_CONNECTED(10),
	DLL_DISCONNECTED(11),
	MEMORY_ALLOCATION_ERROR(12),
	WRONG_CONNECTION_HANDLE(13),
	WRONG_INPUT_PARAMS(14),
	/** Any code this enum does not know. The raw value is kept in {@link CallResult#code()}. */
	UNKNOWN(-1);

	private static final Status[] BY_CODE = new Status[15];

	static {
		for (Status s : values()) {
			if (s != UNKNOWN) {
				BY_CODE[s.code] = s;
			}
		}
	}

	private final int code;

	Status(int code) {
		this.code = code;
	}

	public int code() {
		return code;
	}

	public static Status decode(long code) {
		if (code < 0 || code >= BY_CODE.length) {
			return UNKNOWN;
		}
		return BY_CODE[(int) code];
	}
}
