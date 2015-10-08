package com.nmp.service;

import android.util.Log;

public class NMPUtils {
	private static final String LOGTAG = "NMPUtils";
	
	public native boolean nativeCheckInetCntStatus();
	public native String nativeReadItem(String key);
	public native boolean nativeWriteItem(String key, String value);
	public native boolean nativeDeleteItem(String key);
	
	static {
		Log.i(LOGTAG, "load library libnmptools.so");
		System.loadLibrary("nmptools");
	}
	
	public boolean checkInetCntStatus() {
		Log.i(LOGTAG, "check internet connect status");
		return nativeCheckInetCntStatus();
	}
	
	public String nmpReadItem(String key) {
		Log.i(LOGTAG, "nmpReadItem directly");
		return nativeReadItem(key);
	}
	
	public boolean nmpWriteItem(String key, String value) {
		Log.i(LOGTAG, "nmpWriteItem directly");
		return nativeWriteItem(key, value);
	}
	
	public boolean nmpDeleteItem(String key) {
		Log.i(LOGTAG, "nmpDeleteItem directly");
		return nativeDeleteItem(key);
	}
}
