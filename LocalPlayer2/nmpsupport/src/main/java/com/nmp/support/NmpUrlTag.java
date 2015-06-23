package com.nmp.support;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import android.util.Log;

public class NmpUrlTag {

	private final String TAG = "NmpUrlTag";
	private String m_appID;
	private HashMap<String,Vector<String>> m_tagMap;
	private Vector<String> m_tagNameList;
	public NmpUrlTag() {
		m_tagMap = new HashMap<String, Vector<String>>();
		m_tagNameList = new Vector<String>();
	}
	public String getAppID(){
		return m_appID;
	}

	public void setAppID(String appID){
		m_appID = appID;
	}
	
	public Vector<String> getTagNameList(){
		return m_tagNameList;
	}
	
	private void setTagNameList(Vector<String> tagNameList){
		m_tagNameList = tagNameList;
	}
	
	public void setTagMap(HashMap<String,Vector<String>> tagMap){

		m_tagMap = tagMap;
		Vector<String> tagNameList = new Vector<String>();
		for(String key:m_tagMap.keySet()){
//			Log.d(TAG, "key=" + key + " value=" + m_tagMap.get(key).get(0));
			tagNameList.add(key);
		}
		setTagNameList(tagNameList);
	}
	
	public HashMap<String,Vector<String>> getTagMap(){
		return m_tagMap;
	}
	
	public boolean getTagValueListByTagName(String tagName,Vector<String> tagValueList){
		if(!m_tagMap.containsKey(tagName))
			return false;
		tagValueList.add(m_tagMap.get(tagName).get(0));
		return true;
	}
	
	public void what() {
		Log.d(TAG, "appID = "+m_appID);
		
		Iterator it = m_tagNameList.iterator();
		while(it.hasNext()){
			String name = (String)it.next();
			Log.d(TAG, "  tagName = "+name);
			Vector<String> tagValueList =new Vector<String>();
			if(getTagValueListByTagName(name, tagValueList)) {
				Log.d(TAG, "      tagValue = "+tagValueList.get(0));
			}
		}
	}

	public void clear(){
		m_tagMap.clear();
		m_tagNameList.clear();
	}
}
