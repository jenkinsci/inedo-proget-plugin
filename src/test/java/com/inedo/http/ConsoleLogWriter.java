package com.inedo.http;

public class ConsoleLogWriter implements LogWriter {

	@Override
	public void info(String message) {
		System.out.println(message);
	}

}
