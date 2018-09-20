package com.polyspot.utils.bytescounting;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.execchain.CountingResponseEntityProxy;
import org.apache.http.protocol.HttpContext;

import com.polyspot.utils.CloseableHttpClientUtil;

public class CloseableHttpClientUtilCountingBytes extends CloseableHttpClientUtil {

	private static final String HEADER_COUNTING_ENTITY_WRAPPER_UID = "entityHashCode";
	private static Map<String, CountingResponseEntityProxy> countingInputStreamMap = new ConcurrentHashMap<String, CountingResponseEntityProxy>();
	
	public HttpResponseContent retrieveContentCountingBytesByGet(CloseableHttpClient httpClient, String targetUrl) throws Exception
	{
		return retrieveContentCountingBytesByGet(httpClient, targetUrl, HttpStatus.SC_OK);
	}
	
	public HttpResponseContent retrieveContentCountingBytesByGet(CloseableHttpClient httpClient, String targetUrl, int ... expectedStatusCodes) throws Exception
	{
		// Url encode the spaces
		targetUrl = cleanSpecialChars(targetUrl);
		return retrieveContentCountingBytes(httpClient, new HttpGet(targetUrl), expectedStatusCodes);
	}
	
	public HttpResponseContent retrieveContentCountingBytes(CloseableHttpClient httpClient, HttpRequestBase httpRequest, int ... expectedStatusCodes) throws Exception 
	{
		return retrieveContentCountingBytes(httpClient, httpRequest, null, expectedStatusCodes);
	}
	
	public HttpResponseContent retrieveContentCountingBytes(CloseableHttpClient httpClient, String targetUrl, Map<String, Iterable<String>> params) throws Exception {
		return retrieveContentCountingBytes(httpClient, new HttpGet(targetUrl), params,  HttpStatus.SC_OK);
	}
	
	public HttpResponseContent retrieveContentCountingBytes(CloseableHttpClient httpClient, String targetUrl, Map<String, Iterable<String>> params, int ... expectedStatusCodes) throws Exception {
		return retrieveContentCountingBytes(httpClient, new HttpGet(targetUrl), params, expectedStatusCodes);
	}
	
	public HttpResponseContent retrieveContentCountingBytes(CloseableHttpClient httpClient, HttpRequestBase httpRequest, Map<String, Iterable<String>> params, int ... expectedStatusCodes) throws Exception 
	{
		addParameters(httpRequest, params);
		
		String response = null;
		int reponseContentLength = -1;
		CloseableHttpResponse httpResponse = null;
		try
		{
			if(log.isDebugEnabled())
				log.debug("Going to call " + httpRequest.getRequestLine().toString());
			
			httpResponse = httpClient.execute(httpRequest, httpClientContext);

			if(log.isDebugEnabled()) {
				Header[] headers = httpResponse.getAllHeaders();
				if(headers != null) {
					for(Header header : headers) {
						log.debug(header.getName() + ": " + header.getValue());
					}
				}
			}

			response = retrieveResponseContent(httpResponse, expectedStatusCodes);
			
			reponseContentLength = retrieveResponseBytesCount(httpResponse);
			
			handleHttpResponseStatusCheck(httpRequest, response, httpResponse, expectedStatusCodes);
		}
		finally
		{
			if(httpResponse != null)
				httpResponse.close();
			
			httpRequest.releaseConnection();
		}
		return new HttpResponseContent(response, reponseContentLength, httpResponse);
	}
	
	private int retrieveResponseBytesCount(CloseableHttpResponse httpResponse) {
		int bytesCount = -1;
		if(httpResponse != null && httpResponse.getHeaders(HEADER_COUNTING_ENTITY_WRAPPER_UID) != null && httpResponse.getHeaders(HEADER_COUNTING_ENTITY_WRAPPER_UID).length > 0)
		{
			String entityHashCode = httpResponse.getHeaders(HEADER_COUNTING_ENTITY_WRAPPER_UID)[0].getValue();
			if(StringUtils.isNotEmpty(entityHashCode)) {
				if(countingInputStreamMap.get(entityHashCode) != null) { 
					if(countingInputStreamMap.get(entityHashCode).getCountingInputStream() != null) {
						bytesCount = countingInputStreamMap.get(entityHashCode).getCountingInputStream().getCount();
					}
					countingInputStreamMap.remove(entityHashCode);
				}
				else
					log.warn("Can not find a countingInputStream with key: " + entityHashCode);
			}
		}
		return bytesCount;
	}
	
	public static HttpResponseInterceptor buildCountingResponseInterceptor() {
		return new HttpResponseInterceptor() {
			@Override
			public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
				CountingResponseEntityProxy countingEntityWrapper = new CountingResponseEntityProxy(response.getEntity(), null); 
				response.setEntity(countingEntityWrapper);
				
				String entityUid = UUID.randomUUID().toString();
				response.setHeader(HEADER_COUNTING_ENTITY_WRAPPER_UID, entityUid);
				countingInputStreamMap.put(entityUid, countingEntityWrapper);
			}
		};
	}
	
	public static void main (String args[]) throws Exception {
		
		String[] urls = {"http://fr.linkedin.com/pub/dir/Karine/\\(levents\\)/", 
				"https://uk.linkedin.com/pub/dir/Srinivas kalyan/Chakravarthy", 
				"http://fr.linkedin.com/pub/dir/Marques/[banquebcp]/",
				"http://fr.linkedin.com/pub/dir/Yoann/Aubeuf+hacquin+webmaster+|+referenceur/",
				"http://fr.linkedin.com/pub/dir/Je+t&#39;aime/Je+t&#39;aime/",
				"https://uk.linkedin.com/pub/dir/Ryan/Du plessis"};
		
		for(String url : urls)
		{
			System.out.println(extractHostname(url));
		}
		/*
		CloseableHttpClient newHttpClient = HttpClients.custom()
				.addInterceptorFirst(buildCountingResponseInterceptor())
				.build();

		CloseableHttpClientUtilCountingBytes httpClientUtil = new CloseableHttpClientUtilCountingBytes();
		// 15,8 KB
		
		System.out.println(httpClientUtil.retrieveContentCountingBytesByGet(newHttpClient, "http://backend1.customermatrix.com:8180/back-end-rest-api/ll"));
		*/
	}


}
