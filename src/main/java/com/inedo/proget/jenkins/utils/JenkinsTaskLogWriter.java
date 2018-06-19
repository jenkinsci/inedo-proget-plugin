package com.inedo.proget.jenkins.utils;

import hudson.model.TaskListener;

public class JenkinsTaskLogWriter extends JenkinsLogWriter {
    private final TaskListener listener;

    public JenkinsTaskLogWriter(TaskListener listener) {
        this.listener = listener;
    }

    @Override
    public void info(String message) {
        listener.getLogger().println(prefixLines(message));
    }

    @Override
    public void error(String message) {
        listener.error(prefixLines(message));
    }
    
    @Override
    public void fatalError(String message) {
        listener.fatalError(prefixLines(message));
    }
}
