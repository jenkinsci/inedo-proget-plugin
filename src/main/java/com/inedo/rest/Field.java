package com.inedo.rest;

import com.google.common.net.MediaType;

class Field {
	final MediaType type;
	final String name;
	final Object value;

	Field(String name, Object value, MediaType type) {
		this.name = name;
		this.value = value;
		this.type = type;
	}
}
