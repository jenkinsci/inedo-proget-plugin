package com.inedo.rest;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;
import com.inedo.rest.EasyHttpReader.Family;

/**
 * Rest request utility that supports NTLM proxy authentication.
 * 
 * This is been designed with as a fluent REST API similar to RestEasy and
 * RestAssurred with the only real difference being that it has great proxy
 * support.
 * 
 * Behind the scenes java.net.HttpURLConnection is used as this is the only
 * thing that I found that could get through an NTLM proxy easily.
 * 
 * There are two starting points for creating a rest request:
 * RestRequest.withDefaults() - allows you to set some settings that apply to
 * all requests such as configuring a proxy RestRequest.request() - perform the
 * actual call, these HTTP methods are implemented: GET, HEAD, POST, PUT, DELETE
 * 
 * Note: if your url can contain weird characters you will want to encode it,
 * something like this: myUrl = URLEncoder.encode(myUrl, "UTF-8");
 *
 * Have a look at these as an alternative to HttpURLConnection:
 * http://restlet.com/projects/restlet-framework/
 * http://hc.apache.org/httpcomponents-client-ga/
 * https://github.com/hgoebl/DavidWebb#background
 *
 * Example ======= RestResultReader r = RestRequest.request()
 * .baseURI(databaseURI) .path(viewPath +
 * "?startkey=\"{startkey}\"&endkey=\"{endkey}\") .pathParameters(startKey[0],
 * endKey[0]) .get();
 * 
 * String id = r.jsonPath("rows[0].doc._id").getAsString(); String rev =
 * r.jsonPath("rows[0].doc._rev").getAsString();
 *
 * Error Handling ============== An IOException is thrown whenever a call
 * returns a response code that is not part of the SUCCESS family (ie 200-299).
 * 
 * In order to prevent an exception being thrown for an expected response use
 * one of the following methods: • request().doNotFailOn(Integer...
 * reponseCodes) • request().doNotFailOn(Family... responseFamily)
 * 
 * Authentication ============== supports two formats •
 * http://username:password@where.ever • request().authorization(String
 * username, String password)
 * 
 * Host and Certificate Verification ================================= There is
 * no fine grained control, its more of an all or nothing approach:
 * 
 * RestRequest.withDefaults() .allowAllHosts() .trustAllCertificates()
 * 
 * Proxy ===== Only basic authentication is supported, although I believe the
 * domain can be added by included "domain/" infront of the username (not
 * tested)
 * 
 * RestRequest.withDefaults() .proxy(Proxy proxy) .proxyAuth(String userName,
 * String password) .bypassProxyForLocalAddresses(boolean bypassLocalAddresses)
 *
 * 
 * Redirects ========= Redirects are NOT automatically followed - at least for
 * REST base calls - even though the documentation for HttpURLConnection says
 * that it should...
 * 
 * RestResultReader response = RestRequest.request()
 * .doNotFailOn(Family.REDIRECTION) .path(url) .head();
 * 
 * if (response.getResponseCodeFamily() == Family.REDIRECTION) { ... }
 * 
 */
public class EasyHttp {
	static final Logger LOGGER = LoggerFactory.getLogger(EasyHttp.class);

	// Static values are set by RestRequestDefaults and apply to all requests
	static Proxy proxy = Proxy.NO_PROXY;
	static String proxyUser = null;
	static String proxyPassword = null;
	static boolean bypassProxyForLocalAddresses = true;

	// These only apply per request - but are visible to package
	List<Integer> ignoreResponseCodes = new ArrayList<Integer>();
	List<Family> ignoreResponseFamily = new ArrayList<Family>();

	// These only apply per request
	private String authString = null;
	private String baseURI = "";
	private String path = "";
	private Object[] urlParams = new Object[0];
	private String rawData = null;
	private MediaType rawDataMediaType = null;
	private Map<String, Object> headers = new LinkedHashMap<String, Object>();
	private List<Field> fields = new ArrayList<Field>();

	public static EasyHttpDefaults withDefaults() {
		return new EasyHttpDefaults();
	}

	public static EasyHttp request() {
		return new EasyHttp();
	}

	public EasyHttp header(String name, String value) {
		headers.put(name, value);
		return this;
	}

	public EasyHttp authorization(String name, String value) {
		authString = name + ":" + value;
		return this;
	}

