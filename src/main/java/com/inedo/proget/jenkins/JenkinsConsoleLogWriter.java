package com.inedo.proget.jenkins;

public class JenkinsConsoleLogWriter extends JenkinsLogWriter {
    
	@Override
	public void info(String message) {
		System.out.println(LOG_PREFIX + message);
	}

	@Override
	public void error(String message) {
	    System.err.println(LOG_PREFIX + message);
	}
	
	@Override
	public void fatalError(String message) {
	    System.err.println(LOG_PREFIX + message);
    }
}
