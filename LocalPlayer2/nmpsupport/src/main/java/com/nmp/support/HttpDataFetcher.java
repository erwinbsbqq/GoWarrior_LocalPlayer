package com.nmp.support;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class HttpDataFetcher {
    private static final String TAG = "HttpDataFetcher";
	private HttpClient httpClient;
	private HttpParams httpParams;

	public HttpDataFetcher() {
		super();
		this.httpParams = new BasicHttpParams();
		String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2) Gecko/20100115 Firefox/3.6";
		HttpProtocolParams.setUserAgent(httpParams, userAgent);
		HttpConnectionParams.setConnectionTimeout(httpParams, 20 * 1000);
		HttpConnectionParams.setSoTimeout(httpParams, 20 * 1000);
		HttpConnectionParams.setSocketBufferSize(httpParams, 16 * 1024);
		HttpClientParams.setRedirecting(httpParams, true);

		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		schReg.register(new Scheme("https",
				SSLSocketFactory.getSocketFactory(), 80));

		ThreadSafeClientConnManager connManager = new ThreadSafeClientConnManager(
				httpParams, schReg);

		httpClient = new DefaultHttpClient(connManager, httpParams);
	}

//	private void setProxy(){
//        Properties props = System.getProperties();
//        props.setProperty("proxySet", "true");
//        props.setProperty("http.proxyHost", "192.168.9.1");
//        props.setProperty("http.proxyPort", "3128");
//        Authenticator.setDefault(new Authenticator() {
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication("johnny.he", new String("0000").toCharArray());
//            }
//        });
//    }

	public String post(String url, String xmlData) {
		String retData = "";
		HttpPost postMethod = new HttpPost(url);
		StringEntity se = null;
		try {
			se = new StringEntity(xmlData, HTTP.UTF_8);
			se.setContentType("text/xml charset=utf-8");
		} catch (UnsupportedEncodingException e) {
            Log.e(TAG, Utility.getMessage(e));
		}
		postMethod.addHeader("Content-type", "text/xml; charset=utf-8");
		postMethod.setEntity(se);
		
		try {
			HttpResponse httpResponse = httpClient.execute(postMethod);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				retData = EntityUtils.toString(httpResponse.getEntity());
			} else {
				retData = httpResponse.getStatusLine().toString();
			}
		} catch (ClientProtocolException e) {
            Log.e(TAG, Utility.getMessage(e));
		} catch (IOException e) {
            Log.e(TAG, Utility.getMessage(e));
		}
		
		return retData;
	}

	public String get(String url) {
		String retData = "";
		HttpGet getMethod = new HttpGet(url);
		getMethod.addHeader("Content-type", "text/xml; charset=utf-8");
		try {
			HttpResponse httpResponse = httpClient.execute(getMethod);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				retData = EntityUtils.toString(httpResponse.getEntity());
			} else {
				retData = httpResponse.getStatusLine().toString();
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG, Utility.getMessage(e));
		} catch (IOException e) {
            Log.e(TAG, Utility.getMessage(e));
		}
		
		return retData;
	}

	public void closeHttpClient() {
		httpClient.getConnectionManager().shutdown();
	}
}
