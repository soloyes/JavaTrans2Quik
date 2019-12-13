package main.dictionary;

import lombok.Getter;

public enum Status {
	TRANS2QUIK_SUCCESS(0) {
	},
	TRANS2QUIK_FAILED(1) {
	},
	TRANS2QUIK_QUIK_TERMINAL_NOT_FOUND(2) {
	},
	TRANS2QUIK_DLL_VERSION_NOT_SUPPORTED(3) {
	},
	TRANS2QUIK_ALREADY_CONNECTED_TO_QUIK(4) {
	},
	TRANS2QUIK_WRONG_SYNTAX(5) {
	},
	TRANS2QUIK_QUIK_NOT_CONNECTED(6) {
	},
	TRANS2QUIK_DLL_NOT_CONNECTED(7) {
	},
	TRANS2QUIK_QUIK_CONNECTED(8) {
	},
	TRANS2QUIK_QUIK_DISCONNECTED(9) {
	},
	TRANS2QUIK_DLL_CONNECTED(10) {
	},
	TRANS2QUIK_DLL_DISCONNECTED(11) {
	},
	TRANS2QUIK_MEMORY_ALLOCATION_ERROR(12) {
	},
	TRANS2QUIK_WRONG_CONNECTION_HANDLE(13) {
	},
	TRANS2QUIK_WRONG_INPUT_PARAMS(14) {
	},
	UNKNOWN_VALUE(666) {
	};

	@Getter
	private int code;

	Status(int code) {
		this.code = code;
	}

	public static Status decode(int code) {
		for (int i = 0; i < Status.values().length; i++) {
			if (Status.values()[i].code == code) {
				return Status.values()[i];
			}
		}
		return UNKNOWN_VALUE;
	}
}