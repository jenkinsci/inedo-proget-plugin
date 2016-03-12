package com.inedo.proget.jenkins;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.PrintStream;
import jenkins.model.Jenkins;

import com.inedo.proget.api.ProGetConfig;

/**
 * Does the real work of Trigger a BuildMaster build, has been seperated out from the Builder and Publisher actions
 * so that the code can be shared between them. 
 * 
 * @author Andrew Sumner
 */
public class ProGetHelper {
	public static final String LOG_PREFIX = "[ProGet] "; 
	public static final String DEFAULT_BUILD_NUMBER = "${BUILDMASTER_BUILD_NUMBER}"; 
	private static ProGetConfig config = null;
	
	/**
	 * TODO: As I haven't been able to successfully mock a static class this is my work around for testing purposes
	 */
	public static void injectConfiguration(ProGetConfig value) {
		config = value;
	}
	
	public static boolean validateProGetConfig() {
		if (config != null) {
			return true;
		}
		
		return getSharedDescriptor().validatePluginConfiguration();
	}

	public static ProGetConfig getProGetConfig(PrintStream logger) {
		if (config != null) {
			return config;
		}
		
		return getSharedDescriptor().getProGetConfig(logger);
	}
	
	private static ProGetConfiguration.DescriptorImpl getSharedDescriptor() {
		return (ProGetConfiguration.DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(ProGetConfiguration.class);
	}
	
	public static String expandVariable(AbstractBuild<?, ?> build, BuildListener listener, String variable) {
		if (variable == null || variable.isEmpty()) {
			return variable;
		}
		
		String expanded = variable;
		
		try {
			expanded = build.getEnvironment(listener).expand(variable);
		} catch (Exception e) {
			listener.getLogger().println(LOG_PREFIX + "Exception thrown expanding '" + variable + "' : " + e.getClass().getName() + " " + e.getMessage());
		}
		
		return expanded;
	}
	
}
