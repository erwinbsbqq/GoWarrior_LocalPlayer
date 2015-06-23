package com.nmp.support;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

public class NmpConfReader {

	private final String TAG = "NmpConfReader";
	private Vector<NmpUrlTag> m_allTags = null;

	public NmpConfReader() {
		m_allTags = new Vector<NmpUrlTag>();
	}

	public boolean loadConf(String xmlData) {
		xmlData = xmlData.replaceAll("&", "&amp;");
//		Log.d(TAG, xmlData);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
			StringReader sr = new StringReader(xmlData);
			InputSource is = new InputSource(sr);
			Document doc = builder.parse(is);
			Element rootElement = doc.getDocumentElement();
			NodeList apps = rootElement.getElementsByTagName("app");
			for (int i = 0; i < apps.getLength(); i++) {
				Node item = apps.item(i);
				NmpUrlTag tag = new NmpUrlTag();
				buildTagMap(tag, item);
				m_allTags.add(tag);
			}

		} catch (ParserConfigurationException e) {
			Log.e(TAG, Utility.getMessage(e));
		} catch (SAXException e) {
            Log.e(TAG, Utility.getMessage(e));
		} catch (IOException e) {
            Log.e(TAG, Utility.getMessage(e));
		}
		return true;
	}

	boolean buildTagMap(NmpUrlTag tag, Node node) {
		tag.clear();
		String attrName = node.getAttributes().item(0).getNodeName();
		if (attrName.equals("id")) {
			String appID = node.getAttributes().item(0).getNodeValue();
			tag.setAppID(appID);
		}
		NodeList properties = node.getChildNodes();
		HashMap<String, Vector<String>> tagMap = new HashMap<String, Vector<String>>();
		for (int j = 0; j < properties.getLength(); j++) {
			HashMap<String, Vector<String>> tmpMap = new HashMap<String, Vector<String>>();
			Node property = properties.item(j);
			String nodeName = property.getNodeName();
			String nodeValue = null;
			Vector<String> v = new Vector<String>();
			if (property.getFirstChild() != null) {
				nodeValue = property.getFirstChild().getNodeValue();
				v.add(nodeValue);
			}
			if (nodeValue != null)
				tagMap.put(nodeName, v);
		}
		tag.setTagMap(tagMap);
		return true;
	}

	public Vector<NmpUrlTag> getAllTags() {
		return m_allTags;
	}

}
