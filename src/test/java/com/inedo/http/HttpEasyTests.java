package com.inedo.http;

import static org.junit.Assert.assertThat;

import static org.hamcrest.Matchers.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.inedo.http.HttpEasy;

public class HttpEasyTests {
	@BeforeClass 
	public static void setup() {
		HttpEasy.withDefaults()
			.allowAllHosts()
			.trustAllCertificates();
		
//		if (ConfigUtils.isProxyRequired()) {
//			RestRequest.withDefaults()
//				.allowAllHosts()
//				.trustAllCertificates()
//				.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ConfigUtils.getProxyHost(), ConfigUtils.getProxyPort())))
//				.proxyAuth(ConfigUtils.getProxyUser(), ConfigUtils.getProxyPassword())
//				.bypassProxyForLocalAddresses(true);
//    	}
	}
	
	@Test
	public void httpGet() throws Exception {
		String url = "http://jsonplaceholder.typicode.com/posts";
		
		String json = HttpEasy.request()
				.path(url)
				.get()
				.asString();
		
		assertThat(json, containsString("sunt aut facere repellat provident occaecati excepturi optio reprehenderit"));
	}
	
	@Test
	public void httpsGet() throws Exception {
		String url = "https://jsonplaceholder.typicode.com/posts";
		
		String json = HttpEasy.request()
				.path(url)
				.get()
				.asString();
		
		assertThat(json, containsString("sunt aut facere repellat provident occaecati excepturi optio reprehenderit"));
	}
	
}
