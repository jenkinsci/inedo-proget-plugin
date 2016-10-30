package com.inedo.proget.jenkins;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;

/**
 * Common Jenkins tasks. 
 * 
 * @author Andrew Sumner
 */
public class JenkinsEnvrionmentHelper {
	private final AbstractBuild<?, ?> build;
	private final TaskListener listener;
		
	/**
	 * For unit tests as they don't have access to the build or listener
	 */
	public JenkinsEnvrionmentHelper() {
		this.build = null;
		this.listener = null;
	}
	
	public JenkinsEnvrionmentHelper(AbstractBuild<?, ?> build, TaskListener listener) {
		this.build = build;
		this.listener = listener;
	}

	public String expandVariable(String variable) {
		if (build == null) {
			return variable;
		}
		
		if (variable == null || variable.isEmpty()) {
			return variable;
		}
		
		String expanded = variable;
		
		try {
			expanded = build.getEnvironment(listener).expand(variable);
		} catch (Exception e) {
			getLogWriter().info("Exception thrown expanding '" + variable + "' : " + e.getClass().getName() + " " + e.getMessage());
		}
		
		return expanded;
	}
	
	public void injectEnvrionmentVariable(String key, String value) {
		if (build == null) {
			return;
		}
		
		build.addAction(new VariableInjectionAction(key, value));
	}
	
	private JenkinsLogWriter logWriter = null;
	        
	private JenkinsLogWriter getLogWriter() {
		if (logWriter == null) {
			logWriter = new JenkinsLogWriter(listener);	
		}
		
		return logWriter;
	}
}
