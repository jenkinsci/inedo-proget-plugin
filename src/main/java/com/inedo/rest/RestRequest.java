package com.inedo.rest;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;
import com.inedo.rest.RestResultReader.Family;

/**
 * Rest request utility that supports NTLM proxy authentication.  
 * 
 * This is been designed with as a fluent REST API similar to RestEasy and RestAssurred with the only real difference 
 * being that it has great proxy support.  
 * 
 * Behind the scenes java.net.HttpURLConnection is used as this is the only thing that I found that could get through an NTLM
 * proxy easily.
 * 
 * There are two starting points for creating a rest request:
 * 		RestRequest.withDefaults()	- allows you to set some settings that apply to all requests such as configuring a proxy
 * 		RestRequest.request()		- perform the actual call, these HTTP methods are implemented: GET, HEAD, POST, PUT, DELETE
 * 
 * Note: if your url can contain weird characters you will want to encode it, something like this:
 *		 	myUrl = URLEncoder.encode(myUrl, "UTF-8");
 *
 * Have a look at these as an alternative to HttpURLConnection:
 * 		http://restlet.com/projects/restlet-framework/
 * 		http://hc.apache.org/httpcomponents-client-ga/
 * 		https://github.com/hgoebl/DavidWebb#background
 *
 * Example
 * =======
 * 		RestResultReader r = RestRequest.request()
 * 									.baseURI(databaseURI)
 * 									.path(viewPath + "?startkey=\"{startkey}\"&endkey=\"{endkey}\")
 * 									.pathParameters(startKey[0], endKey[0])
 * 									.get();
 * 
 * 		String id = r.jsonPath("rows[0].doc._id").getAsString();
 * 		String rev = r.jsonPath("rows[0].doc._rev").getAsString();
 *
 * Error Handling
 * ==============
 * An IOException is thrown whenever a call returns a response code that is not part of the SUCCESS family (ie 200-299).
 *  
 * In order to prevent an exception being thrown for an expected response use one of the following methods:
 * 		• request().doNotFailOn(Integer... reponseCodes)
 * 		• request().doNotFailOn(Family... responseFamily)  
 *  
 * Authentication
 * ============== 			
 * supports two formats
 * 		• http://username:password@where.ever
 * 		• request().authorization(String username, String password)
 * 					
 * Host and Certificate Verification
 * =================================
 * There is no fine grained control, its more of an all or nothing approach:
 * 
 * 		RestRequest.withDefaults()
 * 				.allowAllHosts()
 * 				.trustAllCertificates()
 * 			
 * Proxy
 * =====
 * Only basic authentication is supported, although I believe the domain can be added by included "domain/" infront of the username (not tested)
 * 
 * 		RestRequest.withDefaults()
 * 				.proxy(Proxy proxy)
 *				.proxyAuth(String userName, String password)
 *				.bypassProxyForLocalAddresses(boolean bypassLocalAddresses)
 *
 * 
 * Redirects
 * =========
 * Redirects are NOT automatically followed - at least for REST base calls - even though the documentation for HttpURLConnection says that it should...
 * 
 * 		RestResultReader response = RestRequest.request()
 *         			.doNotFailOn(Family.REDIRECTION)
 * 					.path(url)
 * 					.head();
 * 
 *    	if (response.getResponseCodeFamily() == Family.REDIRECTION) {
 * 			...
 * 		}
 * 
 * Multiple Attachments
 * =====================
 * Not currently supported - will take a single file / string
 * 
 */
public class RestRequest
{
	static final Logger LOGGER = LoggerFactory.getLogger(RestRequest.class);

	// Static values are set by RestRequestDefaults and apply to all requests
	static Proxy proxy = Proxy.NO_PROXY;
	static String proxyUser = null;
	static String proxyPassword = null;
	static boolean bypassProxyForLocalAddresses = true;
	
	// These only apply per request - but are visible to package
	Integer[] ignoreResponseCodes = new Integer[0];
	Family[] ignoreResponseFamily = new Family[0];
	
