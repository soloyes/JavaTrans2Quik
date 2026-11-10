package io.github.soloyes.quikbridge.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusTest {

	@Test
	void decodesEveryDocumentedCode() {
		String[] names = {"SUCCESS", "FAILED", "QUIK_TERMINAL_NOT_FOUND", "DLL_VERSION_NOT_SUPPORTED",
			"ALREADY_CONNECTED_TO_QUIK", "WRONG_SYNTAX", "QUIK_NOT_CONNECTED", "DLL_NOT_CONNECTED",
			"QUIK_CONNECTED", "QUIK_DISCONNECTED", "DLL_CONNECTED", "DLL_DISCONNECTED",
			"MEMORY_ALLOCATION_ERROR", "WRONG_CONNECTION_HANDLE", "WRONG_INPUT_PARAMS"};
		for (int code = 0; code < names.length; code++) {
			Status s = Status.decode(code);
			assertEquals(names[code], s.name());
			assertEquals(code, s.code());
		}
	}

	@ParameterizedTest
	@ValueSource(longs = {-1, 15, 666, Long.MAX_VALUE})
	void unknownCodesDecodeToUnknownAndKeepRawValue(long code) {
		assertEquals(Status.UNKNOWN, Status.decode(code));
		CallResult r = new CallResult(code, 0, "");
		assertEquals(code, r.code());
		assertTrue(r.toString().contains(Long.toString(code)));
	}

	@Test
	void callResultToStringCarriesExtendedError() {
		CallResult r = CallResult.failed(42, "no connection");
		assertEquals(Status.FAILED, r.status());
		assertEquals("FAILED [ext=42, no connection]", r.toString());
	}

	@Test
	void cStringStopsAtZeroTerminatorAndDecodesWindows1251() {
		byte[] buf = new byte[64];
		// "Connection error" in Russian, as QUIK sends it in windows-1251
		byte[] text = "\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0435\u0434\u0438\u043d\u0435\u043d\u0438\u044f".getBytes(Charset.forName("windows-1251"));
		System.arraycopy(text, 0, buf, 0, text.length);
		buf[text.length + 2] = 'x'; // garbage after the terminator must be ignored
		assertEquals("\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0435\u0434\u0438\u043d\u0435\u043d\u0438\u044f", CStrings.decode(buf));
	}

	@Test
	void cStringWithoutTerminatorUsesWholeBufferAndNullIsEmpty() {
		assertEquals("abc", CStrings.decode(new byte[]{'a', 'b', 'c'}));
		assertEquals("", CStrings.decode(new byte[8]));
		assertEquals("", CStrings.decode(null));
	}
}
