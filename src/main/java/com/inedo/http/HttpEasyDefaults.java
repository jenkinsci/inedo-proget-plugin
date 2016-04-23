package com.inedo.http;

import java.net.Proxy;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Allows setting of default properties used by all subsequent HttpEasy requests. 
 * 
 * @author Andrew Sumner
 */
public class HttpEasyDefaults {
	
	/**
	 * Create all-trusting certificate verifier.
	 * @return A self reference
	 */
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

	/**
	 * Create all-trusting host name verifier.
	 * 
	 * @return A self reference
	 */
	public HttpEasyDefaults allowAllHosts() {
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
	 * @param proxy	Sets the default {@link Proxy} to use for all connections 
	 * @return A self reference
	 */
	public HttpEasyDefaults proxy(Proxy proxy) {
		HttpEasy.proxy = proxy;
		return this;
	}
	
	/**
	 * Set the default username and password for proxy authentication.
	 * 
	 * @param userName Proxy username
	 * @param password Proxy password
	 * @return A self reference
	 */
	public HttpEasyDefaults proxyAuth(String userName, String password) {
		HttpEasy.proxyUser = userName;
		HttpEasy.proxyPassword = password;
		return this;
	}

	/**
	 * Use proxy, or not, for local addresses.
	 * 
	 * @param bypassLocalAddresses Value
	 * @return A self reference
	 */
	public HttpEasyDefaults bypassProxyForLocalAddresses(boolean bypassLocalAddresses) {
		HttpEasy.bypassProxyForLocalAddresses = bypassLocalAddresses;
		return this;
	}

	/**
	 * Set the default base url for all HttpEasy requests.
	 * 
	 * @param baseUrl Base URL
	 * @return A self reference
	 */
	public HttpEasyDefaults baseUrl(String baseUrl) {
		HttpEasy.defaultbaseURI = baseUrl;
		return this;
	}
	
	/**
	 * Set the logger to write to
	 * @param logWriter
	 * @return A self reference
	 */
	public HttpEasyDefaults withLogWriter(LogWriter logWriter) {
		HttpEasy.defaultLogWriter = logWriter;
		return this;
	}
}