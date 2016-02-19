package com.inedo.proget;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import jenkins.model.Jenkins;

import com.inedo.proget.api.ProGet;
import com.inedo.proget.api.ProGetConfig;
import com.inedo.proget.domain.Variable;

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
	
	public static boolean triggerBuild(AbstractBuild<?, ?> build, BuildListener listener, Triggerable trigger) throws IOException, InterruptedException {
		if (!validateProGetConfig()) {
			listener.getLogger().println("Please configure PorGet Plugin global settings");
			return false;
		}
		
		ProGetConfig config = getProGetConfig(listener.getLogger());
		ProGet proget = new ProGet(config);		
		String feedName = trigger.getFeedName();
		
		listener.getLogger().println(LOG_PREFIX + "Upload to " + feedName);
			
		proget.upload(feedName);
		
		return true;
	}

}
