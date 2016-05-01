package com.inedo.proget.api;

import java.io.File;
import java.io.IOException;
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
import com.inedo.proget.jenkins.DownloadPackageBuilder.DownloadFormat;

/**
 * BuildMaster json api interface 
 * 
 * http://localhost:81/api/json
 * http://inedo.com/support/documentation/proget/reference/universal-feed-api-and-package-format
 * 
 * @author Andrew Sumner
 */
public class ProGetApi {
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
	
	private ProGetApi(ProGetConfig config, LogWriter logWriter) {
		this.config = config;
		
		HttpEasy.withDefaults()
			.allowAllHosts()
			.trustAllCertificates()
			.baseUrl(config.url)
			.withLogWriter(logWriter);
	}

	/**
	 * Ensure can ping ProGet.  If we need to get assurance that it is running we could call a URL using HEAD method.
	 * 
	 * @throws IOException If cannot connect
	 */
	public void canConnect() throws IOException {
		try (Socket socket = new Socket()) {
			URL url = new URL(config.url);
	        socket.connect(new InetSocketAddress(url.getHost(), url.getPort()));
	    }
	}

	/** Gets the details of a feed by its name */
	public Feed getFeed(String feedName) throws IOException {
		Feed feed = HttpEasy.request()
				.path("api/json/Feeds_GetFeed?API_Key={}&Feed_Name={}")
				.urlParameters(config.apiKey, feedName)
				.get()
				.getJsonReader()
				.asJson(Feed.class);
		
		if (feed == null) {
			throw new IOException("Feed " + feedName + " was not found");
		}
		
		return feed;
	}

	/** Get all active feeds */
	public Feed[] getFeeds() throws IOException {
		Feed[] result = HttpEasy.request()
				.path("api/json/Feeds_GetFeeds?API_Key={}&IncludeInactive_Indicator={}")
				.urlParameters(config.apiKey, "N")
				.get()
				.getJsonReader()
				.asJson(Feed[].class);
		
		return result;
	}
		
	/** Gets the packages in a ProGet feed */
	public ProGetPackage[] getPackages(String feedId) throws IOException {
		return HttpEasy.request()
				.path("api/json/ProGetPackages_GetPackages?API_Key={}&Feed_Id={}&IncludeVersions_Indicator=Y")
				.urlParameters(config.apiKey, feedId, "Y")
				.get()
				.getJsonReader()
				.asJson(ProGetPackage[].class);
	}

	/** Gets the package versions in a ProGet feed */
	public Version[] getPackageVersions(String feedId, String groupName, String packageName) throws IOException {
		return HttpEasy.request()
				.path("api/json/ProGetPackages_GetPackageVersions?API_Key={}&Feed_Id={}&Group_Name={}&Package_Name={}")
				.urlParameters(config.apiKey, feedId, groupName, packageName)
				.get()
				.getJsonReader()
				.asJson(Version[].class);
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
		String path = "upack/{«feed-name»}/download/{«group-name»}/{«package-name»}";
		String query = "";
		
		if (version == null || version.trim().isEmpty()) {
			version = "";
		} else {
			path += "/{«package-version»}";
		}
		
		if (downloadFormat == DownloadFormat.CONTENT_AS_ZIP || downloadFormat == DownloadFormat.CONTENT_AS_TGZ) {
			query = "contentOnly={«zip|tgz»}";
		}
		
		return HttpEasy.request()
				.path(path)
				.query(query)
				.urlParameters(feedName, groupName, packageName, version, downloadFormat.getFormat())
				.get()
				.downloadFile(toFolder);
	}
	
	public void uploadPackage(String feedName, File progetPackage) throws IOException {
		HttpEasy.request()
				.path("upack/{«feed-name»}/upload")
				.urlParameters(feedName)
				.data(progetPackage, MediaType.ZIP)
				.authorization(config.user, config.password)
				.post();
	}	
}