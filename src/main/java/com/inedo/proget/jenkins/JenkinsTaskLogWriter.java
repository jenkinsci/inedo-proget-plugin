package com.inedo.proget.jenkins;

import hudson.model.TaskListener;

public class JenkinsTaskLogWriter extends JenkinsLogWriter {
	private final TaskListener listener;
		
	public JenkinsTaskLogWriter(TaskListener listener) {
		this.listener = listener;
	}

	@Override
    public void info(String message) {
        listener.getLogger().println(LOG_PREFIX + message);
    }

    @Override
    public void error(String message) {
        listener.error(LOG_PREFIX + message);
    }
    
    @Override
    public void fatalError(String message) {
        listener.fatalError(LOG_PREFIX + message);
    }
}
