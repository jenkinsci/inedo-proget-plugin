package com.inedo.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.HttpResponseException;
import org.xml.sax.SAXException;

/**
 * Response reader for HTTP requests, can parse JSON and XML and download files.
 * 
 * @author Andrew Sumner
 */
public class HttpEasyReader {
	private HttpURLConnection connection;
	private String returned = null;
	
	/**
	 * Create new HttpEasyReader.
	 * 
	 * @param connection HttpURLConnection
	 * @param request Request that is creating this reader
	 * @throws HttpResponseException if request failed
	 * @throws IOException
	 */
	public HttpEasyReader(HttpURLConnection connection, HttpEasy request) throws IOException {
		this.connection = connection;
		
		Family resposeFamily = getResponseCodeFamily(); 

		//TODO Should I automatically follow redirects?
		if (resposeFamily != Family.SUCCESSFUL) {
			if (listContains(request.ignoreResponseCodes, getResponseCode())) {
				return;
			}
			
			if (listContains(request.ignoreResponseFamily, resposeFamily)) {
				return;
			}
			
			throw new HttpResponseException(getResponseCode(), 
						String.format("Server returned HTTP response code %s: %s%s%s",
								 connection.getResponseCode(), 
								 connection.getResponseMessage(), 
								 (asString().isEmpty()) ? "" : " - ", 
								 asString()));
		}
	}
	
	

	private <T> boolean listContains(List<T> array, T targetValue) {
		for (T s : array) {
			if (s.equals(targetValue)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns the underlying connection object in the event that the 
	 * exposed methods don't provide the information you are after.
	 * 
	 * @return A {@link HttpURLConnection}
	 */
	public HttpURLConnection getConnection() {
		return connection;
	}
	
	/**
	 * Gets the status code from an HTTP response message, see {@link HttpURLConnection#getResponseCode()}.
	 * @return Response code
	 * @throws IOException
	 */
	public int getResponseCode() throws IOException {
		return connection.getResponseCode();
	}

	/**
	 * Gets the family of the status code from an HTTP response message, see {@link Family}.
	 * @return Response family
	 * @throws IOException
	 */
	public Family getResponseCodeFamily() throws IOException {
		return Family.familyOf(connection.getResponseCode());
	}
	
	/** 
	 * @return The response as a string, makes no attempt to determine the content type
	 *  
	 * @throws IOException If unable to read the response
	 */
	public String asString() throws IOException {
		if (returned != null) {
			return returned;
		} 
		
		if (getResponseCodeFamily() == Family.SUCCESSFUL) {
			return asString(connection.getInputStream());
		} else {
			return asString(connection.getErrorStream());
		}
	}
	
	private String asString(InputStream stream) throws IOException {
		returned = "";
		
		// read the output from the server
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
			StringBuilder sb = new StringBuilder();

			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			
			returned = sb.toString();
		} finally {
			connection.disconnect();
		}
	
		return returned;
	}
	
	/**
	 * @return A JsonReader to handle a json response.
	 * 
	 * @throws IOException If unable to read the response
	 */
	public JsonReader getJsonReader() throws IOException {
		return new JsonReader(asString());
	}
	
	/**
	 * @return An XmlReader to handle an xml response.
	 * @throws SAXException 
	 * @throws ParserConfigurationException
	 * @throws IOException If unable to read the response
	 */
	public XmlReader getXmlReader() throws ParserConfigurationException, SAXException, IOException {
		return new XmlReader(asString());
	}
	
	
	/**
	 * Download a file from the response.
	 * 
	 * @param saveDir Location to place the file, the file name is gotten from the response headers
	 * @return File object
	 * @throws IOException If unable to write the file
	 */
	public File downloadFile(String saveDir) throws IOException {
		final int bufferSize = 4096;

		String fileName = parseDispositionFilename(connection.getHeaderField("Content-Disposition"));
		
		if (fileName == null) {
			fileName = connection.getURL().getPath();
			
			if (connection.getURL().getQuery() != null || fileName == null || fileName.isEmpty()) {
				throw new IOException("Unable to get fileName from either Content-Disposition header or url:" + connection.getURL());
			}
		}
		
		fileName = new File(fileName).getName();

//		System.out.println("Content-Type = " + connection.getContentType());
//		System.out.println("Content-Disposition = " + disposition);
//		System.out.println("Content-Length = " + connection.getContentLength());
//		System.out.println("fileName = " + fileName);

		File folder = new File(saveDir);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		
		File saveFile = new File(saveDir, fileName);
		
		try (
			InputStream inputStream = connection.getInputStream();
			FileOutputStream outputStream = new FileOutputStream(saveFile); 
		) {
			
			int bytesRead = -1;
			byte[] buffer = new byte[bufferSize];
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
		} finally {
			connection.disconnect();
		}

		return saveFile;
	}

	/** 
     * Retrieves the "filename" attribute from a content disposition line.
     *
     * @param dispositionString The entire "Content-disposition" string
     * @return <code>null</code> if no filename could be found, otherwise,
     *         returns the filename
     * @see #parseForAttribute(String, String)
     */
	private String parseDispositionFilename(String dispositionString) {
		return parseForAttribute("filename", dispositionString);
	}
	
	/**
     * Parses a string looking for a attribute-value pair, and returns the value.
     * For example:
     * <pre>
     *      String parseString = "Content-Disposition: filename=\"bob\" name=\"jack\"";
     *      MultipartIterator.parseForAttribute(parseString, "name");
     * </pre>
     * That will return "bob".
     * 
     * @param attribute The name of the attribute you're trying to get
     * @param parseString The string to retrieve the value from
     * @return The value of the attribute, or <code>null</code> if none could be found
     */
	public static String parseForAttribute(String attribute, String parseString) {
		if (parseString == null) {
			return null;
		}

		int nameIndex = parseString.indexOf(attribute + "=\"");
		if (nameIndex != -1) {
			int endQuoteIndex = parseString.indexOf("\"", nameIndex + attribute.length() + 3);

			if (endQuoteIndex != -1) {
				return parseString.substring(nameIndex + attribute.length() + 2, endQuoteIndex);
			}
		}

		return null;
	}
    
	

    /**
     * Response header field.
     * @param name Field name
     * @return Field value
     */
	public String getHeaderField(String name) {
		return connection.getHeaderField(name);
	}
		
	
	/**
     * An enumeration representing the class of an http response status code.
     */
	public enum Family {

		/**
		 * {@code 1xx} HTTP status codes.
		 */
		INFORMATIONAL,
		/**
		 * {@code 2xx} HTTP status codes.
		 */
		SUCCESSFUL,
		/**
		 * {@code 3xx} HTTP status codes.
		 */
		REDIRECTION,
		/**
		 * {@code 4xx} HTTP status codes.
		 */
		CLIENT_ERROR,
		/**
		 * {@code 5xx} HTTP status codes.
		 */
		SERVER_ERROR,
		/**
		 * Other, unrecognized HTTP status codes.
		 */
		OTHER;

		/**
		 * Get the response status family for the status code.
		 *
		 * @param statusCode response status code to get the family for.
		 * @return family of the response status code.
		 */
		public static Family familyOf(final int statusCode) {
			switch (statusCode / 100) {
			case 1:
				return Family.INFORMATIONAL;
			case 2:
				return Family.SUCCESSFUL;
			case 3:
				return Family.REDIRECTION;
			case 4:
				return Family.CLIENT_ERROR;
			case 5:
				return Family.SERVER_ERROR;
			default:
				return Family.OTHER;
			}
		}
	}
}