	// These only apply per request
	private String authString = null;
    private String baseURI = "";
    private String path = "";
    private Object[] pathParams = new Object[0];
    MediaType mediaType = null;    
    private String uploadData = "";
    private File uploadFile = null;
    private Map<String, Object> formFields = new LinkedHashMap<String, Object>();	
    private Map<String, Object> headers = new LinkedHashMap<String, Object>();

    public static RestRequestDefaults withDefaults() {
		return new RestRequestDefaults();
	}
    
	public static RestRequest request() {
		return new RestRequest();
	}

	public RestRequest header(String name, String value) {
		headers.put(name, value);
		return this;
	}
	
	public RestRequest authorization(String name, String value) {
		authString = name + ":" + value;
		return this;
	}
	
	public RestRequest baseURI(String uri) {
		this.baseURI = uri;
		return this;
	}

	public RestRequest path(String path) {
		this.path = path;
		return this;
	}

	public RestRequest pathParameters(Object... pathParams) {
		this.pathParams = pathParams;
		return this;
	}
	
	public RestRequest doNotFailOn(Integer... reponseCodes) {
		this.ignoreResponseCodes = reponseCodes;
		return this;
	}

	public RestRequest doNotFailOn(Family... responseFamily) {
		this.ignoreResponseFamily = responseFamily;
		return this;
	}
	
	public RestRequest attachment(MediaType mediaType, Map<String, Object> formFields) throws IOException {
		this.mediaType = mediaType;
		this.formFields = formFields;
		return this;
	}

	public RestRequest attachment(MediaType mediaType, String data) {
		this.mediaType = mediaType;
		this.uploadData = data;
		return this;
	}

	public RestRequest attachment(MediaType mediaType, File file) {
		this.mediaType = mediaType;
		this.uploadFile = file;
		return this;
	}
	
	public RestResultReader get() throws IOException
	{	
		return new RestResultReader(getConnectionMethod("GET"), this);
	}
	
	public RestResultReader head() throws IOException {
		return new RestResultReader(getConnectionMethod("HEAD"), this);
	}

	public RestResultReader post() throws IOException {
		return new RestResultReader(getConnectionMethod("POST"), this);
	}
	
	public RestResultReader put() throws IOException {
		return new RestResultReader(getConnectionMethod("PUT"), this);
	}

	public RestResultReader delete() throws IOException {
		return new RestResultReader(getConnectionMethod("DELETE"), this);
	}

	private HttpURLConnection getConnectionMethod(String requestMethod) throws IOException 
	{	
		URL url = getURL();
		HttpURLConnection connection = getConnection(url);
                
		setHeaders(connection);

		connection.setRequestMethod(requestMethod);
		connection.setReadTimeout(15*1000);	// give it 15 seconds to respond
		connection.setInstanceFollowRedirects(false);
		
		boolean isPost = requestMethod.equals("POST") || requestMethod.equals("PUT");;
		byte[] postEndcoded = null;
		
		if (isPost) {
			if (uploadFile != null) {
				connection.setRequestProperty("Content-Type", mediaType.toString());
				connection.setRequestProperty("Content-Length", Long.toString(uploadFile.length()));
//				connection.setFixedLengthStreamingMode(uploadFile.length());
				connection.setRequestProperty("Content-Disposition", "attachment; filename=\"" + uploadFile.getName() + "\"");
			} else {
				StringBuilder postData = new StringBuilder();
				
				if (url.getQuery() != null) {
					postData.append(url.getQuery());
				}
					
				for (Map.Entry<String, Object> param : formFields.entrySet()) {
		            if (postData.length() > 0) postData.append('&');
		            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
		            postData.append('=');
		            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
		        }
	
				if (uploadData != null && !uploadData.isEmpty()) {
					// Assume data is encoded correctly
					postData.append(uploadData);
					//postData.append(URLEncoder.encode(String.valueOf(data), "UTF-8"));
		        }
				
				postEndcoded = postData.toString().getBytes(StandardCharsets.UTF_8);
				
				connection.setRequestProperty("charset", "utf-8");
				connection.setRequestProperty("Content-Type", mediaType.toString());
				connection.setRequestProperty("Content-Length", Integer.toString(postEndcoded.length));
			}
			
			connection.setDoOutput(true);
		}
		
		LOGGER.trace("Sending " + requestMethod + " to " + url.toString());
		connection.connect();

		if (isPost) {
			if (uploadFile != null) {
				OutputStream outputStream = connection.getOutputStream();
				
				try (FileInputStream inputStream = new FileInputStream(uploadFile)) {
					byte[] buffer = new byte[4096];
			        int bytesRead = -1;
			        while ((bytesRead = inputStream.read(buffer)) != -1) {
			            outputStream.write(buffer, 0, bytesRead);
			        }
			        outputStream.flush();
				}
			} else {
				try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
					wr.write(postEndcoded);
				}
			}
		}
		
