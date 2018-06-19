package com.inedo.proget.jenkins.utils;

public class JenkinsConsoleLogWriter extends JenkinsLogWriter {
    
    @Override
    public void info(String message) {
        System.out.println(prefixLines(message));
    }

    @Override
    public void error(String message) {
        System.err.println(prefixLines(message));
    }

    @Override
    public void fatalError(String message) {
        System.err.println(prefixLines(message));
    }
}
