package io.github.soloyes.quikbridge.api;

import java.nio.charset.Charset;

/**
 * Decoding of the zero-terminated byte buffers TRANS2QUIK.dll fills with error messages.
 */
public final class CStrings {
	/** QUIK writes its messages in the Windows ANSI code page for Russian. */
	public static final Charset QUIK_CHARSET = Charset.forName("windows-1251");

	private CStrings() {
	}

	/** Bytes up to the first zero (or the whole buffer if there is none), decoded as windows-1251 and trimmed. */
	public static String decode(byte[] buffer) {
		if (buffer == null) {
			return "";
		}
		int len = 0;
		while (len < buffer.length && buffer[len] != 0) {
			len++;
		}
		return new String(buffer, 0, len, QUIK_CHARSET).trim();
	}
}
