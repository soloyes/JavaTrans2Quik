package main;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuration {
	private static Configuration instance = new Configuration();
	private static Properties properties;

	private static Logger logger = Logger.getLogger(Configuration.class);

	private Configuration() {
		properties = new Properties();
		try {
			properties.load(new FileInputStream(new File("C:\\Users\\zheny\\IdeaProjects\\JavaTrans2Quik\\src\\main\\resources\\app.properties")));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Long getLong(String key) {
		checkValue(key);
		Long value;
		try {
			value = Long.valueOf(properties.getProperty(key));
		} catch (NumberFormatException e) {
			value = 0L;
			logger.warn(key + " has wrong value. Set to default 0");
		}
		return value;
	}

	public static String getString(String key) {
		checkValue(key);
		return properties.getProperty(key);
	}

	public static boolean getBoolean(String key) {
		checkValue(key);
		return Boolean.valueOf(properties.getProperty(key));
	}

	private static void checkValue(String key) {
		if (properties.getProperty(key) == null) {
			logger.error("There is no property " + key, new IllegalArgumentException());
			System.exit(0);
		}
	}
}
