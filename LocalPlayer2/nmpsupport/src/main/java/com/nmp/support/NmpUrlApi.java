package com.nmp.support;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import org.apache.http.util.EncodingUtils;

import android.util.Log;

public class NmpUrlApi {

    private final String TAG = "NmpUrlApi";
	private Vector<NmpUrlTag> m_tagList = null;
	
	public NmpUrlApi() {
	}

	public boolean getServiceConf() {
		if (readConfig(Global.FILE_URL_CONF_IN_RAM)) {
			return true;
		}
        Log.e(TAG, "readConfig failed at " + Global.FILE_URL_CONF_IN_RAM);
		
//		if (readConfig(Global.FILE_CONFIG_IN_FLASH)) {
//			return true;
//		}
//      Log.e(TAG, "readConfig failed at " + Global.FILE_CONFIG_IN_FLASH);
		
		if (readConfig(Global.FILE_URL_CONF_IN_ROM)) {
			return true;
		}
        Log.e(TAG, "readConfig failed at " + Global.FILE_URL_CONF_IN_ROM);

		return false;
	}

	public boolean getServiceConf(String appID, NmpUrlTag tag) {
		if (!getServiceConf()) {
			return false;
		}
		Iterator it = m_tagList.iterator();
		while (it.hasNext()) {
			NmpUrlTag tmpTag = (NmpUrlTag) it.next();
			if (tmpTag.getAppID().equals(appID)) {
				tag.setAppID(tmpTag.getAppID());
				tag.setTagMap(tmpTag.getTagMap());
				return true;
			}
		}
		return false;
	}

	public Vector<NmpUrlTag> getAllTagList() {
		return m_tagList;
	}

	private synchronized String readLocalFile(String fileName) {
		String res = null;
		try {
			FileInputStream fin = new FileInputStream(fileName);
			int length;

			length = fin.available();

			byte[] buffer = new byte[length];

			fin.read(buffer);

			res = EncodingUtils.getString(buffer, "UTF-8");

			fin.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, Utility.getMessage(e));
		}catch (IOException e) {
			Log.e(TAG, Utility.getMessage(e));
		}
		return res;
	}

	private boolean readConfig(String fileName) {
		String content;
		content = readLocalFile(fileName);
		if (content == null)
			return false;
		NmpConfReader reader = new NmpConfReader();
		reader.loadConf(content);
		m_tagList = reader.getAllTags();
		return true;
	}
}
