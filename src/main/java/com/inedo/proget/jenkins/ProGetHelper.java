package com.inedo.proget.jenkins;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;

import jenkins.model.Jenkins;

import com.inedo.http.LogWriter;
import com.inedo.proget.api.ProGetConfig;

/**
 * Does the real work of Trigger a BuildMaster build, has been seperated out from the Builder and Publisher actions
 * so that the code can be shared between them. 
 * 
 * @author Andrew Sumner
 */
public class ProGetHelper implements LogWriter {
	private static final String LOG_PREFIX = "[ProGet] "; 
	private static ProGetConfig config;
	private final AbstractBuild<?, ?> build;
	private final TaskListener listener;
	
	public ProGetHelper(ProGetConfig config) {
		ProGetHelper.config = config;
		this.build = null;
		this.listener = null;
	}
	
	public ProGetHelper(AbstractBuild<?, ?> build, TaskListener listener) {
		this.build = build;
		this.listener = listener;
	}

	/**
	 * TODO: As I haven't been able to successfully mock a static class this is my work around for testing purposes
	 */
	public static void injectConfiguration(ProGetConfig value) {
		config = value;
	}
	
	public boolean validateProGetConfig() {
		if (config != null) {
			return true;
		}
		
		boolean valid = getSharedDescriptor().validatePluginConfiguration();
		
		if (!valid) {
			info("Please configure ProGet Plugin global settings");
		}
		
		return valid;
	}

	public ProGetConfig getProGetConfig() {
		if (config != null) {
			return config;
		}
		
		return getSharedDescriptor().getProGetConfig();
	}
	
	private ProGetConfiguration.DescriptorImpl getSharedDescriptor() {
		return (ProGetConfiguration.DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(ProGetConfiguration.class);
	}
	
	public String expandVariable(String variable) {
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

	@Override
	public void info(String message) {
		if (listener != null) {
			listener.getLogger().println(LOG_PREFIX + message);
		}
	}

	public void error(String message) {
		if (listener != null) {
			listener.error(LOG_PREFIX + message);
		}
	}
}
