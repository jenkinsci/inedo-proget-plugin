package com.inedo.http;

public interface HttpEasyListener {
    public void request(String msg, Object... args);
    public void details(String msg, Object... args);
    public void error(String message, Throwable t);
}
