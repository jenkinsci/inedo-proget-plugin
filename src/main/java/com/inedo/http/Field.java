package com.inedo.http;

import com.google.common.net.MediaType;

/**
 * Form field for http request.
 *  
 * @author Andrew Sumner
 */
class Field {
	final MediaType type;
	final String name;
	final Object value;

	/**
	 * Create a new form field.
	 * 
	 * @param name Field name
	 * @param value Field contents
	 * @param type Field media type
	 */
	Field(String name, Object value, MediaType type) {
		this.name = name;
		this.value = value;
		this.type = type;
	}
}
