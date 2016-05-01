package com.inedo.proget.jenkins;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;

import java.util.Scanner;

import com.inedo.http.LogWriter;
import com.inedo.proget.domain.PackageMetadata;

/**
 * Does the real work of Trigger a BuildMaster build, has been seperated out from the Builder and Publisher actions
 * so that the code can be shared between them. 
 * 
 * @author Andrew Sumner
 */
public class JenkinsHelper implements LogWriter {
	private static final String LOG_PREFIX = "[ProGet] "; 
	private final AbstractBuild<?, ?> build;
	private final TaskListener listener;
	
	public JenkinsHelper() {
		this.build = null;
		this.listener = null;
	}
	
	public JenkinsHelper(AbstractBuild<?, ?> build, TaskListener listener) {
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
			info("Exception thrown expanding '" + variable + "' : " + e.getClass().getName() + " " + e.getMessage());
		}
		
		return expanded;
	}
	
	public void injectEnvrionmentVariable(String key, String value) {
		if (build == null) {
			return;
		}
		
		build.addAction(new VariableInjectionAction(key, value));
	}

	@Override
	public void info(String message) {
		if (listener != null) {
			listener.getLogger().println(LOG_PREFIX + message);
		} else {
			System.out.println(LOG_PREFIX + message);
		}
	}

	public void error(String message) {
		if (listener != null) {
			listener.error(LOG_PREFIX + message);
		}
	}
}
