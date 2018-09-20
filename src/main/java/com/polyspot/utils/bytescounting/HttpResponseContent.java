package com.polyspot.utils.bytescounting;

import org.apache.http.client.methods.CloseableHttpResponse;

public class HttpResponseContent {

	private final String content;
	private final int responseBytesCount;
	private final CloseableHttpResponse httpResponse;
	
	public HttpResponseContent(String content, int responseBytesCount, CloseableHttpResponse httpResponse) {
		super();
		this.content = content;
		this.responseBytesCount = responseBytesCount;
		this.httpResponse = httpResponse;
	}

	public String getContent() {
		return content;
	}

	public int getResponseBytesCount() {
		return responseBytesCount;
	}
	
	public CloseableHttpResponse getHttpResponse() {
		return httpResponse;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("Text length: " + ((content != null) ? content.length() : "no content"));
		sb.append(" - response bytes count: " + responseBytesCount);
		return sb.toString();
	}
}