	public EasyHttp baseURI(String uri) {
		this.baseURI = uri;
		return this;
	}

	public EasyHttp path(String path) {
		this.path = path;
		return this;
	}

	public EasyHttp urlParameters(Object... pathParams) {
		this.urlParams = pathParams;
		return this;
	}

	public EasyHttp doNotFailOn(Integer... reponseCodes) {
		this.ignoreResponseCodes.addAll(Arrays.asList(reponseCodes));
		return this;
	}

	public EasyHttp doNotFailOn(Family... responseFamily) {
		this.ignoreResponseFamily.addAll(Arrays.asList(responseFamily));
		return this;
	}

	public EasyHttp data(MediaType mediaType, String data) {
		if (rawData != null) {
			throw new InvalidParameterException("Only a single data value can be added");
		}

		if (!fields.isEmpty()) {
			throw new InvalidParameterException("Data cannot be used at the same time as fields");
		}

		rawDataMediaType = mediaType;
		rawData = data;

		return this;
	}

	public EasyHttp field(String name, Object value) {
		if (rawData != null) {
			throw new InvalidParameterException("Data cannot be used at the same time as fields");
		}

		fields.add(new Field(name, value, null));
		return this;
	}

	public EasyHttp field(String name, Object value, MediaType type) {
		if (rawData != null) {
			throw new InvalidParameterException("Data cannot be used at the same time as fields");
		}

		fields.add(new Field(name, value, type));
		return this;
	}

	public EasyHttpReader get() throws IOException {
		return new EasyHttpReader(getConnectionMethod("GET"), this);
	}

	public EasyHttpReader head() throws IOException {
		return new EasyHttpReader(getConnectionMethod("HEAD"), this);
	}

	public EasyHttpReader post() throws IOException {
		return new EasyHttpReader(getConnectionMethod("POST"), this);
	}

	public EasyHttpReader put() throws IOException {
		return new EasyHttpReader(getConnectionMethod("PUT"), this);
	}

	public EasyHttpReader delete() throws IOException {
		return new EasyHttpReader(getConnectionMethod("DELETE"), this);
	}

	private HttpURLConnection getConnectionMethod(String requestMethod) throws IOException 
	{	
		DataWriter dataWriter = null;
		URL url = getURL();
		HttpURLConnection connection = getConnection(url);
        
		setHeaders(connection);

		connection.setRequestMethod(requestMethod);
		connection.setReadTimeout(15*1000);	// give it 15 seconds to respond
		connection.setInstanceFollowRedirects(false);
		
		boolean isPost = requestMethod.equals("POST") || requestMethod.equals("PUT");
		
		if (isPost) {
			if (rawData != null) {
				dataWriter = new RawDataWriter(connection, rawDataMediaType, rawData);
			}
			
			if (!fields.isEmpty()) {
				if (fieldsHasFile()) {
					dataWriter = new FormDataWriter(connection, url.getQuery(), fields);
				} else {
					dataWriter = new FormUrlEncodedDataWriter(connection, url.getQuery(), fields);
				}
			}
			
			connection.setDoOutput(true);
		}
		
		LOGGER.trace("Sending " + requestMethod + " to " + url.toString());
		connection.connect();

		if (dataWriter != null) {
			dataWriter.write();
		}
		
		return connection;
	}

	private boolean fieldsHasFile() {
		for (Field field : fields) {
			if (field.value instanceof File) {
				return true;
			}
		}

		return false;
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
		// check to / or ? on path and base not end in /
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
			return path = path.substring(0, path.length() - 1);
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
			if (param < urlParams.length) {
				currentParameter = String.valueOf(urlParams[param]);
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
		if (authString == null || authString.isEmpty())
			return;

		authString = "Basic " + Base64.getEncoder().encodeToString(authString.getBytes());
		connection.setRequestProperty("Authorization", authString);
	}

	private void setProxyAuthorization(HttpURLConnection connection) {
		if (proxyUser == null || proxyUser.isEmpty())
			return;
		if (proxyPassword == null || proxyPassword.isEmpty())
			return;

		String uname_pwd = proxyUser + ":" + proxyPassword;
		String proxyAuthString = "Basic " + Base64.getEncoder().encodeToString(uname_pwd.getBytes());
		connection.setRequestProperty("Proxy-Authorization", proxyAuthString);
	}

	
}
