package com.polyspot.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;

public class SocksHttpClientUtil extends CloseableHttpClientUtil {

	// com.polyspot.utils.SocksHttpClientUtil
	
	private static final String HTTP = "http";
	private static final String HTTPS = "https";
	private static final String SOCKS_ADDRESS_ATTRIBUTE = "socks.address";

	public SocksHttpClientUtil(String proxyHost, int proxyPort) {
		this.httpClientContext = buildSocksContext(proxyHost, proxyPort);
	}
	
	public static CloseableHttpClient buildHttpClient() {
		Registry<ConnectionSocketFactory> httpRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
		        .register(HTTP, new Socks5ConnectionSocketFactory())
		        .register(HTTPS, new SSLSocks5ConnectionSocketFactory(SSLContexts.createSystemDefault()))
		        .build();
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(httpRegistry);
		CloseableHttpClient httpClient = HttpClients.custom()
		        .setConnectionManager(cm)
		        .build();
		return httpClient;
	}
	
	public static HttpClientContext buildSocksContext(String proxyHost, int proxyPort) {
		InetSocketAddress socksAddress = new InetSocketAddress(proxyHost, proxyPort);
		HttpClientContext context = HttpClientContext.create();
		context.setAttribute(SOCKS_ADDRESS_ATTRIBUTE, socksAddress);
		return context;
	}
	
	public static Socket buildSocket(final HttpContext context) {
		if(context.getAttribute(SOCKS_ADDRESS_ATTRIBUTE) == null)
			throw new IllegalStateException("Can not find attribute '" + SOCKS_ADDRESS_ATTRIBUTE + "' in context");
		
		InetSocketAddress socksAddress = (InetSocketAddress) context.getAttribute(SOCKS_ADDRESS_ATTRIBUTE);
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksAddress);
        return new Socket(proxy);
	}
	
	static class Socks5ConnectionSocketFactory implements ConnectionSocketFactory {

	    @Override
	    public Socket createSocket(final HttpContext context) throws IOException {
	        return buildSocket(context);
	    }

        @Override
        public Socket connectSocket(
                final int connectTimeout,
                final Socket socket,
                final HttpHost host,
                final InetSocketAddress remoteAddress,
                final InetSocketAddress localAddress,
                final HttpContext context) throws IOException, ConnectTimeoutException {
            Socket sock;
            if (socket != null) {
                sock = socket;
            } else {
                sock = createSocket(context);
            }
            if (localAddress != null) {
                sock.bind(localAddress);
            }
            try {
                sock.connect(remoteAddress, connectTimeout);
            } catch (SocketTimeoutException ex) {
                throw new ConnectTimeoutException(ex, host, remoteAddress.getAddress());
            }
            return sock;
        }
	}
	
	static class SSLSocks5ConnectionSocketFactory extends SSLConnectionSocketFactory  {
	    public SSLSocks5ConnectionSocketFactory(final SSLContext sslContext) {
	        super(sslContext);
	    }

	    @Override
	    public Socket createSocket(final HttpContext context) throws IOException {
	    	return buildSocket(context);
	    }
	}
}
