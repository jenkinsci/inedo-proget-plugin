package com.inedo.proget.api;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

import com.google.common.net.MediaType;
import com.inedo.http.HttpEasy;
import com.inedo.http.LogWriter;
import com.inedo.proget.domain.Feed;
import com.inedo.proget.domain.ProGetPackage;
import com.inedo.proget.domain.Version;
import com.inedo.proget.jenkins.GlobalConfig;
import com.inedo.proget.jenkins.JenkinsLogWriter;
import com.inedo.proget.jenkins.DownloadFormat;

/**
 * BuildMaster json api interface 
 * 
 * http://localhost:81/api/json
 * http://inedo.com/support/documentation/proget/reference/universal-feed-api-and-package-format
 * 
 * @author Andrew Sumner
 */
public class ProGetApi implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private ProGetConfig config;
	
	public ProGetApi() {
		this(GlobalConfig.getProGetConfig(), new JenkinsLogWriter(null));
	}
	
	public ProGetApi(ProGetConfig config) {
		this(config, new JenkinsLogWriter(null));
	}
	
	public ProGetApi(LogWriter logWriter) {
		this(GlobalConfig.getProGetConfig(), logWriter);
	}
	
	public ProGetApi(ProGetConfig config, LogWriter logWriter) {
		this.config = config;
		
		HttpEasy.withDefaults()
			.allowAllHosts()
			.trustAllCertificates()
			.baseUrl(config.url)
			.withLogWriter(logWriter);
	}

	/**
	 * Check that can connect to ProGet, and check the apiKey if configured.
	 * 
	 * @throws IOException If cannot connect
	 */
	public void canConnect() throws IOException {
		if (config.apiKey == null || config.apiKey.isEmpty()) {
			String version = getVersion();
			
			if (!version.startsWith("ProGet")) {
				throw new IOException("URL does not point at ProGet");
			}
		} else {
			getFeeds();
		}
	}

	/**
	 * Ensure can ping an endpointGet.
	 * 
	 * @throws IOException If cannot connect
	 */
	public void ping() throws IOException {
		try (Socket socket = new Socket()) {
			URL url = new URL(config.url);
	        socket.connect(new InetSocketAddress(url.getHost(), url.getPort()));
		}
	}
	
	public String getVersion() throws IOException {
		return HttpEasy.request()
				.path("api/version")
				.get()
				.asString();
	}

	/** Get all active feeds */
	public Feed[] getFeeds() throws IOException {
		Feed[] result = HttpEasy.request()
				.path("api/json/Feeds_GetFeeds?API_Key={}&IncludeInactive_Indicator={}")
				.urlParameters(config.apiKey, "N")
				.get()
				.getJsonReader()
				.fromJson(Feed[].class);
		
		return result;
	}	

	/** Gets the details of a feed by its name */
	public Feed getFeed(String feedName) throws IOException {
		Feed feed = HttpEasy.request()
				.path("api/json/Feeds_GetFeed?API_Key={}&Feed_Name={}")
				.urlParameters(config.apiKey, feedName)
				.get()
				.getJsonReader()
				.fromJson(Feed.class);
		
		if (feed == null) {
			throw new IOException("Feed " + feedName + " was not found");
		}
		
		return feed;
	}
		
	/** Gets the packages in a ProGet feed */
	public ProGetPackage[] getPackages(String feedId) throws IOException {
		return HttpEasy.request()
				.path("api/json/ProGetPackages_GetPackages?API_Key={}&Feed_Id={}&IncludeVersions_Indicator=Y")
				.urlParameters(config.apiKey, feedId, "Y")
				.get()
				.getJsonReader()
				.fromJson(ProGetPackage[].class);
	}

	/** Gets the package versions in a ProGet feed */
	public Version[] getPackageVersions(String feedId, String groupName, String packageName) throws IOException {
		return HttpEasy.request()
				.path("api/json/ProGetPackages_GetPackageVersions?API_Key={}&Feed_Id={}&Group_Name={}&Package_Name={}")
				.urlParameters(config.apiKey, feedId, groupName, packageName)
				.get()
				.getJsonReader()
				.fromJson(Version[].class);
	}
	
	
	/**
	 * 
	 * @param feedName		Required
	 * @param groupName		Required
	 * @param packageName	Required
	 * @param version		Optional - empty string returns latest version
	 * @param toFolder		Folder to save file to
	 * @param downloadFormat 
	 * @return	Reference to downloaded file
	 * @throws IOException
	 */
	public File downloadPackage(String feedName, String groupName, String packageName, String version, String toFolder, DownloadFormat downloadFormat) throws IOException {
	    boolean latest = (version == null || version.trim().isEmpty() || version.equalsIgnoreCase("latest"));
		String path = "upack/{feed-name}/download/{group-name}/{package-name}";
		
		if (!latest){
			path += "/{package-version}";
		}
		
		HttpEasy request = HttpEasy.request()
                .path(path)
                .urlParameters(feedName, groupName, packageName, version);
		
		if (latest) {
		    request.query("latest");
		}
		
		if (downloadFormat == DownloadFormat.CONTENT_AS_ZIP || downloadFormat == DownloadFormat.CONTENT_AS_TGZ) {
		    request.queryParam("contentOnly", downloadFormat.getFormat());
		}
		
		return request.get().downloadFile(toFolder);
	}
	
	public void uploadPackage(String feedName, File progetPackage) throws IOException {
		HttpEasy.request()
				.path("upack/{feed-name}/upload")
				.urlParameters(feedName)
				.data(progetPackage, MediaType.ZIP)
				.authorization(config.user, config.password)
				.post();
	}
}