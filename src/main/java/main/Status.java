package main;

import org.apache.log4j.Logger;

public enum Status {
	TRANS2QUIK_SUCCESS(0) {
		void log(){
			logger.info(name());
		}
	},
	TRANS2QUIK_FAILED(1) {
		void log(){
			logger.error(name());
		}
	},
	TRANS2QUIK_QUIK_TERMINAL_NOT_FOUND(2) {
		void log(){
			logger.fatal(name());
		}
	},
	TRANS2QUIK_DLL_VERSION_NOT_SUPPORTED(3) {
		void log(){
			logger.fatal(name());
		}
	},
	TRANS2QUIK_ALREADY_CONNECTED_TO_QUIK(4) {
		void log(){
			logger.error(name());
		}
	},
	TRANS2QUIK_WRONG_SYNTAX(5) {
		void log(){
			logger.error(name());
		}
	},
	TRANS2QUIK_QUIK_NOT_CONNECTED(6) {
		void log(){
			logger.error(name());
		}
	},
	TRANS2QUIK_DLL_NOT_CONNECTED(7) {
		void log(){
			logger.error(name());
		}
	},
	TRANS2QUIK_QUIK_CONNECTED(8) {
		void log(){
			logger.info(name());
		}
	},
	TRANS2QUIK_QUIK_DISCONNECTED(9) {
		void log(){
			logger.warn(name());
		}
	},
	TRANS2QUIK_DLL_CONNECTED(10) {
		void log(){
			logger.info(name());
		}
	},
	TRANS2QUIK_DLL_DISCONNECTED(11) {
		void log(){
			logger.warn(name());
		}
	},
	TRANS2QUIK_MEMORY_ALLOCATION_ERROR(12) {
		void log(){
			logger.error(name());
		}
	},
	TRANS2QUIK_WRONG_CONNECTION_HANDLE(13) {
		void log(){
			logger.error(name());
		}
	},
	TRANS2QUIK_WRONG_INPUT_PARAMS(14) {
		void log(){
			logger.error(name());
		}
	},
	UNKNOWN_VALUE(666) {
		void log(){
			logger.fatal(name());
		}
	};

	private static Logger logger = Logger.getLogger(Status.class);
	private int code;

	Status(int code) {
		this.code = code;
	}

	abstract void log();

	public static Status decode(int code) {
		for (int i = 0; i < Status.values().length; i++) {
			if (Status.values()[i].code == code) {
				return Status.values()[i];
			}
		}
		return UNKNOWN_VALUE;
	}
}