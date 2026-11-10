package org.jtrans2quik.loader;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import org.jtrans2quik.wrapper.Trans2QuikLibrary;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Arsentii Nerushev on 14.05.2015.
 * <p>
 * Fork changes (soloyes): {@code Native.load} instead of the deprecated {@code Native.loadLibrary} (JNA 5),
 * and strings are marshalled as windows-1251, the code page QUIK uses for its messages.
 * The DLL is loaded the first time this class is touched, so code that must run without
 * TRANS2QUIK.dll (tests, non-Windows machines) must not reference {@link #LIBRARY}.
 *
 * @version 1.0.1
 */
public final class Trans2QuikLibraryLoader {

    /** Code page of strings passed to and from TRANS2QUIK.dll. */
    public static final String QUIK_ENCODING = "windows-1251";

    public static final Trans2QuikLibrary LIBRARY;

    static {
        final Map<String, Object> options = new HashMap<String, Object>();
        options.put(Library.OPTION_FUNCTION_MAPPER, StdCallLibrary.FUNCTION_MAPPER);
        options.put(Library.OPTION_CALLING_CONVENTION, Function.C_CONVENTION);
        options.put(Library.OPTION_STRING_ENCODING, QUIK_ENCODING);
        LIBRARY = Native.load("TRANS2QUIK", Trans2QuikLibrary.class, options);
    }

    private Trans2QuikLibraryLoader() {
        throw new AssertionError("This should never be invoked.");
    }
}
