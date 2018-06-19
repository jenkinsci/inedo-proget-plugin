package com.inedo.proget.api;

import java.io.Serializable;

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
    public boolean logApiRequests;
    public boolean trustAllCertificates;

}
