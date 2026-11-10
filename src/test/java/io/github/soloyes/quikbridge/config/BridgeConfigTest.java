package io.github.soloyes.quikbridge.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class BridgeConfigTest {

	private static BridgeConfig config(String... kv) {
		Properties p = new Properties();
		for (int i = 0; i < kv.length; i += 2) {
			p.setProperty(kv[i], kv[i + 1]);
		}
		return BridgeConfig.fromProperties(p);
	}

	@Test
	void missingRequiredKeyThrowsInsteadOfExiting() {
		ConfigException e = assertThrows(ConfigException.class, () -> config().quikPath());
		assertTrue(e.getMessage().contains("quik.path"));
	}

	@Test
	void quikPathLosesSurroundingQuotes() {
		assertEquals("C:\\SBERBANK\\QUIK\\", config("quik.path", "\"C:\\SBERBANK\\QUIK\\\"").quikPath());
		assertEquals("C:\\QUIK", config("quik.path", " 'C:\\QUIK' ").quikPath());
		assertEquals("C:\\QUIK", config("quik.path", "C:\\QUIK").quikPath());
	}

	@Test
	void emptyQuikPathIsAnError() {
		assertThrows(ConfigException.class, () -> config("quik.path", "\"\"").quikPath());
	}

	@Test
	void defaultsApplyWhenKeysAreAbsentOrEmpty() {
		BridgeConfig c = config("heartbeat.interval.ms", "");
		assertEquals(1000, c.reconnectIntervalMs());
		assertEquals(0, c.reconnectMaxAttempts());
		assertEquals(5000, c.heartbeatIntervalMs());
		assertEquals(0, c.runDurationMs());
		assertTrue(c.subscribeOrders());
		assertTrue(c.subscribeTrades());
	}

	@Test
	void malformedNumbersAndBooleansAreRejected() {
		assertThrows(ConfigException.class, () -> config("reconnect.interval.ms", "1s").reconnectIntervalMs());
		assertThrows(ConfigException.class, () -> config("reconnect.interval.ms", "-5").reconnectIntervalMs());
		assertThrows(ConfigException.class, () -> config("subscribe.orders", "yes").subscribeOrders());
		assertFalse(config("subscribe.orders", " FALSE ").subscribeOrders());
	}

	@Test
	void loadsExplicitPathAsUtf8(@TempDir Path dir) throws Exception {
		Path f = dir.resolve("my.properties");
		Files.write(f, "quik.path=C:\\\\\u041a\u0432\u0438\u043a\nreconnect.interval.ms=250\n".getBytes(StandardCharsets.UTF_8));
		BridgeConfig c = BridgeConfig.load(f.toString());
		assertEquals("C:\\\u041a\u0432\u0438\u043a", c.quikPath());
		assertEquals(250, c.reconnectIntervalMs());
		assertEquals(f.toAbsolutePath().toString(), c.source());
	}

	@Test
	void explicitPathThatDoesNotExistIsAnError(@TempDir Path dir) {
		assertThrows(ConfigException.class, () -> BridgeConfig.load(dir.resolve("nope.properties").toString()));
	}

	@Test
	void fallsBackToClasspathWhenNoFileInWorkingDirectory() {
		assumeFalse(Files.exists(Paths.get(BridgeConfig.FILE_NAME)), "app.properties present in CWD");
		BridgeConfig c = BridgeConfig.load(null);
		assertEquals("classpath:app.properties", c.source());
		assertEquals("C:\\QUIK-from-classpath", c.quikPath());
	}
}
