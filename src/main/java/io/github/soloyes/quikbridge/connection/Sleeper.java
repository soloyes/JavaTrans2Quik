package io.github.soloyes.quikbridge.connection;

/** Pause between reconnect attempts. Tests replace it to run retries instantly. */
@FunctionalInterface
public interface Sleeper {
	void sleep(long millis) throws InterruptedException;

	Sleeper SYSTEM = Thread::sleep;
}
