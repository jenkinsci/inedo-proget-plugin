package com.inedo.http;

import java.net.Proxy;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

// Class to segregate the setting of 'global' properties 
public class HttpEasyDefaults {
	public HttpEasyDefaults trustAllCertificates() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}
					public void checkClientTrusted(X509Certificate[] certs, String authType) {
					}
					public void checkServerTrusted(X509Certificate[] certs, String authType) {
					}
				}
		};

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			HttpEasy.LOGGER.error(e.getMessage());
		}
		
		return this;
	}

	public HttpEasyDefaults allowAllHosts() {
		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		
		return this;
	}
	
	/**
	 * Set an entry representing a PROXY connection. 
	 *
	 * @param type		the Type of the proxy
	 * @param socket	the SocketAddress for that proxy 
	 * 
	 * @see java.net.Proxy.Proxy
	 */
	public HttpEasyDefaults proxy(Proxy proxy) {
		HttpEasy.proxy = proxy ;
		return this;
	}
	
	public HttpEasyDefaults proxyAuth(String userName, String password) {
		HttpEasy.proxyUser = userName ;
		HttpEasy.proxyPassword = password;
		return this;
	}

	public HttpEasyDefaults bypassProxyForLocalAddresses(boolean bypassLocalAddresses) {
		HttpEasy.bypassProxyForLocalAddresses = bypassLocalAddresses;
		return this;
	}

	public HttpEasyDefaults baseUrl(String baseUrl) {
		HttpEasy.defaultbaseURI = baseUrl;
		return this;
	}
	
	public HttpEasyDefaults withLogWriter(LogWriter logWriter) {
		HttpEasy.defaultLogWriter = logWriter;
		return this;
	}
}