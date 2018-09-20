package com.polyspot.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientUtil 
{	
	public static final String SLASH_STRING = "/";
	private static final String SPACE_STRING = " ";
	public static final String LOCATION_HEADER = "Location";
	private static final String CONNECTION_HEADER_NAME = "Connection";
	private static final String CONNECTION_HEADER_NAME_CLOSE_VALUE = "close";
	
	protected static Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

	protected static final String UTF_8_ENCODING = "UTF-8";
	protected String paramStreamEncoding = UTF_8_ENCODING;
	protected String inputStreamEncoding = UTF_8_ENCODING;
	protected List<Header> reccurentHeaders;
	protected HttpClientContext httpClientContext;

	public static final String HEADER_USER_AGENT = "User-Agent";
	
	public static final String HTTPS_PROTOCOL = "https://";
	public static final String HTTP_PROTOCOL = "http://";
	private static final String PROTOCOL_END = HTTP_PROTOCOL.substring(4);
	
	protected boolean throwingExceptionOnStatusCodes = true;
	protected boolean alwaysRetrievingResponseContent = false;
	
	protected static Set<Integer> HTTP_REDIRECT_CODES = new HashSet<Integer>();
	
	static {
		HTTP_REDIRECT_CODES.add(HttpStatus.SC_MOVED_PERMANENTLY);
		HTTP_REDIRECT_CODES.add(HttpStatus.SC_MOVED_TEMPORARILY);
		HTTP_REDIRECT_CODES.add(HttpStatus.SC_TEMPORARY_REDIRECT);
		HTTP_REDIRECT_CODES.add(308);
	}
	
	private static final String URL_PARAMETERS_BEGIN = "?";
	
	protected  void addParameters(HttpRequestBase method, Map<String, Iterable<String>> params) throws Exception 
	{
		if(method instanceof HttpPost)
			addParameters((HttpPost)method, params);
		else if (method instanceof HttpGet)
			addParameters((HttpGet)method, params);
		else
			throw new IllegalArgumentException("Can not handled a method of class:" + method.getClass().getName());
	}
	
	public void addParameters(HttpGet method, Map<String, Iterable<String>> params) throws Exception 
	{
		if(params != null && ! params.isEmpty())
		{
			StringBuffer sb = new StringBuffer();
			if(method.getURI() != null)
				sb.append(method.getURI());
			sb.append(URL_PARAMETERS_BEGIN);

			for(Entry<String, Iterable<String>> param : params.entrySet())
			{
				if(param.getValue() != null)
				{
					for(String paramValue : param.getValue())
					{
						sb.append(param.getKey());
						sb.append("=");
						if(StringUtils.isNotEmpty(paramStreamEncoding))
							sb.append(URLEncoder.encode(paramValue, paramStreamEncoding));
						else
							sb.append(param.getValue());
						sb.append("&");
					}
				}
			}
			String newUrl = sb.toString().substring(0, sb.toString().length() - 1);
			method.setURI(new URI(newUrl));
		}
	}
	
	public void addParameters(HttpPost method, Map<String, Iterable<String>> params) throws Exception 
	{
		
	    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	    if(params != null && ! params.isEmpty())
		{
			for(Entry<String, Iterable<String>> param : params.entrySet())
			{
				if(param.getValue() != null)
				{
					for(String paramValue : param.getValue())
					{
						nameValuePairs.add(new BasicNameValuePair(param.getKey(), paramValue));
						log.debug("Param:" + param.getKey() + ":" +  paramValue);
					}
				}
			}
			UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs, paramStreamEncoding);
			method.setEntity(urlEncodedFormEntity);
		}
	}
	
	public String retrieveContentByPost(HttpClient httpClient, String targetUrl) throws Exception
	{
		return retrieveContent(httpClient, new HttpPost(targetUrl), null, HttpStatus.SC_OK, -1);
	}
	
	public String retrieveContentByPost(HttpClient httpClient, String targetUrl, int expectedStatusCode) throws Exception
	{
		return retrieveContent(httpClient, new HttpPost(targetUrl), null, expectedStatusCode, -1);
	}
	
	public String retrieveContentByPost(HttpClient httpClient, String targetUrl, Map<String, Iterable<String>> params) throws Exception
	{
		return retrieveContent(httpClient, new HttpPost(targetUrl), params, HttpStatus.SC_OK, -1);
	}
	
	public String retrieveContentByPost(HttpClient httpClient, String targetUrl, Map<String, Iterable<String>> params, int expectedStatusCode) throws Exception
	{
		return retrieveContent(httpClient, new HttpPost(targetUrl), params, expectedStatusCode, -1);
	}
	
	public String retrieveContentByPost(HttpClient httpClient, String targetUrl, String requestBody) throws Exception 
	{
		return retrieveContentByPost(httpClient, new HttpPost(targetUrl), requestBody, HttpStatus.SC_OK);
	}
	
	public String retrieveContentByPost(HttpClient httpClient, HttpPost httpPost, String requestBody, int ... expectedStatusCodes) throws Exception 
	{
		String response = null;

		if(httpPost.getParams() != null)
			httpPost.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.TRUE);
		
		if(reccurentHeaders != null)
		{
			for(Header header : reccurentHeaders)
			{
				httpPost.addHeader(header);
			}
		}
		
		try
		{
			if(log.isDebugEnabled())
				log.debug("Going to call " + httpPost.getRequestLine().toString());
			
			httpPost.setEntity(new StringEntity(requestBody, inputStreamEncoding));
			
			HttpResponse httpResponse = httpClient.execute(httpPost, httpClientContext);

			response = retrieveResponseContent(httpResponse, expectedStatusCodes);
			
			handleHttpResponseStatusCheck(httpPost, response, httpResponse, expectedStatusCodes);
		}
		finally
		{
			httpPost.releaseConnection();
		}
		return response;
	}
	
	public String retrieveContentByGet(HttpClient httpClient, String targetUrl) throws Exception
	{
		return retrieveContent(httpClient, new HttpGet(targetUrl), null, HttpStatus.SC_OK, -1);
	}
	
	public String retrieveContentByGet(HttpClient httpClient, String targetUrl, Map<String, Iterable<String>> params) throws Exception
	{
		return retrieveContent(httpClient, new HttpGet(targetUrl), params, HttpStatus.SC_OK, -1);
	}
	
	public String retrieveContentByGet(HttpClient httpClient, String targetUrl, int expectedStatusCode) throws Exception
	{
		return retrieveContent(httpClient, new HttpGet(targetUrl), null, expectedStatusCode, -1);
	}
	
	public String retrieveContentByGet(HttpClient httpClient, String targetUrl, Map<String, Iterable<String>> params, int expectedStatusCode) throws Exception
	{
		return retrieveContent(httpClient, new HttpGet(targetUrl), params, expectedStatusCode, -1);
	}
	
	public String retrieveContent(HttpClient httpClient, HttpRequestBase httpRequest) throws Exception
	{
		return retrieveContent(httpClient, httpRequest, null, HttpStatus.SC_OK, -1);
	}
	
	public String retrieveContent(HttpClient httpClient, HttpRequestBase httpRequest, Map<String, Iterable<String>> params) throws Exception 
	{
		return retrieveContent(httpClient, httpRequest, params, HttpStatus.SC_OK, -1);
	}
	
	public String retrieveContent(HttpClient httpClient, HttpRequestBase httpRequest, Map<String, Iterable<String>> params, int ... expectedStatusCodes) throws Exception 
	{
		return retrieveContent(httpClient, httpRequest, params, HttpStatus.SC_OK, -1);
	}
	
	public String retrieveContent(HttpClient httpClient, HttpRequestBase httpRequest, Map<String, Iterable<String>> params, int expectedStatusCode, int maxFollowRedirects) throws Exception 
	{
		String response = null;
		
		addParameters(httpRequest, params);
		
		if(httpRequest.getParams() != null)
			httpRequest.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.TRUE);
		
		if(reccurentHeaders != null)
		{
			for(Header header : reccurentHeaders)
			{
				httpRequest.addHeader(header);
			}
		}
		
		try
		{
			if(log.isDebugEnabled())
				log.debug("Going to call " + httpRequest.getRequestLine().toString());
			
			HttpResponse httpResponse = httpClient.execute(httpRequest, httpClientContext);
			
			int followedRedirect = 0;
			int responseStatusCode = httpResponse.getStatusLine().getStatusCode();
			while(maxFollowRedirects >= followedRedirect && responseStatusCode == HttpStatus.SC_MOVED_TEMPORARILY)
			{
				HttpGet httpGet = new HttpGet(retrieveLocationHeader(httpResponse));
				httpResponse = httpClient.execute(httpGet, httpClientContext);
				followedRedirect++;
			}
			
			response = retrieveResponseContent(httpResponse, expectedStatusCode);
			
			handleHttpResponseStatusCheck(httpRequest, response, httpResponse, expectedStatusCode);
		}
		finally
		{
			httpRequest.releaseConnection();
		}
		return response;
	}

	public static String retrieveLocationHeader(HttpResponse httpResponse) {
		if(httpResponse != null) {
			Header locationHeader = httpResponse.getFirstHeader(LOCATION_HEADER);		
			if(locationHeader != null && StringUtils.isNotEmpty(locationHeader.getValue()))
				return locationHeader.getValue();
		}
		throw new IllegalStateException("Can not extract LOCATION_HEADER from " + httpResponse);
	}
	
	public static boolean isHttpRedirect(int statusCode) {
		return HTTP_REDIRECT_CODES.contains(statusCode);
	}
	
	protected void handleHttpResponseStatusCheck(HttpRequestBase httpRequest, String response, HttpResponse httpResponse, int ... expectedStatusCodes) throws HttpResponseException {
		
		int responseStatusCode = httpResponse.getStatusLine().getStatusCode();
		
		if(log.isDebugEnabled())
			log.debug(new StringBuffer().append("HTTP - ").append(responseStatusCode).append(" - ").append(httpResponse.getStatusLine().getReasonPhrase()).append(" / response body '").append(response).append("'").toString());
		
		if( ! isResponseValid(responseStatusCode, expectedStatusCodes) && throwingExceptionOnStatusCodes)
		{
			String exceptionMessage = buildErrorMessage(httpRequest.getURI().toString(), httpResponse, expectedStatusCodes);
			log.error(exceptionMessage);
			throw new HttpResponseException(responseStatusCode, exceptionMessage);
		}
		
		if(log.isDebugEnabled() && hasReceivedTheConnectionCloseHeader(httpResponse))
			log.debug("Has received the Connection: close response header => can not reuse the socket connection");
	}

	public static String buildErrorMessage(String url, HttpResponse httpResponse, int ... expectedStatusCodes) {
		
		int responseStatusCode = httpResponse.getStatusLine().getStatusCode();
		
		return new StringBuffer().append("On url:").append(url).append(" - Not expected HTTP statut received '").append(responseStatusCode)
				.append("', this call was supposed to respond with - HTTP ").append(expectedStatusCodes != null ? Arrays.toString(expectedStatusCodes) : null).append("|").append(httpResponse.getStatusLine().getReasonPhrase()).toString();
	}

	public boolean isResponseValid(int responseStatusCode, int ... expectedStatusCodes) {
		for(int i = 0 ; i < expectedStatusCodes.length ; i++)
			if(responseStatusCode == expectedStatusCodes[i])
				return true;
		return false;
	}

	protected String retrieveResponseContent(HttpResponse httpResponse, int ... expectedStatusCodes) throws IOException {
		String response = null;

		if(isResponseValid(httpResponse.getStatusLine().getStatusCode(), expectedStatusCodes) || alwaysRetrievingResponseContent || log.isDebugEnabled())
		{
			InputStream is = null;
			try
			{
				if(httpResponse.getEntity() != null)
				{
					is = httpResponse.getEntity().getContent();
					if(StringUtils.isNotEmpty(inputStreamEncoding))
						response = IOUtils.toString(is, inputStreamEncoding);
					else
						response = IOUtils.toString(is);
				}
			}
			finally
			{
				if(is != null)
					IOUtils.closeQuietly(is);
			}
		}
		return response;
	}

	public String getParamStreamEncoding() {
		return paramStreamEncoding;
	}

	public void setParamStreamEncoding(String paramStreamEncoding) {
		this.paramStreamEncoding = paramStreamEncoding;
	}

	public String getInputStreamEncoding() {
		return inputStreamEncoding;
	}

	public void setInputStreamEncoding(String inputStreamEncoding) {
		this.inputStreamEncoding = inputStreamEncoding;
	}

	public List<Header> getReccurentHeaders() {
		return reccurentHeaders;
	}

	public void setReccurentHeaders(List<Header> reccurentHeaders) {
		this.reccurentHeaders = reccurentHeaders;
	}

	public HttpClientContext getHttpClientContext() {
		return httpClientContext;
	}

	public void setHttpClientContext(HttpClientContext httpClientContext) {
		this.httpClientContext = httpClientContext;
	}

	public void setThrowingExceptionOnStatusCodes(boolean throwingExceptionOnStatusCodes) {
		this.throwingExceptionOnStatusCodes = throwingExceptionOnStatusCodes;
	}
	
	public void setAlwaysRetrievingResponseContent(boolean alwaysRetrievingResponseContent) {
		this.alwaysRetrievingResponseContent = alwaysRetrievingResponseContent;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void addParamToParamMap(Map<String, Iterable<String>> paramMap, String paramName, String paramValue)
	{
		if(paramMap != null && StringUtils.isNotEmpty(paramName) && StringUtils.isNotEmpty(paramValue))
		{
			if(paramMap.get(paramName) == null)
				paramMap.put(paramName, new ArrayList<String>());
			((List)paramMap.get(paramName)).add(paramValue);
		}
	}
	
	public static Map<String, Iterable<String>> convertParameterMap(Map<String, String[]> paramMap)
	{
		Map<String, Iterable<String>> map = new HashMap<String, Iterable<String>>();
		if(paramMap != null)
		{
			for(Entry<String, String[]> entry : paramMap.entrySet())
			{
				if(StringUtils.isNotEmpty(entry.getKey()) && entry.getValue() != null)
				{
					map.put(entry.getKey(), Arrays.asList(entry.getValue()));
				}
			}
		}
		return map;
	}
	
	protected boolean hasReceivedTheConnectionCloseHeader(HttpResponse httpResponse) {
		if(httpResponse.getAllHeaders() != null)
		{
			for(Header header : httpResponse.getAllHeaders()) {
				if(header.getName().equalsIgnoreCase(CONNECTION_HEADER_NAME) && header.getValue().equalsIgnoreCase(CONNECTION_HEADER_NAME_CLOSE_VALUE))
					return true;
			}
		}
		return false;
	}
	
	public static String removeUrlParams(String url) {
		return removeUrlParams(url, false);
	}
	
	public static String removeUrlParams(String url, boolean slashAtTheEnd) {
		if(StringUtils.isNotEmpty(url)) {
			int indexOfParametersBegin = url.indexOf(URL_PARAMETERS_BEGIN);
			if(indexOfParametersBegin > 0) {
				url = url.substring(0, indexOfParametersBegin);
			}
			
			if( ! slashAtTheEnd && url.endsWith(SLASH_STRING))
				url = url.substring(0, url.length() - 1);
			
			if(slashAtTheEnd && ! url.endsWith(SLASH_STRING))
				url = new StringBuffer().append(url).append(SLASH_STRING).toString();
		}
		return url;
	}
	
	public static String cleanSpecialChars(String targetUrl)throws UnsupportedEncodingException {
		return targetUrl.replaceAll(SPACE_STRING, URLEncoder.encode(SPACE_STRING, UTF_8_ENCODING));
	}
	
	public static String extractHostname(String url) {
		if(url.indexOf(HTTP_PROTOCOL) == 0 || url.indexOf(HTTPS_PROTOCOL) == 0) {
			int indexOfEndProtocol = url.indexOf(PROTOCOL_END);
			if(indexOfEndProtocol > 0)
			{
				int hostnameBeginning = indexOfEndProtocol + PROTOCOL_END.length();
				int indexOfEndHostname = url.indexOf(SLASH_STRING, hostnameBeginning);
				if(indexOfEndHostname > 0)
					return url.substring(hostnameBeginning, indexOfEndHostname);
				else
					return url.substring(hostnameBeginning);
			}
		}
		throw new IllegalStateException("Can not extract hostname from: " + url);
	} 
	
	public static int[] buildOkOrRedirectStatusCodesArray() {
		Set<Integer> statusCodes = new HashSet<Integer>();
		statusCodes.addAll(HTTP_REDIRECT_CODES);
		statusCodes.add(HttpStatus.SC_OK);
		
		return ArrayUtils.toPrimitive(statusCodes.toArray(new Integer[statusCodes.size()]));
	}
}
