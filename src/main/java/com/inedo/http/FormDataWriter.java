package com.inedo.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.net.MediaType;

class FormDataWriter implements DataWriter {
	private final HttpURLConnection connection;
	private final List<Field> fields;
	private final String boundary = "---FormBoundary" + System.currentTimeMillis();
	private OutputStream outputStream;
    private PrintWriter writer = null;
    private static final String LINE_FEED = "\r\n";
    
	public FormDataWriter(HttpURLConnection connection, String query, List<Field> fields) throws UnsupportedEncodingException {
		this.connection = connection;
		this.fields = fields;
		
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
	}

	@Override
	public void write() throws IOException {
		outputStream = connection.getOutputStream();
		
		try {
			writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);
						
			for (Field field : fields) {
				if (field.value instanceof File) {
					addFilePart(field.name, (File)field.value, field.type);
				} else {
					addFormField(field.name, field.value);
				}
			}
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	/**
     * Adds a form field to the request
     * @param name field name
     * @param value field value
     */
    public void addFormField(String name, Object value) {
        writer.append(boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=utf-8").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(String.valueOf(value)).append(LINE_FEED);
        writer.flush();
    }
 
    /**
     * Adds a upload file section to the request
     * @param fieldName name attribute in <input type="file" name="..." />
     * @param uploadFile a File to be uploaded
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile, MediaType type) throws IOException {
        String fileName = uploadFile.getName();
                
        writer.append(boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"").append(LINE_FEED);
        if (type == null) {
        	writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(fileName)).append(LINE_FEED);
        } else {
        	writer.append("Content-Type: " + type.toString()).append(LINE_FEED);
        }
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();
 
        try (FileInputStream inputStream = new FileInputStream(uploadFile)) {
	        byte[] buffer = new byte[4096];
	        int bytesRead = -1;
	        while ((bytesRead = inputStream.read(buffer)) != -1) {
	            outputStream.write(buffer, 0, bytesRead);
	        }
	        outputStream.flush();
        }
        
        writer.append(LINE_FEED);
        writer.flush();    
    }
}
