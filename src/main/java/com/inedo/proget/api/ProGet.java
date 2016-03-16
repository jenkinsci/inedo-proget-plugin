package com.inedo.proget.api;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.types.FileSet;

import com.google.common.net.MediaType;
import com.inedo.http.HttpEasy;
import com.inedo.proget.domain.Feed;
import com.inedo.proget.domain.PackageMetadata;
import com.inedo.proget.domain.ProGetPackage;
import com.inedo.proget.jenkins.ProGetHelper;
import hudson.Util;

/**
 * BuildMaster json api interface 
 * 
 * http://localhost:81/api/json
 * http://inedo.com/support/documentation/proget/reference/universal-feed-api-and-package-format
 * 
 * @author Andrew Sumner
 */
public class ProGet {
	private ProGetConfig config;
	
	public ProGet(ProGetHelper helper) {
		this.config = helper.getProGetConfig();
		
		HttpEasy.withDefaults()
			.allowAllHosts()
			.trustAllCertificates()
			.baseUrl(config.url)
			.withLogWriter(helper);
	}

	/**
	 * Ensure can call BuildMaster api. An exception will be thrown if cannot.
	 * 
	 * @throws IOException
	 */
	public void checkConnection() throws IOException {
		getFeeds();
	}

	/** Gets the details of a feed by its name */
	public Feed getFeed(String feedName) throws IOException {
		Feed feed = HttpEasy.request().
				path("api/json/Feeds_GetFeed?API_Key={}&Feed_Name={}").
				urlParameters(config.apiKey, feedName).
				get().
				asJson(Feed.class);
		
		if (feed == null) {
			throw new IOException("Feed " + feedName + " was not found");
		}
		
		return feed;
	}

	/** Get all active feeds */
	public Feed[] getFeeds() throws IOException {
		Feed[] result = HttpEasy.request().
				path("api/json/Feeds_GetFeeds?API_Key={}&IncludeInactive_Indicator={}").
				urlParameters(config.apiKey, "N").
				get().
				asJson(Feed[].class);
		
		return result;
	}
	
	/** Gets the packages in a ProGet feed */
	public ProGetPackage[] getPackageList(String feedId) throws IOException {
		return HttpEasy.request().
				path("api/json/ProGetPackages_GetPackages?API_Key={}&Feed_Id={}&IncludeVersions_Indicator=Y").
				urlParameters(config.apiKey, feedId, "Y").
				get().
				asJson(ProGetPackage[].class);
	}

	/**
	 * 
	 * @param feedName		Required
	 * @param groupName		Required
	 * @param packageName	Required
	 * @param version		Optional - empty string returns latest version
	 * @param toFolder		Folder to save file to
	 * @return	Reference to downloaded file
	 * @throws IOException
	 */
	public File downloadPackage(String feedName, String groupName, String packageName, String version, String toFolder) throws IOException {
		String path = "upack/{«feed-name»}/download/{«group-name»}/{«package-name»}";
		
		if (version == null || version.trim().isEmpty()) {
			version = "";
		} else {
			path += "/{«package-version»}";
		}
		
		return HttpEasy.request().
				path(path).
				urlParameters(feedName, groupName, packageName, version).
				get().
				downloadFile(toFolder);
	}
	
	public File createPackage(File workFolder, String includes, String excludes, boolean caseSensitive) throws IOException {
		List<File> files = ProGetPackageUtils.getFileList(workFolder, includes, excludes, caseSensitive);
        
        if (files.isEmpty()) {
        	return null;
        }
        
		return new ProGetPackageUtils().createPackage(workFolder, files, getMetadataObject());
	}
	
	private PackageMetadata getMetadataObject() throws IOException {
		PackageMetadata metadata = new PackageMetadata();
		metadata.group = "com/inedo/proget";
		metadata.name = "ExamplePackage";
		metadata.version = "0.0.3";
		metadata.title = "Example Package";
		metadata.description = "Example package for testing";
		return metadata;
	}
	
	public void uploadPackage(String feedName, File progetPackage) throws IOException {
		HttpEasy.request().
				path("upack/{«feed-name»}/upload").
				urlParameters(feedName).
				data(progetPackage, MediaType.ZIP).
				authorization("Admin", "Admin").
				post();
	}	
}