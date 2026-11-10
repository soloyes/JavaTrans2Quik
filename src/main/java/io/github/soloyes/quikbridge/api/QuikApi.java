package io.github.soloyes.quikbridge.api;

/**
 * The part of TRANS2QUIK.dll the bridge uses, in plain Java types.
 * <p>
 * {@link io.github.soloyes.quikbridge.jna.JnaQuikApi} talks to the real DLL; tests use an in-memory fake,
 * so nothing outside the {@code jna} package ever touches JNA or needs Windows.
 */
public interface QuikApi {

	/** TRANS2QUIK_CONNECT: attach the DLL to the terminal installed in {@code quikPath} (folder with info.exe). */
	CallResult connect(String quikPath);

	/** TRANS2QUIK_DISCONNECT. */
	CallResult disconnect();

	/** TRANS2QUIK_IS_DLL_CONNECTED: DLL_CONNECTED or DLL_NOT_CONNECTED. */
	CallResult isDllConnected();

	/** TRANS2QUIK_IS_QUIK_CONNECTED: is the terminal connected to the broker's server. */
	CallResult isQuikConnected();

	/** TRANS2QUIK_SET_CONNECTION_STATUS_CALLBACK. */
	CallResult setConnectionStatusListener(ConnectionStatusListener listener);

	/**
	 * TRANS2QUIK_SUBSCRIBE_ORDERS. Empty class code = all classes; empty sec codes = all instruments of the class.
	 * {@code secCodes} is the DLL format: codes joined with {@code |}.
	 */
	CallResult subscribeOrders(String classCode, String secCodes);

	/** TRANS2QUIK_SUBSCRIBE_TRADES, same parameters as {@link #subscribeOrders}. */
	CallResult subscribeTrades(String classCode, String secCodes);

	/** TRANS2QUIK_START_ORDERS: start delivering orders of the subscribed classes/instruments. */
	CallResult startOrders(OrderListener listener);

	/** TRANS2QUIK_START_TRADES. */
	CallResult startTrades(TradeListener listener);

	/** TRANS2QUIK_UNSUBSCRIBE_ORDERS: drop all order subscriptions. */
	CallResult unsubscribeOrders();

	/** TRANS2QUIK_UNSUBSCRIBE_TRADES. */
	CallResult unsubscribeTrades();

	@FunctionalInterface
	interface ConnectionStatusListener {
		/**
		 * @param event one of QUIK_CONNECTED, QUIK_DISCONNECTED, DLL_CONNECTED, DLL_DISCONNECTED
		 */
		void onConnectionStatus(Status event, long extendedErrorCode, String message);
	}

	@FunctionalInterface
	interface OrderListener {
		void onOrder(OrderUpdate order);
	}

	@FunctionalInterface
	interface TradeListener {
		void onTrade(TradeUpdate trade);
	}
}
