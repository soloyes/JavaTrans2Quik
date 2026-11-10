package io.github.soloyes.quikbridge.api;

/**
 * One call of TRANS2QUIK_ORDER_STATUS_CALLBACK: a new or changed order of this account.
 * The order descriptor is deliberately not kept: it is only valid inside the callback.
 */
public final class OrderUpdate {
	private final long mode;
	private final long transId;
	private final double orderNumber;
	private final String classCode;
	private final String secCode;
	private final double price;
	private final long balance;
	private final double value;
	private final boolean sell;
	private final long status;

	public OrderUpdate(long mode, long transId, double orderNumber, String classCode, String secCode,
					   double price, long balance, double value, boolean sell, long status) {
		this.mode = mode;
		this.transId = transId;
		this.orderNumber = orderNumber;
		this.classCode = classCode;
		this.secCode = secCode;
		this.price = price;
		this.balance = balance;
		this.value = value;
		this.sell = sell;
		this.status = status;
	}

	/** 0 = new order, 1 = initial snapshot in progress, 2 = last order of the initial snapshot. */
	public long mode() { return mode; }
	public boolean isSnapshot() { return mode == 1 || mode == 2; }
	public boolean isSnapshotEnd() { return mode == 2; }
	/** TRANS_ID of the transaction that placed the order (0 if placed outside this API). */
	public long transId() { return transId; }
	public double orderNumber() { return orderNumber; }
	public String classCode() { return classCode; }
	public String secCode() { return secCode; }
	public double price() { return price; }
	/** Quantity still unfilled. */
	public long balance() { return balance; }
	public double value() { return value; }
	public boolean sell() { return sell; }
	/** Raw nStatus: 1 = active, 2 = cancelled, anything else = filled. */
	public long status() { return status; }
	public boolean isActive() { return status == 1; }
	public boolean isCancelled() { return status == 2; }
	public boolean isFilled() { return status != 1 && status != 2; }

	@Override
	public String toString() {
		return "Order{" + classCode + ":" + secCode + " #" + (long) orderNumber
			+ (sell ? " SELL " : " BUY ") + "price=" + price + " left=" + balance
			+ " state=" + (isActive() ? "active" : isCancelled() ? "cancelled" : "filled")
			+ " transId=" + transId + (isSnapshot() ? " snapshot" : "") + "}";
	}
}
