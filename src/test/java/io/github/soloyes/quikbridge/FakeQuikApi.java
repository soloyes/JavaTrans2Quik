package io.github.soloyes.quikbridge;

import io.github.soloyes.quikbridge.api.CallResult;
import io.github.soloyes.quikbridge.api.OrderUpdate;
import io.github.soloyes.quikbridge.api.QuikApi;
import io.github.soloyes.quikbridge.api.Status;
import io.github.soloyes.quikbridge.api.TradeUpdate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * In-memory stand-in for TRANS2QUIK.dll. Scripts connect results, records every call and lets tests
 * fire the callbacks the DLL would fire.
 */
public final class FakeQuikApi implements QuikApi {
	public final List<String> calls = Collections.synchronizedList(new ArrayList<>());
	private final Deque<CallResult> connectResults = new ArrayDeque<>();
	private volatile boolean dllConnected;
	private volatile boolean serverConnected = true;
	private volatile ConnectionStatusListener statusListener;
	private volatile OrderListener orderListener;
	private volatile TradeListener tradeListener;

	/** Results returned by the next connect() calls; after they run out, connect() succeeds. */
	public FakeQuikApi scriptConnect(CallResult... results) {
		Collections.addAll(connectResults, results);
		return this;
	}

	public FakeQuikApi serverConnected(boolean v) {
		serverConnected = v;
		return this;
	}

	public boolean dllConnected() {
		return dllConnected;
	}

	public long count(String prefix) {
		synchronized (calls) {
			return calls.stream().filter(c -> c.startsWith(prefix)).count();
		}
	}

	@Override
	public synchronized CallResult connect(String quikPath) {
		calls.add("connect " + quikPath);
		CallResult r = connectResults.isEmpty() ? CallResult.of(Status.SUCCESS) : connectResults.poll();
		if (r.is(Status.SUCCESS) || r.is(Status.ALREADY_CONNECTED_TO_QUIK)) {
			dllConnected = true;
		}
		return r;
	}

	@Override
	public CallResult disconnect() {
		calls.add("disconnect");
		if (!dllConnected) {
			return CallResult.of(Status.DLL_NOT_CONNECTED);
		}
		dllConnected = false;
		return CallResult.of(Status.SUCCESS);
	}

	@Override
	public CallResult isDllConnected() {
		return CallResult.of(dllConnected ? Status.DLL_CONNECTED : Status.DLL_NOT_CONNECTED);
	}

	@Override
	public CallResult isQuikConnected() {
		if (!dllConnected) return CallResult.of(Status.DLL_NOT_CONNECTED);
		return CallResult.of(serverConnected ? Status.QUIK_CONNECTED : Status.QUIK_NOT_CONNECTED);
	}

	@Override
	public CallResult setConnectionStatusListener(ConnectionStatusListener listener) {
		calls.add("setConnectionStatusListener");
		statusListener = listener;
		return CallResult.of(Status.SUCCESS);
	}

	@Override
	public CallResult subscribeOrders(String classCode, String secCodes) {
		calls.add("subscribeOrders " + classCode + " " + secCodes);
		return CallResult.of(dllConnected ? Status.SUCCESS : Status.DLL_NOT_CONNECTED);
	}

	@Override
	public CallResult subscribeTrades(String classCode, String secCodes) {
		calls.add("subscribeTrades " + classCode + " " + secCodes);
		return CallResult.of(dllConnected ? Status.SUCCESS : Status.DLL_NOT_CONNECTED);
	}

	@Override
	public CallResult startOrders(OrderListener listener) {
		calls.add("startOrders");
		orderListener = listener;
		return CallResult.of(Status.SUCCESS);
	}

	@Override
	public CallResult startTrades(TradeListener listener) {
		calls.add("startTrades");
		tradeListener = listener;
		return CallResult.of(Status.SUCCESS);
	}

	@Override
	public CallResult unsubscribeOrders() {
		calls.add("unsubscribeOrders");
		return CallResult.of(Status.SUCCESS);
	}

	@Override
	public CallResult unsubscribeTrades() {
		calls.add("unsubscribeTrades");
		return CallResult.of(Status.SUCCESS);
	}

	// --- what the DLL would do ---

	/** The terminal went away: DLL state drops and the status callback fires. */
	public void dropDll() {
		dllConnected = false;
		fireStatus(Status.DLL_DISCONNECTED);
	}

	public void fireStatus(Status s) {
		ConnectionStatusListener l = statusListener;
		if (l != null) l.onConnectionStatus(s, 0, "");
	}

	public void fireOrder(OrderUpdate o) {
		orderListener.onOrder(o);
	}

	public void fireTrade(TradeUpdate t) {
		tradeListener.onTrade(t);
	}
}
