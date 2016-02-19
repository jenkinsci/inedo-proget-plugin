package com.inedo.proget.api;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Configuration settings required to can the BuildMaster json api
 * 
 * @author Andrew Sumner
 */
public class ProGetConfig {
	// Global Configuration Items
	public String url;
	public String authentication;
	public String domain;
	public String user;
	public String password;
	public String apiKey;
	public PrintStream printStream = System.out;
    
	/**
	 * Get the name of the host the service is running on.
	 */
	public String getHost() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			System.err.println(e.getMessage());
			return "Unknown";
		}
	}

}
