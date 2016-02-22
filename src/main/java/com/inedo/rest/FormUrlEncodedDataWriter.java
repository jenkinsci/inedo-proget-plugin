package com.inedo.rest;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

class FormUrlEncodedDataWriter implements DataWriter {
	private final HttpURLConnection connection;
	private final byte[] postEndcoded;
	
	public FormUrlEncodedDataWriter(HttpURLConnection connection, String query, List<Field> fields) throws UnsupportedEncodingException {
		this.connection = connection;
		
		StringBuilder postData = new StringBuilder();
		
		if (query != null && !query.isEmpty()) {
			postData.append(query);
		}
		
		for (Field field : fields) {
			if (postData.length() > 0) postData.append('&');
            postData.append(field.name);
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(field.value), "UTF-8"));
		}
		
		postEndcoded = postData.toString().getBytes(StandardCharsets.UTF_8);
		
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Content-Length", Integer.toString(postEndcoded.length));
	}

	@Override
	public void write() throws IOException {
		try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
			wr.write(postEndcoded);
		}
	}
	
}
