package com.inedo.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogWriter implements HttpEasyListener {
    static final Logger LOGGER = LoggerFactory.getLogger(HttpEasy.class);
    private boolean logRequestDetails = false;
    
    public LogWriter setLogRequestDetails(boolean logRequestDetails) {
        this.logRequestDetails = logRequestDetails;
        return this;
    }

    @Override
    public void request(String msg, Object... args) {
        LOGGER.trace(msg, args);
    }
    
    @Override
    public void details(String msg, Object... args) {
        if (logRequestDetails) {
            LOGGER.trace(msg, args);
        }
    }

    @Override
    public void error(String message, Throwable t) {
        LOGGER.error(message, t);
    }
}
