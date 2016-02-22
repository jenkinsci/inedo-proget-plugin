package com.inedo.rest;

import java.net.Proxy;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

// Class to segregate the setting of 'global' properties 
public class EasyHttpDefaults {
	public EasyHttpDefaults trustAllCertificates() {
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
			EasyHttp.LOGGER.error(e.getMessage());
		}
		
		return this;
	}

	public EasyHttpDefaults allowAllHosts() {
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
	public EasyHttpDefaults proxy(Proxy proxy) {
		EasyHttp.proxy = proxy ;
		return this;
	}
	
	public EasyHttpDefaults proxyAuth(String userName, String password) {
		EasyHttp.proxyUser = userName ;
		EasyHttp.proxyPassword = password;
		return this;
	}

	public void bypassProxyForLocalAddresses(boolean bypassLocalAddresses) {
		EasyHttp.bypassProxyForLocalAddresses = bypassLocalAddresses;
	}	
}