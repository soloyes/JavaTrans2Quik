package main.connection;

import com.sun.jna.NativeLong;
import com.sun.jna.ptr.NativeLongByReference;
import main.Configuration;
import main.dictionary.Status;
import main.dictionary.Translator;
import main.eventbusImpl.events.CallBackConnectionEvent;
import main.eventbusImpl.events.ConnectionEvent;
import main.eventbusImpl.Bus;
import org.apache.log4j.Logger;
import org.jtrans2quik.loader.Trans2QuikLibraryLoader;
import org.jtrans2quik.wrapper.Trans2QuikLibrary;

public class Connector {
	private static Connector instance;
	private static Trans2QuikLibrary quikAdapter = Trans2QuikLibraryLoader.LIBRARY;
	private static NativeLongByReference errorCode = new NativeLongByReference();
	private static byte[] buffer = new byte[255];
	private static Logger logger = Logger.getLogger(Connector.class);

	private Connector() {
	}

	public static Connector getInstance() {
		if (instance == null) {
			instance = new Connector();
		}
		return instance;
	}

	public Runnable connectToQuik() {
		return () -> {
			try {
				NativeLong returnCode = quikAdapter.TRANS2QUIK_CONNECT(Configuration.getString("path.to.quick"), errorCode, buffer, buffer.length);
				Status status = Status.decode(returnCode.intValue());

				while (!Thread.currentThread().isInterrupted()) {
					logger.trace("connectToQuik()");

					switch (status) {
					case TRANS2QUIK_SUCCESS:
						logger.info("Connection to QUIK terminal established.");
						Bus.getInstance().dispatch(new CallBackConnectionEvent());
						return;
					case TRANS2QUIK_FAILED:
						logger.error("Connection to QUIK terminal failed. " + errorCode.getValue().intValue() + " " + Translator.byteToString(buffer));
						break;
					case TRANS2QUIK_QUIK_TERMINAL_NOT_FOUND:
						logger.fatal("Either INFO.EXE is missing in the specified directory or the external transaction processing service is not running on it. Please check QUIK terminal.");
						break;
					case TRANS2QUIK_DLL_VERSION_NOT_SUPPORTED:
						logger.error("The used version of Trans2QUIK.dll is not supported by the specified INFO.EXE.");

//      		Documentation collision
//				case TRANS2QUIK_DLL_ALREADY_CONNECTED_TO_QUIK:
//				logger.warn("Connection to QUIK already established.");
//				break;

					default:
						logger.fatal(Status.UNKNOWN_VALUE + " " + returnCode.intValue());
						logger.info("Exit program");
						System.exit(0);
					}
					Thread.sleep(Configuration.getLong("heartbeat.thread.timeout"));
				}
			} catch (InterruptedException e) {
				logger.error("", e);
				Thread.currentThread().interrupt();
			}
		};
	}

	public Runnable disconnectFromQuik() {
		return () -> {
			try {
				NativeLong returnCode = quikAdapter.TRANS2QUIK_DISCONNECT(errorCode, buffer, buffer.length);
				Status status = Status.decode(returnCode.intValue());

				while (!Thread.currentThread().isInterrupted()) {
					logger.trace("disconnectFromQuik()");

					switch (status) {
					case TRANS2QUIK_SUCCESS:
						logger.info("Connection of Trans2QUIK.dll library with QUIK workstation was broken successfully.");
						return;
					case TRANS2QUIK_FAILED:
						logger.error("An error occurred while disconnecting. " + errorCode.getValue().intValue() + " " + Translator.byteToString(buffer));
						break;
					case TRANS2QUIK_DLL_NOT_CONNECTED:
						logger.error("Attempt to disconnect when connection is not established.");
						return;
					default:
						logger.fatal(Status.UNKNOWN_VALUE + " " + returnCode.intValue());
						logger.info("Exit program");
						System.exit(0);
					}
					Thread.sleep(Configuration.getLong("heartbeat.thread.timeout"));
				}
			} catch (InterruptedException e) {
				logger.error("", e);
				Thread.currentThread().interrupt();
			}
		};
	}

