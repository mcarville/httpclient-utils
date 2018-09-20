package com.polyspot.utils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;

public class SSLUtil {
	
	// com.polyspot.utils.SSLUtil
	
	public static void disableSSLVerifications(HttpClient httpClient) throws NoSuchAlgorithmException, KeyManagementException 
	{
		X509TrustManager tm = new X509TrustManager() {
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			
			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
					String authType) throws java.security.cert.CertificateException {				
			}
			
			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
					String authType) throws java.security.cert.CertificateException {
			}
		};
		
		X509HostnameVerifier trustAllVerifier = new X509HostnameVerifier() {
            @Override
            public void verify(String string, SSLSocket ssls) throws IOException {
            }

            @Override
            public void verify(String string, X509Certificate xc) throws SSLException {
            }

            @Override
            public void verify(String string, String[] strings, String[] strings1) throws SSLException {
            }

            @Override
            public boolean verify(String string, SSLSession ssls) {
                return true;
            }
        };

		SSLContext sslcontext = SSLContext.getInstance("TLS");
		sslcontext.init(null, new TrustManager[]{tm}, null);
		SSLSocketFactory sf = new SSLSocketFactory(sslcontext, trustAllVerifier);
		
		httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, sf));
	}
	
	public static void main (String args[]) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException
	{
		HttpClient httpClient = new DefaultHttpClient();
		SSLUtil.disableSSLVerifications(httpClient);

		HttpGet httpGet = new HttpGet("https://api.linkedin.com/v1/");
		HttpResponse httpResponse = httpClient.execute(httpGet);
		System.out.println(httpResponse.getStatusLine().getStatusCode());
		httpGet.releaseConnection();
	}
}
