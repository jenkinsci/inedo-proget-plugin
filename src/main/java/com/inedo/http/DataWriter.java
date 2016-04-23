package com.inedo.http;

import java.io.IOException;

/**
 * Http request data writer interface.
 * 
 * @author Andrew Sumner
 */
interface DataWriter {

	/**
	 * Add data to Http request.
	 * 
	 * @throws IOException
	 */
	public void write() throws IOException;

}
