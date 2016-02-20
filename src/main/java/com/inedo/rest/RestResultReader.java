package com.inedo.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

// Class to segregate reading return values
public class RestResultReader {
	HttpURLConnection connection;
	JsonElement json = null;
	String returned = null;
	
	public RestResultReader(HttpURLConnection connection, RestRequest request) throws IOException {
		this.connection = connection;
		
		Family resposeFamily = getResponseCodeFamily(); 
		
		if (resposeFamily != Family.SUCCESSFUL) {
			if (arrayContains(request.ignoreResponseCodes, getResponseCode())) return;
			if (arrayContains(request.ignoreResponseFamily, resposeFamily)) return;

			throw new IOException("Server returned HTTP response code " + connection.getResponseCode() + ": " + connection.getResponseMessage());
		}
	}
	
	private <T> boolean arrayContains(T[] array, T targetValue) {
		for(T s : array){
			if(s.equals(targetValue))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns the underlying connection object in the event that the 
	 * exposed methods don't provide the information you are after.
	 * 
	 * @return
	 */
	public HttpURLConnection getConnection() {
		return connection;
	}
	
	public int getResponseCode() throws IOException {
		return connection.getResponseCode();
	}
	
	public Family getResponseCodeFamily() throws IOException {
		return Family.familyOf(connection.getResponseCode());
	}
	
	public String asString() throws IOException {
		if (returned == null) {
			// read the output from the server
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				StringBuilder sb = new StringBuilder();
	
				String line = null;
				while ((line = reader.readLine()) != null)
				{
					sb.append(line + "\n");
				}
				
				returned = sb.toString();
			} finally {
				connection.disconnect();
			}
		}
		
		return returned;
	}
	
	public String asPrettyString() throws IOException {
		JsonParser parser = new JsonParser();
		JsonElement json = parser.parse(asString());
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(json);
	}
	
	/**
	 * Deserialize the returned Json string into an object of the specified class.
	 * 
	 * @param type
	 * @return
	 * @throws IOException
	 */
	public <T> T asJson(Class<T> type) throws IOException {
		return new Gson().fromJson(asString(), type);		
	}
	
	public File downloadFile(String saveDir) throws IOException {
		final int BUFFER_SIZE = 4096;

		String fileName = "";
		String disposition = connection.getHeaderField("Content-Disposition");

		if (disposition != null) {
			// extracts file name from header field
			int index = disposition.indexOf("filename=");
			if (index > 0) {
				fileName = disposition.substring(index + 10, disposition.length() - 1);
			}
		} else {
			throw new IOException("Unable to get fileName from " + connection.getURL());
		}

//		System.out.println("Content-Type = " + connection.getContentType());
//		System.out.println("Content-Disposition = " + disposition);
//		System.out.println("Content-Length = " + connection.getContentLength());
//		System.out.println("fileName = " + fileName);

		File saveFile = new File(saveDir, fileName);;
		InputStream inputStream = null;
		FileOutputStream outputStream = null;
		
		try {
			// opens input stream from the HTTP connection
			inputStream = connection.getInputStream();
	
			// opens an output stream to save into file
			outputStream = new FileOutputStream(saveFile);
	
			int bytesRead = -1;
			byte[] buffer = new byte[BUFFER_SIZE];
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
		} finally {
			if (outputStream != null) outputStream.close();
			if (inputStream != null) inputStream.close();
			connection.disconnect();
		}
		
		return saveFile;
    }
	
	/**
	 * Deserialize the returned Json string 
	 * @return
	 * @throws IOException
	 * 
	 * @see
	 * https://code.google.com/p/json-simple/
	 */
	public JsonElement asJson() throws IOException {
		if (json == null) {
			json = new JsonParser().parse(asString());
		}
		
		return json;
	}
	
    public JsonElement jsonPath (String path) throws IOException {
    	return jsonPath(asJson(), path);
	} 
    
    private JsonElement jsonPath(JsonElement json, String path) {
    	if (!path.contains(".")) {
	        return json.getAsJsonObject().get(path); 
	    } else {
	    	JsonElement newJson;
	        String next = path.split("[/.]")[0]; 
	        
	        if (next.endsWith("]")) {
	        	int pos = next.lastIndexOf('[');
	        	String index = next.substring(pos + 1, next.length() - 1);
		        next = next.substring(0, pos);
	        	
		        newJson = json.getAsJsonObject().get(next).getAsJsonArray().get(Integer.valueOf(index));
	        } else {
		        newJson = json.getAsJsonObject().get(next);
	        }
		        
		    String newPath = path.substring(path.indexOf(".") + 1); 
		        
	        return jsonPath(newJson, newPath);
	    } 
	}

	public String getHeaderField(String name) {
		return connection.getHeaderField(name);
	}
		
	
	/**
     * An enumeration representing the class of status code. Family is used
     * here since class is overloaded in Java.
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