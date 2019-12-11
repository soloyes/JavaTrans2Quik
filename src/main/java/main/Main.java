package main;

import org.apache.log4j.Logger;

public class Main {
	private static Logger logger = Logger.getLogger(Main.class);

	public static void main(String[] args) {
		logger.info("setConnectionCallBack");
		Connector.getInstance().setConnectionCallBack();
		try {
			Thread.sleep(1000);
			logger.info("connectToQuik");
			Connector.getInstance().connectToQuik();

			Thread.sleep(1000);
			logger.info("setConnectionCallBack");
			Connector.getInstance().setConnectionCallBack();

			Thread.sleep(1000);
			logger.info("disconnectFromQuik");
			Connector.getInstance().disconnectFromQuik();

			Thread.sleep(1000);
			logger.info("setConnectionCallBack");
			Connector.getInstance().setConnectionCallBack();

			Thread.sleep(1000);
			logger.info("disconnectFromQuik");
			Connector.getInstance().disconnectFromQuik();

			Thread.sleep(1000);
			logger.info("disconnectFromQuik");
			Connector.getInstance().disconnectFromQuik();

			Thread.sleep(1000);
			logger.info("checkConnectionToServer");
			Connector.getInstance().checkConnectionToServer();

			Thread.sleep(1000);
			logger.info("checkConnectionToQuik");
			Connector.getInstance().checkConnectionToQuik();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}