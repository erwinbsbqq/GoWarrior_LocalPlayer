package com.nmp.support;

import android.os.Environment;

public class Global {

    public final static String FILE_URL_CONF_IN_RAM = "/dev/LocalUrl.conf";
    public final static String FILE_URL_CONF_IN_FLASH = "/data/com.nmp.myplayer/LocalUrl.conf"; 
    public final static String FILE_URL_CONF_IN_ROM = "/system/tango/DefaultUrl.conf"; 

    public final static String FILE_NMPSP_INFO = "/dev/NMPSP.info";

    public final static String FILE_NMPSP_CONF = "/data/com.nmp.myplayer/NMPSP.conf";
    
    public final static String FILE_SETTINGS = Environment.getExternalStorageDirectory().getAbsolutePath() + "/com.nmp.myplayer/Settings.conf";

    public final static boolean SUPPORT_MULTI_LANGUAGE = true;
}