		return connection;
	}

	private HttpURLConnection getConnection(URL url) throws IOException {
		HttpURLConnection connection;
		Proxy useProxy = proxy;
		
		if (bypassProxyForLocalAddresses && isLocalAddress(url)) {
			useProxy = Proxy.NO_PROXY;
		}
		
		if (url.getProtocol().equals("https")) {
			connection = (HttpsURLConnection) url.openConnection(useProxy);
		} else {
			connection = (HttpURLConnection) url.openConnection(useProxy);
		}
		
		return connection;
	}

	private boolean isLocalAddress(URL url) {
		return "localhost, 127.0.0.1".contains(url.getHost());
	}

	private URL getURL() throws MalformedURLException {
		//check to / or ? on path and base not end in /
		String fullUrl = appendSegmentToUrl(baseURI, replaceParameters(path));

		URL url = new URL(fullUrl);
		
		if (url.getUserInfo() != null) {			
			authString = url.getUserInfo();
			fullUrl = url.toExternalForm().replace(url.getUserInfo() + "@", "");
			url = new URL(fullUrl);
		}
		
		return url;
	}
	
	private String appendSegmentToUrl(String path, String segment) {
		  if (path == null || path.isEmpty()) {
			  return segment;
		  }
		  
		  if (segment == null || segment.isEmpty()) {
			  return path;
		  }

		  if (path.endsWith("/")) {
			  return path = path.substring(0,  path.length() - 1);
		  }

		  if (!segment.startsWith("/") && !segment.startsWith("?")) {
			  segment = "/" + segment;
		  }

		  return path + segment;
	}
	
	private String replaceParameters(String value) {
	      int index = 0;
	      int param = 0;
	      String currentParameter = "";
	      
	      // Check all occurrences
	      while ((index = value.indexOf("{", index)) > 0) {
	    	  if (param < pathParams.length) {
	    		  currentParameter = String.valueOf(pathParams[param]);
	    		  param++;
	    	  }
	    		  
	    	  value = value.substring(0, index) + currentParameter + value.substring(value.indexOf("}") + 1);
	      }
	      
	      return value;
	}	

	private void setHeaders(HttpURLConnection connection) throws UnsupportedEncodingException {
		setProxyAuthorization(connection);
		setAuthorization(connection);
		
		for (Map.Entry<String, Object> header : headers.entrySet()) {
			connection.setRequestProperty(header.getKey(), String.valueOf(header.getValue()));
        }
	}

	private void setAuthorization(HttpURLConnection connection) {
		if (authString == null || authString.isEmpty()) return;
		
		authString = "Basic " + Base64.getEncoder().encodeToString(authString.getBytes());
		connection.setRequestProperty("Authorization", authString);
	}

	private void setProxyAuthorization(HttpURLConnection connection) {
		if (proxyUser == null || proxyUser.isEmpty()) return;
		if (proxyPassword == null || proxyPassword.isEmpty()) return;

		String uname_pwd = proxyUser + ":" + proxyPassword;
		String proxyAuthString = "Basic " + Base64.getEncoder().encodeToString(uname_pwd.getBytes());
		connection.setRequestProperty("Proxy-Authorization", proxyAuthString);
	}
}
