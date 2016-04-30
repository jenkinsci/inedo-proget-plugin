package com.inedo.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.inedo.proget.api.ProGetConfig;
import com.inedo.proget.domain.Feed;

/**
 * A Mocked server that replaces a live ProGet installation
 * 
 * @author Andrew Sumner
 */
public class MockServer {
	private HttpServer server = null;
	private HttpRequestHandler handler;
	private ProGetConfig config;

	public MockServer() throws IOException {
		handler = new HttpHandler();
		server = ServerBootstrap.bootstrap()
					.setLocalAddress(InetAddress.getLocalHost())
					.setListenerPort(0)	// Any free port
					.registerHandler("*", handler)
					.create();

		server.start();

		config = new ProGetConfig();
		config.url = "http://" + server.getInetAddress().getHostName() + ":" + server.getLocalPort();
	}
	
	public ProGetConfig getProGetConfig() {
		return config;
	}
	
	public void stop() {
		if (server!= null) {
			server.stop();
		}
	}
	
	// Handler for the test server that returns responses based on the requests.
	public class HttpHandler implements HttpRequestHandler {

		@Override
		public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
			URI uri = URI.create(request.getRequestLine().getUri());
			
			String method = uri.getPath().replace("/api/json/", "");
			
			switch (method) {
			case "Feeds_GetFeeds":
				response.setEntity(new StringEntity(Feed.EXAMPLE));
				break;
				
			default:
				response.setStatusCode(HttpStatus.SC_NOT_FOUND);
				response.setEntity(new StringEntity("API method " + method + " not found."));
			}
		}
		
	}
}
