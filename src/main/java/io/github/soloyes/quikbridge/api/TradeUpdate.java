package io.github.soloyes.quikbridge.api;

/**
 * One call of TRANS2QUIK_TRADE_STATUS_CALLBACK: a fill of one of this account's orders.
 */
public final class TradeUpdate {
	private final long mode;
	private final double tradeNumber;
	private final double orderNumber;
	private final String classCode;
	private final String secCode;
	private final double price;
	private final long quantity;
	private final double value;
	private final boolean sell;

	public TradeUpdate(long mode, double tradeNumber, double orderNumber, String classCode, String secCode,
					   double price, long quantity, double value, boolean sell) {
		this.mode = mode;
		this.tradeNumber = tradeNumber;
		this.orderNumber = orderNumber;
		this.classCode = classCode;
		this.secCode = secCode;
		this.price = price;
		this.quantity = quantity;
		this.value = value;
		this.sell = sell;
	}

	/** 0 = new trade, 1 = initial snapshot in progress, 2 = last trade of the initial snapshot. */
	public long mode() { return mode; }
	public boolean isSnapshot() { return mode == 1 || mode == 2; }
	public double tradeNumber() { return tradeNumber; }
	public double orderNumber() { return orderNumber; }
	public String classCode() { return classCode; }
	public String secCode() { return secCode; }
	public double price() { return price; }
	public long quantity() { return quantity; }
	public double value() { return value; }
	public boolean sell() { return sell; }

	@Override
	public String toString() {
		return "Trade{" + classCode + ":" + secCode + " #" + (long) tradeNumber + " order=" + (long) orderNumber
			+ (sell ? " SELL " : " BUY ") + quantity + " @ " + price + (isSnapshot() ? " snapshot" : "") + "}";
	}
}
