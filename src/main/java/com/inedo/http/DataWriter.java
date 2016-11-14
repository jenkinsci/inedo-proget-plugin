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
	 * @param eventManager Listener(s) to write details to
	 * 
	 * @throws IOException
	 */
	public void write(EventManager eventManager) throws IOException;

}
