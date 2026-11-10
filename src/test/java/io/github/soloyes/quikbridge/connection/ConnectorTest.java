package io.github.soloyes.quikbridge.connection;

import io.github.soloyes.quikbridge.demo.SimulatedQuikApi;
import io.github.soloyes.quikbridge.api.CallResult;
import io.github.soloyes.quikbridge.api.QuikException;
import io.github.soloyes.quikbridge.api.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectorTest {
	private final List<Long> sleeps = new ArrayList<>();
	private final Sleeper recordingSleeper = sleeps::add;

	@AfterEach
	void clearInterrupt() {
		Thread.interrupted();
	}

	private Connector connector(SimulatedQuikApi api, long maxAttempts) {
		return new Connector(api, "C:\\QUIK", 250, maxAttempts, recordingSleeper);
	}

	@Test
	void retriesWhileTerminalIsNotUpThenConnects() {
		SimulatedQuikApi api = new SimulatedQuikApi().scriptConnect(
			CallResult.of(Status.QUIK_TERMINAL_NOT_FOUND),
			CallResult.failed(7, "terminal busy"),
			CallResult.of(Status.QUIK_TERMINAL_NOT_FOUND));

		assertTrue(connector(api, 0).connect());
		assertEquals(4, api.count("connect C:\\QUIK"));
		assertEquals(List.of(250L, 250L, 250L), sleeps);
	}

	@Test
	void alreadyConnectedCountsAsConnected() {
		SimulatedQuikApi api = new SimulatedQuikApi().scriptConnect(CallResult.of(Status.ALREADY_CONNECTED_TO_QUIK));
		assertTrue(connector(api, 0).connect());
		assertTrue(sleeps.isEmpty());
	}

	@Test
	void unsupportedDllVersionIsFatalAndNotRetried() {
		// The 2019 code fell through from this case into "unknown status -> System.exit(0)".
		SimulatedQuikApi api = new SimulatedQuikApi().scriptConnect(CallResult.of(Status.DLL_VERSION_NOT_SUPPORTED));
		QuikException e = assertThrows(QuikException.class, () -> connector(api, 0).connect());
		assertTrue(e.getMessage().contains("not supported"));
		assertEquals(1, api.count("connect"));
		assertTrue(sleeps.isEmpty());
	}

	@Test
	void undocumentedResultIsFatal() {
		SimulatedQuikApi api = new SimulatedQuikApi().scriptConnect(new CallResult(99, 0, ""));
		assertThrows(QuikException.class, () -> connector(api, 0).connect());
	}

	@Test
	void givesUpAfterMaxAttemptsWithoutSleepingAfterTheLast() {
		SimulatedQuikApi api = new SimulatedQuikApi().scriptConnect(
			CallResult.of(Status.FAILED), CallResult.of(Status.FAILED), CallResult.of(Status.FAILED));
		assertFalse(connector(api, 3).connect());
		assertEquals(3, api.count("connect"));
		assertEquals(2, sleeps.size());
	}

	@Test
	void interruptDuringBackoffStopsTheLoopAndKeepsTheFlag() {
		SimulatedQuikApi api = new SimulatedQuikApi().scriptConnect(CallResult.of(Status.FAILED), CallResult.of(Status.FAILED));
		Connector c = new Connector(api, "C:\\QUIK", 250, 0, ms -> {
			throw new InterruptedException();
		});
		assertFalse(c.connect());
		assertTrue(Thread.currentThread().isInterrupted());
		assertEquals(1, api.count("connect"));
	}

	@Test
	void decisionTable() {
		assertEquals(Connector.Decision.CONNECTED, Connector.decide(CallResult.of(Status.SUCCESS)));
		assertEquals(Connector.Decision.RETRY, Connector.decide(CallResult.of(Status.FAILED)));
		assertEquals(Connector.Decision.RETRY, Connector.decide(CallResult.of(Status.QUIK_TERMINAL_NOT_FOUND)));
		assertEquals(Connector.Decision.FATAL, Connector.decide(CallResult.of(Status.DLL_VERSION_NOT_SUPPORTED)));
		assertEquals(Connector.Decision.FATAL, Connector.decide(CallResult.of(Status.WRONG_INPUT_PARAMS)));
	}

	@Test
	void stateMachine() {
		ConnectionState s = ConnectionState.DLL_DISCONNECTED;
		assertEquals(ConnectionState.DLL_DISCONNECTED, s.on(Status.QUIK_CONNECTED), "server event without DLL is ignored");
		s = s.on(Status.DLL_CONNECTED);
		assertEquals(ConnectionState.DLL_CONNECTED, s);
		s = s.on(Status.QUIK_CONNECTED);
		assertEquals(ConnectionState.SERVER_CONNECTED, s);
		assertEquals(ConnectionState.SERVER_CONNECTED, s.on(Status.DLL_CONNECTED));
		assertEquals(ConnectionState.DLL_CONNECTED, s.on(Status.QUIK_DISCONNECTED));
		assertEquals(ConnectionState.DLL_DISCONNECTED, s.on(Status.DLL_DISCONNECTED));
		assertEquals(ConnectionState.DLL_CONNECTED, ConnectionState.DLL_CONNECTED.on(Status.QUIK_DISCONNECTED));
	}
}
