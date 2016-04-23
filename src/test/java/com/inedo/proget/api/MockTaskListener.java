package com.inedo.proget.api;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import hudson.console.ConsoleNote;
import hudson.model.TaskListener;

public class MockTaskListener implements TaskListener {

	@Override
	public PrintStream getLogger() {
		return System.out;
	}

	@Override
	public void annotate(ConsoleNote ann) throws IOException {
	}

	@Override
	public void hyperlink(String url, String text) throws IOException {
	}

	@Override
	public PrintWriter error(String msg) {
		System.err.println(msg);
		return null;
	}

	@Override
	public PrintWriter error(String format, Object... args) {
		System.err.println(String.format(format, args));
		return null;
	}

	@Override
	public PrintWriter fatalError(String msg) {
		System.err.println(msg);
		return null;
	}

	@Override
	public PrintWriter fatalError(String format, Object... args) {
		System.err.println(String.format(format, args));
		return null;
	}

}
