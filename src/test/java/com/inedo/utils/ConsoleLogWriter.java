package com.inedo.utils;

import com.inedo.http.LogWriter;

public class ConsoleLogWriter implements LogWriter {

	@Override
	public void info(String message) {
		System.out.println(message);
	}

}
