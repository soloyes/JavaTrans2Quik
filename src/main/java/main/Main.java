package main;

public class Main {
	public static void main(String[] args) {
		try {
			Connector.getInstance().connectToQuik();

			Thread.sleep(1000);
			Connector.getInstance().disconnectFromQuik();

		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			Connector.getInstance().shutdwon();
		}
	}
}