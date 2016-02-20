package com.inedo.proget.api;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.Proxy;
//import java.util.Arrays;
//import org.apache.http.auth.AuthScope;
//import org.apache.http.auth.UsernamePasswordCredentials;
//import org.apache.http.auth.NTCredentials;
//import org.apache.http.client.CredentialsProvider;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.config.AuthSchemes;
//import org.apache.http.client.config.RequestConfig;
//import org.apache.http.impl.client.BasicCredentialsProvider;
//import org.apache.http.impl.client.HttpClientBuilder;
//import org.apache.http.impl.client.HttpClients;
//import com.inedo.proget.ConnectionType;
import com.inedo.proget.domain.Feed;
import com.inedo.proget.domain.PackageMetadata;
import com.inedo.proget.domain.ProGetPackage;
import com.inedo.rest.RestRequest;

/**
 * BuildMaster json api interface
 * 
 * @author Andrew Sumner
 */
public class ProGet {
	private ProGetConfig config;
//	private boolean logRequest = true;
	
	public ProGet(ProGetConfig config) {
		this.config = config;

		// TODO - Assume no need for proxy as unlikely to want to get out of a network...
//		if (config.isProxyRequired()) {
//			RestRequest.withDefaults().allowAllHosts().trustAllCertificates();
//
//			RestRequest.withDefaults()
//					.proxy(new Proxy(Proxy.Type.HTTP,
//							new InetSocketAddress(ConfigUtils.getProxyHost(), ConfigUtils.getProxyPort())))
//					.proxyAuth(ConfigUtils.getProxyUser(), ConfigUtils.getProxyPassword())
//					.bypassProxyForLocalAddresses(true);
//		}

		// TODO - Will probably need to add authentication back in
//		HttpClientBuilder httpbuilder = HttpClients.custom();
//		RequestConfig.Builder configbuilder = RequestConfig.custom();
//
//		if (ConnectionType.BASIC.getId().equalsIgnoreCase(config.authentication)) {
//			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//			credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(config.user,config.password));
//
//			httpbuilder.setDefaultCredentialsProvider(credentialsProvider);
//			configbuilder.setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC));
//		}
//
//		if (ConnectionType.NTLM.getId().equalsIgnoreCase(config.authentication)) {
//			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//			credentialsProvider.setCredentials(
//					AuthScope.ANY,
//					new NTCredentials(config.user, config.password, config.getHost(), config.domain));
//
//			httpbuilder.setDefaultCredentialsProvider(credentialsProvider);
//			configbuilder.setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM));
//		}

		// Finally we instantiate the client. Client is a thread safe object and
		// can be used by several threads at the same time.
		// Client can be used for several request. The life span of the client
		// must be equal to the life span of this EJB.
//		httpclient = httpbuilder.setDefaultRequestConfig(configbuilder.build()).build();
	}

	/**
	 * Ensure can call BuildMaster api. An exception will be thrown if cannot.
	 * 
	 * @throws IOException
	 */
	public void checkConnection() throws IOException {
		getFeeds();
	}

	public void upload(String feedName) throws IOException {
		
	}
	
	/** Gets the details of a feed by its name */
	public Feed getFeed(String feedName) throws IOException {
		Feed feed = RestRequest.request().
				baseURI(config.url).
				path("api/json/Feeds_GetFeed?API_Key={}&Feed_Name={}").
				pathParameters(config.apiKey, feedName).
				get().asJson(Feed.class);
		
		if (feed == null) {
			throw new IOException("Feed " + feedName + " was not found");
		}
		
		return feed;
	}

	/** Get all active feeds */
	public Feed[] getFeeds() throws IOException {
		Feed[] result = RestRequest.request().
				baseURI(config.url).
				path("api/json/Feeds_GetFeeds?API_Key={}&IncludeInactive_Indicator={}").
				pathParameters(config.apiKey, "N").
				get().asJson(Feed[].class);
		
		return result;
	}
	
	/** Gets the packages in a ProGet feed */
	public ProGetPackage[] getPackages(String feedId) throws IOException {
		return RestRequest.request().
				baseURI(config.url).
				path("api/json/ProGetPackages_GetPackages?API_Key={}&Feed_Id={}&IncludeVersions_Indicator=Y").
				pathParameters(config.apiKey, feedId, "Y").
				get().asJson(ProGetPackage[].class);
	}

	public File downloadPackage(String feedName, ProGetPackage pkg, String toFolder) throws IOException {
		return RestRequest.request().
				baseURI(config.url).
				path("upack/{«feed-name»}/download/{«group-name»}/{«package-name»}/{«package-version»}").
				pathParameters(feedName, pkg.Group_Name, pkg.Package_Name, pkg.LatestVersion_Text).
				get().
				downloadFile(toFolder);
	}

	public File createPackage(File sourceFolder, PackageMetadata metadata) throws IOException {
		return new ProGetPackageUtils().createPackage(sourceFolder, metadata);
	}
	
	public void updloadPackage() {
		//http://java-monitor.com/forum/showthread.php?t=4090
		//http://www.codejava.net/java-se/networking/upload-files-by-sending-multipart-request-programmatically
	}	
}