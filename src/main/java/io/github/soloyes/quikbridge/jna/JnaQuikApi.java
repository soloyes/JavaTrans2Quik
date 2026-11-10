package io.github.soloyes.quikbridge.jna;

import com.sun.jna.NativeLong;
import com.sun.jna.ptr.NativeLongByReference;
import io.github.soloyes.quikbridge.api.CStrings;
import io.github.soloyes.quikbridge.api.CallResult;
import io.github.soloyes.quikbridge.api.OrderUpdate;
import io.github.soloyes.quikbridge.api.QuikApi;
import io.github.soloyes.quikbridge.api.Status;
import io.github.soloyes.quikbridge.api.TradeUpdate;
import org.jtrans2quik.loader.Trans2QuikLibraryLoader;
import org.jtrans2quik.wrapper.Trans2QuikLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link QuikApi} on top of the upstream JNA wrapper ({@link Trans2QuikLibrary}).
 * <p>
 * Two details that matter with a real DLL:
 * <ul>
 *     <li>JNA callback objects are kept in fields. If only the DLL holds them, the JVM can collect them
 *     and the next callback from QUIK crashes the process.</li>
 *     <li>Every call gets its own error buffer and calls are serialised, so concurrent callers
 *     (connector thread, heartbeat, shutdown hook) never share native out-parameters.</li>
 * </ul>
 * Listener exceptions are caught and logged: an exception must not unwind into the DLL's thread.
 */
public final class JnaQuikApi implements QuikApi {
	private static final Logger log = LoggerFactory.getLogger(JnaQuikApi.class);
	private static final int ERROR_BUFFER_SIZE = 512;

	private final Trans2QuikLibrary lib;

	// Strong references to callbacks handed to the DLL (see class javadoc).
	private volatile Trans2QuikLibrary.ConnectionStatusCallback connectionCallback;
	private volatile Trans2QuikLibrary.OrderStatusCallback orderCallback;
	private volatile Trans2QuikLibrary.TradeStatusCallback tradeCallback;

	/** Loads TRANS2QUIK.dll from {@code jna.library.path} / {@code PATH}. Windows only. */
	public JnaQuikApi() {
		this(Trans2QuikLibraryLoader.LIBRARY);
	}

	public JnaQuikApi(Trans2QuikLibrary lib) {
		this.lib = lib;
	}

	private interface ErrCall {
		NativeLong call(NativeLongByReference err, byte[] buf, int size);
	}

	private synchronized CallResult withError(ErrCall call) {
		NativeLongByReference err = new NativeLongByReference(new NativeLong(0));
		byte[] buf = new byte[ERROR_BUFFER_SIZE];
		NativeLong rc = call.call(err, buf, buf.length);
		return new CallResult(rc.longValue(), err.getValue().longValue(), CStrings.decode(buf));
	}

	private static CallResult plain(NativeLong rc) {
		return new CallResult(rc.longValue(), 0, "");
	}

	@Override
	public CallResult connect(String quikPath) {
		return withError((e, b, n) -> lib.TRANS2QUIK_CONNECT(quikPath, e, b, n));
	}

	@Override
	public CallResult disconnect() {
		return withError(lib::TRANS2QUIK_DISCONNECT);
	}

	@Override
	public CallResult isDllConnected() {
		return withError(lib::TRANS2QUIK_IS_DLL_CONNECTED);
	}

	@Override
	public CallResult isQuikConnected() {
		return withError(lib::TRANS2QUIK_IS_QUIK_CONNECTED);
	}

	@Override
	public CallResult setConnectionStatusListener(ConnectionStatusListener listener) {
		Trans2QuikLibrary.ConnectionStatusCallback cb = (event, ext, msg) -> {
			try {
				listener.onConnectionStatus(Status.decode(event.longValue()), ext.longValue(), msg == null ? "" : msg);
			} catch (RuntimeException e) {
				log.error("Connection status listener failed", e);
			}
		};
		connectionCallback = cb;
		return withError((e, b, n) -> lib.TRANS2QUIK_SET_CONNECTION_STATUS_CALLBACK(cb, e, b, n));
	}

	@Override
	public synchronized CallResult subscribeOrders(String classCode, String secCodes) {
		return plain(lib.TRANS2QUIK_SUBSCRIBE_ORDERS(classCode, secCodes));
	}

	@Override
	public synchronized CallResult subscribeTrades(String classCode, String secCodes) {
		return plain(lib.TRANS2QUIK_SUBSCRIBE_TRADES(classCode, secCodes));
	}

	@Override
	public synchronized CallResult startOrders(OrderListener listener) {
		Trans2QuikLibrary.OrderStatusCallback cb =
			(mode, transId, number, classCode, secCode, price, balance, value, isSell, status, descriptor) -> {
				try {
					listener.onOrder(new OrderUpdate(mode.longValue(), Integer.toUnsignedLong(transId), number,
						classCode, secCode, price, balance.longValue(), value, isSell.longValue() != 0, status.longValue()));
				} catch (RuntimeException e) {
					log.error("Order listener failed", e);
				}
			};
		orderCallback = cb;
		return plain(lib.TRANS2QUIK_START_ORDERS(cb));
	}

	@Override
	public synchronized CallResult startTrades(TradeListener listener) {
		Trans2QuikLibrary.TradeStatusCallback cb =
			(mode, number, orderNumber, classCode, secCode, price, qty, value, isSell, descriptor) -> {
				try {
					listener.onTrade(new TradeUpdate(mode.longValue(), number, orderNumber, classCode, secCode,
						price, qty.longValue(), value, isSell.longValue() != 0));
				} catch (RuntimeException e) {
					log.error("Trade listener failed", e);
				}
			};
		tradeCallback = cb;
		return plain(lib.TRANS2QUIK_START_TRADES(cb));
	}

	@Override
	public synchronized CallResult unsubscribeOrders() {
		return plain(lib.TRANS2QUIK_UNSUBSCRIBE_ORDERS());
	}

	@Override
	public synchronized CallResult unsubscribeTrades() {
		return plain(lib.TRANS2QUIK_UNSUBSCRIBE_TRADES());
	}
}