	public boolean connectedToToServer() {
		logger.trace("connectedToToServer()");
		NativeLong returnCode = quikAdapter.TRANS2QUIK_IS_QUIK_CONNECTED(errorCode, buffer, buffer.length);
		Status status = Status.decode(returnCode.intValue());

		switch (status) {
		case TRANS2QUIK_QUIK_CONNECTED:
			logger.info("Connection from QUIK terminal to server established.");
			return true;
		case TRANS2QUIK_QUIK_NOT_CONNECTED:
			logger.error("Connection from QUIK terminal to server is not established.");
			return false;
		case TRANS2QUIK_DLL_NOT_CONNECTED:
			logger.error("Trans2QUIK.dll was not connected to the QUIK terminal. In this case, it is impossible to verify the presence or absence of communication between the QUIK terminal and the server.");
			return false;
		default:
			logger.fatal(Status.UNKNOWN_VALUE + " " + returnCode.intValue());
			logger.info("Exit program");
			System.exit(0);
		}
		return false;
	}

	public boolean connectedToQuik() {
		logger.trace("connectedToQuik()");
		NativeLong returnCode = quikAdapter.TRANS2QUIK_IS_DLL_CONNECTED(errorCode, buffer, buffer.length);
		Status status = Status.decode(returnCode.intValue());

		switch (status) {
		case TRANS2QUIK_DLL_CONNECTED:
			logger.info("Connection of the Trans2QUIK.dll library with the QUIK terminal is established.");
			return true;
		case TRANS2QUIK_DLL_NOT_CONNECTED:
			logger.error("The connection of the Trans2QUIK.dll library with the QUIK terminal is not established.");
			return false;
		default:
			logger.fatal(Status.UNKNOWN_VALUE + " " + returnCode.intValue());
			logger.info("Exit program");
			System.exit(0);
		}
		return false;
	}

	public Runnable setConnectionCallBack() {
		logger.trace("setConnectionCallBack()");
		return () -> {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					NativeLong returnCode = quikAdapter.TRANS2QUIK_SET_CONNECTION_STATUS_CALLBACK(
						(Trans2QuikLibrary.ConnectionStatusCallback) (nConnectionEvent, nExtendedErrorCode, lpcstrInfoMessage) -> {
							logger.trace("ConnectionStatusCallback.callback()");
							Status status = Status.decode(nConnectionEvent.intValue());
							switch (status) {
							case TRANS2QUIK_QUIK_CONNECTED:
								logger.info("The connection between the QUIK terminal and the server is established");
								break;
							case TRANS2QUIK_QUIK_DISCONNECTED:
								logger.error("The connection between the QUIK terminal and the server is disconnected.");
								break;
							case TRANS2QUIK_DLL_CONNECTED:
								logger.info("The connection between the Trans2QUIK.dll and the used QUIK terminal is established.");
								break;
							case TRANS2QUIK_DLL_DISCONNECTED:
								logger.error("The connection between the Trans2QUIK.dll and the used QUIK terminal is broken.");
								Bus.getInstance().dispatch(new ConnectionEvent());
								break;
							default:
								logger.fatal(Status.UNKNOWN_VALUE + " " + nConnectionEvent.intValue());
								logger.info("Exit program");
								System.exit(0);
							}
						}, errorCode, buffer, buffer.length
					);

					Status status = Status.decode(returnCode.intValue());

					switch (status) {
					case TRANS2QUIK_SUCCESS:
						logger.info("Callback function set.");
						return;
					case TRANS2QUIK_FAILED:
						logger.error("The callback function could not be set. " + errorCode.getValue().intValue() + " " + Translator.byteToString(buffer));
						break;
					default:
						logger.fatal(Status.UNKNOWN_VALUE + " " + returnCode.intValue());
						logger.info("Exit program");
						System.exit(0);
					}
					Thread.sleep(Configuration.getLong("heartbeat.thread.timeout"));
				}
			} catch (InterruptedException e) {
				logger.error("", e);
				Thread.currentThread().interrupt();
			}
		};
	}
}