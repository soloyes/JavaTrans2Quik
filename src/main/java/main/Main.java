package main;

public class Main {
	public static void main(String[] args) {
		try {
			Connector.getInstance().connectToQuik();

			Thread.sleep(1000);
			Connector.getInstance().disconnectFromQuik();

			Thread.sleep(1000);
			Connector.getInstance().disconnectFromQuik();

			Thread.sleep(1000);
			Connector.getInstance().disconnectFromQuik();

			Thread.sleep(1000);
			Connector.getInstance().checkConnectionToServer();

			Thread.sleep(1000);
			Connector.getInstance().checkConnectionToQuik();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}