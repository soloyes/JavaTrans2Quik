package io.github.soloyes.quikbridge.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Application settings from {@code app.properties}.
 * <p>
 * Lookup order of {@link #load(String)}: the explicit path (CLI argument or {@code -Dquikbridge.config}),
 * then {@code ./app.properties}, then {@code app.properties} on the classpath. Files are read as UTF-8.
 * Problems raise {@link ConfigException} instead of exiting the JVM.
 */
public final class BridgeConfig {
	private static final Logger log = LoggerFactory.getLogger(BridgeConfig.class);

	public static final String FILE_NAME = "app.properties";
	public static final String PATH_PROPERTY = "quikbridge.config";

	public static final String QUIK_PATH = "quik.path";
	public static final String RECONNECT_INTERVAL_MS = "reconnect.interval.ms";
	public static final String RECONNECT_MAX_ATTEMPTS = "reconnect.max.attempts";
	public static final String HEARTBEAT_INTERVAL_MS = "heartbeat.interval.ms";
	public static final String RUN_DURATION_MS = "run.duration.ms";
	public static final String SUBSCRIBE_ORDERS = "subscribe.orders";
	public static final String SUBSCRIBE_TRADES = "subscribe.trades";

	private final Properties properties;
	private final String source;

	public BridgeConfig(Properties properties, String source) {
		this.properties = new Properties();
		this.properties.putAll(properties);
		this.source = source;
	}

	public static BridgeConfig fromProperties(Properties properties) {
		return new BridgeConfig(properties, "<in-memory>");
	}

	public static BridgeConfig load(String explicitPath) {
		String path = explicitPath != null ? explicitPath : System.getProperty(PATH_PROPERTY);
		if (path != null && !path.trim().isEmpty()) {
			Path p = Paths.get(path.trim());
			if (!Files.isRegularFile(p)) {
				throw new ConfigException("Config file not found: " + p.toAbsolutePath());
			}
			return fromFile(p);
		}
		Path cwd = Paths.get(FILE_NAME);
		if (Files.isRegularFile(cwd)) {
			return fromFile(cwd);
		}
		InputStream in = BridgeConfig.class.getClassLoader().getResourceAsStream(FILE_NAME);
		if (in == null) {
			throw new ConfigException("No " + FILE_NAME + " in the working directory or on the classpath");
		}
		return read(in, "classpath:" + FILE_NAME);
	}

	private static BridgeConfig fromFile(Path p) {
		try {
			return read(Files.newInputStream(p), p.toAbsolutePath().toString());
		} catch (IOException e) {
			throw new ConfigException("Cannot read " + p.toAbsolutePath(), e);
		}
	}

	private static BridgeConfig read(InputStream in, String source) {
		Properties props = new Properties();
		try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			props.load(r);
		} catch (IOException e) {
			throw new ConfigException("Cannot read " + source, e);
		}
		log.info("Configuration loaded from {}", source);
		return new BridgeConfig(props, source);
	}

	public String source() {
		return source;
	}

	public boolean has(String key) {
		return properties.getProperty(key) != null;
	}

	/** Trimmed value; throws if the key is absent. An empty value is allowed. */
	public String getString(String key) {
		String v = properties.getProperty(key);
		if (v == null) {
			throw new ConfigException("Missing property '" + key + "' in " + source);
		}
		return v.trim();
	}

	public String getString(String key, String defaultValue) {
		return has(key) ? getString(key) : defaultValue;
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		if (!has(key) || getString(key).isEmpty()) {
			return defaultValue;
		}
		String v = getString(key).toLowerCase();
		if (v.equals("true")) return true;
		if (v.equals("false")) return false;
		throw new ConfigException("Property '" + key + "' must be true or false, got '" + getString(key) + "'");
	}

	public long getLong(String key, long defaultValue) {
		if (!has(key) || getString(key).isEmpty()) {
			return defaultValue;
		}
		try {
			return Long.parseLong(getString(key));
		} catch (NumberFormatException e) {
			throw new ConfigException("Property '" + key + "' must be a whole number, got '" + getString(key) + "'");
		}
	}

	private long nonNegative(String key, long defaultValue) {
		long v = getLong(key, defaultValue);
		if (v < 0) {
			throw new ConfigException("Property '" + key + "' must be >= 0, got " + v);
		}
		return v;
	}

	/** Folder of the QUIK terminal (where info.exe is). Surrounding quotes are tolerated and removed. */
	public String quikPath() {
		String p = stripQuotes(getString(QUIK_PATH));
		if (p.isEmpty()) {
			throw new ConfigException("Property '" + QUIK_PATH + "' is empty");
		}
		return p;
	}

	public long reconnectIntervalMs() {
		return nonNegative(RECONNECT_INTERVAL_MS, 1000);
	}

	/** 0 = retry forever. */
	public long reconnectMaxAttempts() {
		return nonNegative(RECONNECT_MAX_ATTEMPTS, 0);
	}

	/** 0 = no heartbeat. */
	public long heartbeatIntervalMs() {
		return nonNegative(HEARTBEAT_INTERVAL_MS, 5000);
	}

	/** 0 = run until stopped (Ctrl+C / service stop). */
	public long runDurationMs() {
		return nonNegative(RUN_DURATION_MS, 0);
	}

	public boolean subscribeOrders() {
		return getBoolean(SUBSCRIBE_ORDERS, true);
	}

	public boolean subscribeTrades() {
		return getBoolean(SUBSCRIBE_TRADES, true);
	}

	static String stripQuotes(String s) {
		String t = s.trim();
		if (t.length() >= 2) {
			char a = t.charAt(0);
			char b = t.charAt(t.length() - 1);
			if ((a == '"' && b == '"') || (a == '\'' && b == '\'')) {
				return t.substring(1, t.length() - 1).trim();
			}
		}
		return t;
	}
}
