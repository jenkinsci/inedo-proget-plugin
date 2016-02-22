package com.inedo.rest;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import com.google.common.net.MediaType;

class RawDataWriter implements DataWriter {
	private HttpURLConnection connection;
	private final byte[] postEndcoded;
	
	public RawDataWriter(HttpURLConnection connection, MediaType rawDataMediaType, String rawData) {
		this.connection = connection;
		// Assume data is encoded correctly
		//this.postEndcoded = rawData.getBytes(URLEncoder.encode(String.valueOf(data), "UTF-8"));
		this.postEndcoded = rawData.getBytes(StandardCharsets.UTF_8);
		
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Type", rawDataMediaType.toString());
		connection.setRequestProperty("Content-Length", Integer.toString(postEndcoded.length));
	}

	@Override
	public void write() throws IOException {
		try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
			wr.write(postEndcoded);
		}
	}
	
}
