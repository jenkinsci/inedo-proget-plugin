package com.inedo.proget.jenkins;

import com.inedo.http.HttpEasyListener;

public abstract class JenkinsLogWriter implements HttpEasyListener {
	static final String LOG_PREFIX = "[ProGet] ";
	
	public abstract void info(String message);
	
	public abstract void error(String message);
	
	public abstract void fatalError(String message);
	
    @Override
    public void request(String msg, Object... args) {
        info(getFormattedMessage(msg, args));
    }

    @Override
    public void details(String msg, Object... args) {
    	 info(getFormattedMessage(msg, args));
    }

    @Override
    public void error(String message, Throwable t) {
        error(message);
    }
    
	private String getFormattedMessage(String message, Object... args) {
	    String msg = message.replace("{}", "%s");
	    return String.format(msg, args);
	}
}
