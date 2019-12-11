package main;

import com.sun.jna.NativeLong;
import com.sun.jna.ptr.NativeLongByReference;
import org.jtrans2quik.loader.Trans2QuikLibraryLoader;
import org.jtrans2quik.wrapper.Trans2QuikLibrary;



public class Connector {
	private static Trans2QuikLibrary quikAdapter = Trans2QuikLibraryLoader.LIBRARY;
	private static NativeLongByReference errorCode = new NativeLongByReference();
	private static byte[] buffer = new byte[255];
	private static String pathToQuik = "C:\\SBERBANK\\QUIK\\";

	public static void connectToQuik() {
		NativeLong returnCode = quikAdapter.TRANS2QUIK_CONNECT(pathToQuik, errorCode, buffer, buffer.length);
		Status.decode(returnCode.intValue()).log();
	}

	public static void checkServerConnection() {
		NativeLong returnCode = quikAdapter.TRANS2QUIK_IS_QUIK_CONNECTED(errorCode, buffer, buffer.length);
		Status.decode(returnCode.intValue()).log();
	}
}
