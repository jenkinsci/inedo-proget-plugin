package com.inedo.proget.jenkins.utils;

import org.concordion.cubano.driver.http.LogWriter;

public abstract class JenkinsLogWriter extends LogWriter {
    static final String LOG_PREFIX = "[ProGet] ";

    public abstract void info(String message);

    public abstract void error(String message);

    public abstract void fatalError(String message);

    @Override
    public void info(String msg, Object... args) {
        info(getFormattedMessage(msg, args));
    }

    @Override
    public void request(String msg, Object... args) {
        info(getFormattedMessage(msg, args));
    }

    @Override
    public void response(String msg, Object... args) {
        info(getFormattedMessage(msg, args));
    }

    @Override
    public void error(String message, Throwable t) {
        error(message);
    }
    
    protected String prefixLines(String message) {
        return message.replaceAll("(?m)^", LOG_PREFIX);
    }
}
