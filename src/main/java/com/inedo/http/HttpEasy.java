package com.inedo.http;

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
import com.inedo.http.HttpEasyReader.Family;


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
 * Example 
 * ======= 
 * RestResultReader r = RestRequest.request()
 * 							.baseURI(databaseURI) 
 * 							.path(viewPath + "?startkey=\"{startkey}\"&endkey=\"{endkey}\") 
 * 							.urlParameters(startKey[0], endKey[0])
 * 							.get();
 * 
 * String id = r.jsonPath("rows[0].doc._id").getAsString(); 
 * String rev = r.jsonPath("rows[0].doc._rev").getAsString();
 *
 * Error Handling
 * ==============
 * An IOException is thrown whenever a call returns a response code that is not part of the SUCCESS 
 * family (ie 200-299).
 * 
 * In order to prevent an exception being thrown for an expected response use
 * one of the following methods: 
 *  • request().doNotFailOn(Integer... reponseCodes)
 *  • request().doNotFailOn(Family... responseFamily)
 * 
 * Authentication
 * ============== 
 * Supports two formats
 *  • http://username:password@where.ever
 *  • request().authorization(String username, String password)
 * 
 * Host and Certificate Verification
 * ================================= 
 * There is no fine grained control, its more of an all or nothing approach:
 * 
 * RestRequest.withDefaults() 
 * 		.allowAllHosts() 
 * 		.trustAllCertificates()
 * 
 * Proxy 
 * ===== 
 * Only basic authentication is supported, although I believe the domain can be added by included "domain/" 
 * in front of the username (not tested)
 * 
 * RestRequest.withDefaults() 
 * 		.proxy(Proxy proxy) 
 * 		.proxyAuth(String userName, String password) 
 * 		.bypassProxyForLocalAddresses(boolean bypassLocalAddresses)
 *
 * 
 * Redirects 
 * ========= 
 * Redirects are NOT automatically followed - at least for REST base calls - even though the documentation
 * for HttpURLConnection says that it should...
 * 
 * RestResultReader response = RestRequest.request()
 * 		.doNotFailOn(Family.REDIRECTION) 
 * 		.path(url) 
 * 		.head();
 * 
 * if (response.getResponseCodeFamily() == Family.REDIRECTION) {
 * 		... 
 * }
 * 
 */
public class HttpEasy {
	static final Logger LOGGER = LoggerFactory.getLogger(HttpEasy.class);

	// Static values are set by RestRequestDefaults and apply to all requests
	static Proxy proxy = Proxy.NO_PROXY;
	static String proxyUser = null;
	static String proxyPassword = null;
	static boolean bypassProxyForLocalAddresses = true;
	static String defaultbaseURI = "";
	static LogWriter defaultLogWriter = null;
	
	// These only apply per request - but are visible to package
	List<Integer> ignoreResponseCodes = new ArrayList<Integer>();
	List<Family> ignoreResponseFamily = new ArrayList<Family>();

	// These only apply per request
	private String authString = null;
	private String baseURI = "";
	private String path = "";
	private String query = "";
	private Object[] urlParams = new Object[0];
	private DataContentType dataContentType = DataContentType.AUTO_SELECT;
	private Object rawData = null;
	private MediaType rawDataMediaType = null;
	private Map<String, Object> headers = new LinkedHashMap<String, Object>();
	private List<Field> fields = new ArrayList<Field>();
	private LogWriter logWriter = null;
	
	public static HttpEasyDefaults withDefaults() {
		return new HttpEasyDefaults();
	}

	public static HttpEasy request() {
		return new HttpEasy();
	}

	public HttpEasy header(String name, String value) {
		headers.put(name, value);
		return this;
	}

	public HttpEasy authorization(String name, String value) {
		authString = name + ":" + value;
		return this;
	}

	public HttpEasy baseURI(String uri) {
		this.baseURI = uri;
		return this;
	}

	public HttpEasy path(String path) {
		this.path = path;
		return this;
	}

	public HttpEasy query(String query) {
		this.query = query;
		return this;
	}
	
	public HttpEasy urlParameters(Object... pathParams) {
		this.urlParams = pathParams;
		return this;
	}
	
	/**
	 * Sets the content type request property to "multipart/form-data"
	 * Any data that is required must be added via field(name, value, mediaType) method.
	 * Files are supported but any other objects must be easily converted to a string value.
	 */
	public HttpEasy dataForm() {
		if (this.dataContentType != DataContentType.AUTO_SELECT) {
			throw new InvalidParameterException("Content type cannot be changed once set");
		}
		
		this.dataContentType = DataContentType.FORM_DATA;
		return this;
	}
	
	/**
	 * Sets the content type request property to "application/x-www-form-urlencoded"
	 * Any data that is required must be added via field(name, value) method
	 * and the value must be easily converted to a string value.
	 */
	public HttpEasy urlEncodedForm() {
		if (this.dataContentType != DataContentType.AUTO_SELECT) {
			throw new InvalidParameterException("Content type cannot be changed once set");
		}
		
		this.dataContentType = DataContentType.X_WWW_FORM_URLENCODED;
		return this;
	}
	
	/**
	 * Add a simple text field, if urlEncodedForm() has been used will try to guess the content type.
	 * 
	 * <p>If urlEncodedForm() or dataForm() have not been called then the form type will be auto selected:
	 * if field containing a file has been added then content type will be set to "multipart/form-data"
	 * else it will be set to "application/x-www-form-urlencoded"</p>
	 */
	public HttpEasy field(String name, Object value) {
		if (rawData != null) {
			throw new InvalidParameterException("Data cannot be used at the same time as fields");
		}
	
		fields.add(new Field(name, value, null));
		return this;
	}

	/**
	 * Add a text field or attach file.  If value is not a file then it must be easily converted to a string.
	 * 
	 * <p>If urlEncodedForm() or dataForm() have not been called then the form type will be auto selected:
	 * if field containing a file has been added then content type will be set to "multipart/form-data"
	 * else it will be set to "application/x-www-form-urlencoded"</p>
	 */
	public HttpEasy field(String name, Object value, MediaType type) {
		if (rawData != null) {
			throw new InvalidParameterException("Data cannot be used at the same time as fields");
		}

		fields.add(new Field(name, value, type));
		return this;
	}
	
	/**
	 * Sets the content type request property to the value of the supplied media type.
	 * This can only be called once as passing raw data can only support one text block or binary file.
	 * With this in mind files are supported but any other objects must be easily converted to a string value.
	 */
	public HttpEasy data(Object data, MediaType mediaType) {
		if (rawData != null) {
			throw new InvalidParameterException("Only a single data value can be added");
		}

		if (this.dataContentType != DataContentType.AUTO_SELECT) {
			throw new InvalidParameterException("Content type cannot be changed once set");
		}

		dataContentType = DataContentType.RAW;
		rawDataMediaType = mediaType;
		rawData = data;

		return this;
	}

	/**
	 * Add a list of response codes to ignore that would otherwise case a exception to be thrown
	 * eg doNotFailOn(HttpURLConnection.HTTP_CONFLICT)
	 */
	public HttpEasy doNotFailOn(Integer... reponseCodes) {
		this.ignoreResponseCodes.addAll(Arrays.asList(reponseCodes));
		return this;
	}

	public HttpEasy withLogWriter(LogWriter logWriter) {
		this.logWriter = logWriter;
		return this;
	}
	
	/**
	 * Add a list of response family codes to ignore that would otherwise case a exception to be thrown
	 * eg doNotFailOn(Family.REDIRECTION)
	 */
	public HttpEasy doNotFailOn(Family... responseFamily) {
		this.ignoreResponseFamily.addAll(Arrays.asList(responseFamily));
		return this;
	}

	public HttpEasyReader get() throws IOException {
		return new HttpEasyReader(getConnectionMethod("GET"), this);
	}

	public HttpEasyReader head() throws IOException {
		return new HttpEasyReader(getConnectionMethod("HEAD"), this);
	}

	public HttpEasyReader post() throws IOException {
		return new HttpEasyReader(getConnectionMethod("POST"), this);
	}

	public HttpEasyReader put() throws IOException {
		return new HttpEasyReader(getConnectionMethod("PUT"), this);
	}

	public HttpEasyReader delete() throws IOException {
		return new HttpEasyReader(getConnectionMethod("DELETE"), this);
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
		
		if (requestMethod.equals("POST") || requestMethod.equals("PUT")) {
			dataWriter = getDataWriter(dataWriter, url, connection);
			
			connection.setDoOutput(true);
		}
		
		log("Sending " + requestMethod + " to " + url.toString());
		connection.connect();

		if (dataWriter != null) {
			dataWriter.write();
		}
		
		return connection;
	}

	private void log(String message) {
		if (logWriter != null) {
			logWriter.writeLogMessage(message);
		} else if (defaultLogWriter != null) {
			defaultLogWriter.writeLogMessage(message);
		} else {
			LOGGER.trace(message);
		}		
	}

	private DataWriter getDataWriter(DataWriter dataWriter, URL url, HttpURLConnection connection) throws UnsupportedEncodingException {
		if (dataContentType == DataContentType.AUTO_SELECT) {
			if (!fields.isEmpty()) {
				if (fieldsHasFile()) {
					dataContentType = DataContentType.FORM_DATA;
				} else {
					dataContentType = DataContentType.X_WWW_FORM_URLENCODED;
				}
			}
		}
		
		switch (dataContentType) {
			case RAW:
				dataWriter = new RawDataWriter(connection, rawData, rawDataMediaType);
				break;
				
			case FORM_DATA:
				dataWriter = new FormDataWriter(connection, url.getQuery(), fields);
				break;
			
			case X_WWW_FORM_URLENCODED:
				dataWriter = new FormUrlEncodedDataWriter(connection, url.getQuery(), fields);
				break;
				
			case AUTO_SELECT:
				break;
				
			default:
				throw new InvalidParameterException(dataContentType.toString() + " is unknown");
		}
		return dataWriter;
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
		String spec = "";
		
		if (!containsProtol(path) && !containsProtol(query)) {
			spec = (baseURI == null || baseURI.isEmpty()) ? defaultbaseURI : baseURI;	
		}
		
		spec = appendSegmentToUrl(spec, path, "/");
		spec = appendSegmentToUrl(spec, query, "?");
		spec = replaceParameters(spec);

		URL url = new URL(spec);

		if (url.getUserInfo() != null) {
			authString = url.getUserInfo();
			spec = url.toExternalForm().replace(url.getUserInfo() + "@", "");
			url = new URL(spec);
		}

		return url;
	}

	private boolean containsProtol(String url) {
		if (url == null || url.isEmpty()) return false;
		
		return url.contains("//");
	}
	
	private String appendSegmentToUrl(String url, String segment, String join) {
		if (url == null || url.isEmpty()) {
			return segment;
		}

		if (segment == null || segment.isEmpty()) {
			return url;
		}

		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}

		if (!segment.startsWith(join)) {
			segment = join + segment;
		}

		return url + segment;
	}

	private String replaceParameters(String url) {
		int index = 0;
		int param = 0;
		String currentParameter = "";

		// Check all occurrences
		while ((index = url.indexOf("{", index)) > 0) {
			if (param < urlParams.length) {
				currentParameter = String.valueOf(urlParams[param]);
				param++;
			}

			url = url.substring(0, index) + currentParameter + url.substring(url.indexOf("}") + 1);
		}

		return url;
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

	private enum DataContentType {
		AUTO_SELECT, RAW, X_WWW_FORM_URLENCODED, FORM_DATA; 
	}
}
