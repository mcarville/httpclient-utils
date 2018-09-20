package com.polyspot.utils;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class CloseableHttpClientUtil extends HttpClientUtil {

	public String retrieveContentByGet(CloseableHttpClient httpClient, String targetUrl) throws Exception
	{
		return retrieveContentByGet(httpClient, targetUrl, HttpStatus.SC_OK);
	}
	
	public String retrieveContentByGet(CloseableHttpClient httpClient, String targetUrl, int ... expectedStatusCodes) throws Exception
	{
		return retrieveContent(httpClient, new HttpGet(targetUrl), expectedStatusCodes);
	}
	
	public String retrieveContent(CloseableHttpClient httpClient, HttpRequestBase httpRequest, int ... expectedStatusCodes) throws Exception 
	{
		String response = null;
		CloseableHttpResponse httpResponse = null;
		try
		{
			if(log.isDebugEnabled())
				log.debug("Going to call " + httpRequest.getRequestLine().toString());
			
			httpResponse = httpClient.execute(httpRequest, httpClientContext);
			
			response = retrieveResponseContent(httpResponse, expectedStatusCodes);
			
			handleHttpResponseStatusCheck(httpRequest, response, httpResponse, expectedStatusCodes);
		}
		finally
		{
			if(httpResponse != null)
				httpResponse.close();
			
			httpRequest.releaseConnection();
		}
		return response;
	}
	
	public static void main (String args[]) throws Exception {
		CloseableHttpClient newHttpClient = HttpClients.createDefault();
		CloseableHttpClientUtil httpClientUtil = new CloseableHttpClientUtil();
		String response = httpClientUtil.retrieveContentByGet(newHttpClient, "https://bitbucket.org/tperdriau/schneider-electric/issues?status=new&status=open");
		System.out.println(response);
	}
}
