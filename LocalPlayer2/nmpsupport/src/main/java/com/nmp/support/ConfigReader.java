package com.nmp.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

import android.os.RemoteException;
import android.util.Log;

import com.nmp.service.IService;


public class ConfigReader {

	private static final String TAG = "ConfigReader";
//	private static final String DEFAULT_SERVER_URL = "http://szdev.3322.org:1288";
//	private static final String DEFAULT_APKUPG_ENTRY = "/store/app";
//	private static final String DEFAULT_USRLOGIN_ENTRY = "/ali/service/login?dev_token=${dev_token}&usrId=${usrId}&password=${password}";

	// Keep matched with string-arrays "languages" in arrays.xml 
    public static final Locale[] LangLocales = {
        Locale.SIMPLIFIED_CHINESE, 
        Locale.ENGLISH
    };
    public static final String[] LangCodes = {
        "", 
        "en"
    };
    private static final int DEFAULT_LANGUAGE = 0;


    public static String getInfoFromConf(String id, String tagName) {
        String ret = null;
        NmpUrlApi nmpUrlAPI = new NmpUrlApi();
        NmpUrlTag tag = new NmpUrlTag();

        if (!nmpUrlAPI.getServiceConf(id, tag)) {
            return ret;
        }

        Vector<String> tagValueList = new Vector<String>();
        if (tag.getTagValueListByTagName(tagName, tagValueList)) {
            ret = tagValueList.get(0);
        }

        return ret;
    }

	public static String getAPKUpgradePostUrl() {
		String url = null;
		url = getInfoFromConf("APKUpgrade", "urlEntry");
//		if (url == null)
//			return (DEFAULT_SERVER_URL + DEFAULT_APKUPG_ENTRY);
//		else
			return url;
	}

	public static String getAPKUpgradeLoginPostUrl() {
		String url = null;
		url = getInfoFromConf("UserLogin", "urlEntry");
//		if (url == null)
//			return (DEFAULT_SERVER_URL + DEFAULT_USRLOGIN_ENTRY);
//		else
			return url;
	}

	public static String getProperty(String file, String name, String defaultValue) {
    	String value = defaultValue;
    	
    	Properties prop = new Properties();
    	try {
    		FileInputStream fs = new FileInputStream(file); 
    		prop.load(fs);
    		value = prop.getProperty(name, defaultValue); 
        	Log.d(TAG, "getProperty: " + name + " = " + value);
			fs.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, Utility.getMessage(e));
		} catch (IOException e) {
			Log.e(TAG, Utility.getMessage(e));
		}
    	
    	if (value == null)
    	    value = "";
    	return value;
	}
	
    public static void setProperty(String file, String name, String value) {
        Properties prop = new Properties();
        try {
            FileInputStream fis = new FileInputStream(file); 
            prop.load(fis);
            fis.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, Utility.getMessage(e));
        } catch (IOException e) {
            Log.e(TAG, Utility.getMessage(e));
        }

        try {
            prop.setProperty(name, value);

            File dir = new File(file);
            dir.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            prop.store(fos, null);
            Log.d(TAG, "setProperty: " + name + " = " + value);
            fos.close();

            IService service = Utility.getInstance().getNmpService();
            service.executeCmd("sync " + dir.getParentFile().getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, Utility.getMessage(e));
        }
    }

    public static String getProperty(String file, String name) {
		return getProperty(file, name, "");
	}

    public static String getToken() {
    	return getProperty(Global.FILE_NMPSP_INFO, "token");
    }
    
    public static String getUserToken() {
    	return getProperty(Global.FILE_NMPSP_INFO, "usr_token");
    }
    
    public static int getLanguage() {
        if (Global.SUPPORT_MULTI_LANGUAGE) {
            String lang = getProperty(Global.FILE_SETTINGS, "language", String.valueOf(DEFAULT_LANGUAGE));
            return Integer.parseInt(lang);
        }
        return 0;
    }
    
    public static void setLanguage(int lang) {
        if (Global.SUPPORT_MULTI_LANGUAGE) {
            if ((lang >= 0) && (lang < LangLocales.length)) {
                setProperty(Global.FILE_SETTINGS, "language", String.valueOf(lang));
            }
        }
    }
    
    public static String getLanguageCode() {
        return LangCodes[getLanguage()];
    }

    public static String getSysProperty(String name, String defaultValue) {
        String value = defaultValue;
        IService service = Utility.getInstance().getNmpService();
        if (service != null) {
            try {
                value = service.getValue(name);
            } catch (RemoteException e) {
                Log.e(TAG, Utility.getMessage(e));
            }
        }
        if (value == null) {
            value = defaultValue;
        }
        Log.d(TAG, name + " = " + value);
        return value;
    }
    
    public static String getSysProperty(String name) {
        return getSysProperty(name, "");
    }
    
    public static String getUserName(){
        return getSysProperty("usr");
    }
    
    public static String getPassWord(){
        return getSysProperty("passwd");
    }
    
    public static String getAuthStatus(){
        return getSysProperty("skey");
    }
    
    public static boolean getUpgFlag() {
        return getSysProperty("upg").equals("on");
    }
    
    public static boolean setSysProperty(String name, String value) {
        boolean ok = false;
        IService service = Utility.getInstance().getNmpService();
        if (service != null) {
            try {
                if (value == null) {
                    service.delValue(name);
                } else {
                    service.setValue(name, value);
                }
                Log.d(TAG, String.format("setSysProperty, key=%s, value=%s", name, value));
                ok = true;
            } catch (RemoteException e) {
                Log.e(TAG, Utility.getMessage(e));
            }
        }
        
        return ok;
    }
    
    public static boolean setUpgFlag(boolean flag) {
        return setSysProperty("upg", flag?"on":null);
    }
}
