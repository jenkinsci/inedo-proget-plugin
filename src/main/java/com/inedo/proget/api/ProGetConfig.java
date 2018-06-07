package com.inedo.proget.api;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Configuration settings required to can the BuildMaster json api
 * 
 * @author Andrew Sumner
 */
public class ProGetConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Global Configuration Items
    public String url;
    public String apiKey;
    public String user;
    public String password;
    public boolean trustAllCertificates;
    
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
