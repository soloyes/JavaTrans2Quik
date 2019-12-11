package main;

import com.sun.jna.NativeLong;
import com.sun.jna.ptr.NativeLongByReference;
import org.apache.log4j.Logger;
import org.jtrans2quik.loader.Trans2QuikLibraryLoader;
import org.jtrans2quik.wrapper.Trans2QuikLibrary;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Connector {
	private static Connector instance;
	private static Trans2QuikLibrary quikAdapter = Trans2QuikLibraryLoader.LIBRARY;
	private static NativeLongByReference errorCode = new NativeLongByReference();
	private static byte[] buffer = new byte[255];
	private static String pathToQuik = "C:\\SBERBANK\\QUIK\\";
	private static Logger logger = Logger.getLogger(Connector.class);
	private static ThreadPoolExecutor connectionCallBackExecutor;

	private Connector() {
	}

	public static Connector getInstance() {
		if (instance == null) {
			instance = new Connector();
		}
		return instance;
	}

	static {
		connectionCallBackExecutor = new ThreadPoolExecutor(
			1, 1, 0L,
			TimeUnit.MILLISECONDS,
			new SynchronousQueue<Runnable>(),
			new RejectedExecutionHandler() {
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
					logger.info("connectionCallBackExecutor is singleThreadPool");
				}
			}
		);
	}

	{
		setConnectionCallBack();
	}

	public void connectToQuik() {
		logger.trace("connectToQuik");
		NativeLong returnCode = quikAdapter.TRANS2QUIK_CONNECT(pathToQuik, errorCode, buffer, buffer.length);
		Status status = Status.decode(returnCode.intValue());

		switch (status) {
		case TRANS2QUIK_SUCCESS:
			logger.info("Connection to QUIK terminal established.");
			break;
		case TRANS2QUIK_FAILED:
			logger.error("Connection to QUIK terminal failed. " + errorCode.getValue().intValue() + " " + Translator.byteToString(buffer));
			break;
		case TRANS2QUIK_QUIK_TERMINAL_NOT_FOUND:
			logger.fatal("Either INFO.EXE is missing in the specified directory or the external transaction processing service is not running on it. Please check QUIK terminal.");
			System.exit(0);
		case TRANS2QUIK_DLL_VERSION_NOT_SUPPORTED:
			logger.error("The used version of Trans2QUIK.dll is not supported by the specified INFO.EXE.");
			System.exit(0);
			break;

//      Documentation collision
//		case TRANS2QUIK_DLL_ALREADY_CONNECTED_TO_QUIK:
//			logger.warn("Connection to QUIK already established.");
//			break;

		default:
			logger.fatal(Status.UNKNOWN_VALUE + " " + returnCode.intValue());
			System.exit(0);
		}
	}

	public void disconnectFromQuik() {
		logger.trace("disconnectFromQuik");
		NativeLong returnCode = quikAdapter.TRANS2QUIK_DISCONNECT(errorCode, buffer, buffer.length);
		Status status = Status.decode(returnCode.intValue());

		switch (status) {
		case TRANS2QUIK_SUCCESS:
			logger.info("Connection of Trans2QUIK.dll library with QUIK workstation was broken successfully.");
			break;
		case TRANS2QUIK_FAILED:
			logger.error("An error occurred while disconnecting. " + errorCode.getValue().intValue() + " " + Translator.byteToString(buffer));
			break;
		case TRANS2QUIK_DLL_NOT_CONNECTED:
			logger.error("Attempt to disconnect when connection is not established.");
			break;
		default:
			logger.fatal(Status.UNKNOWN_VALUE + " " + returnCode.intValue());
			System.exit(0);
		}
	}

	public void checkConnectionToServer() {
		logger.trace("checkConnectionToServer");
		NativeLong returnCode = quikAdapter.TRANS2QUIK_IS_QUIK_CONNECTED(errorCode, buffer, buffer.length);
		Status status = Status.decode(returnCode.intValue());

		switch (status) {
		case TRANS2QUIK_QUIK_CONNECTED:
			logger.info("Connection from QUIK terminal to server established.");
			break;
		case TRANS2QUIK_QUIK_NOT_CONNECTED:
			logger.error("Connection from QUIK terminal to server is not established.");
			break;
		case TRANS2QUIK_DLL_NOT_CONNECTED:
			logger.error("Trans2QUIK.dll was not connected to the QUIK terminal. In this case, it is impossible to verify the presence or absence of communication between the QUIK terminal and the server.");
			break;
		default:
			logger.fatal(Status.UNKNOWN_VALUE + " " + returnCode.intValue());
			System.exit(0);
		}
	}

	public void checkConnectionToQuik() {
		logger.trace("checkConnectionToQuik");
		NativeLong returnCode = quikAdapter.TRANS2QUIK_IS_DLL_CONNECTED(errorCode, buffer, buffer.length);
		Status status = Status.decode(returnCode.intValue());

		switch (status) {
		case TRANS2QUIK_DLL_CONNECTED:
			logger.info("Connection of the Trans2QUIK.dll library with the QUIK terminal is established.");
			break;
		case TRANS2QUIK_DLL_NOT_CONNECTED:
			logger.error("The connection of the Trans2QUIK.dll library with the QUIK terminal is not established.");
			break;
		default:
			logger.fatal(Status.UNKNOWN_VALUE + " " + returnCode.intValue());
			System.exit(0);
		}
	}

	private void setConnectionCallBack() {
		logger.trace("setConnectionCallBack");
		connectionCallBackExecutor.execute(new Runnable() {
			public void run() {
				while (true) {
					NativeLong returnCode = quikAdapter.TRANS2QUIK_SET_CONNECTION_STATUS_CALLBACK(
						new Trans2QuikLibrary.ConnectionStatusCallback() {
							public void callback(NativeLong nConnectionEvent, NativeLong nExtendedErrorCode, String lpcstrInfoMessage) {
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
									break;
								default:
									logger.fatal(Status.UNKNOWN_VALUE + " " + nConnectionEvent.intValue());
									System.exit(0);
								}
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
						waitingFor(1000);
					default:
						logger.fatal(Status.UNKNOWN_VALUE + " " + returnCode.intValue());
						System.exit(0);
					}
				}
			}
		});
	}

	private void waitingFor(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}