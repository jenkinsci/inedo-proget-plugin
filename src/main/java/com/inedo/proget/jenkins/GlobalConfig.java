package com.inedo.proget.jenkins;

import com.inedo.proget.api.ProGetConfig;

import jenkins.model.Jenkins;

public class GlobalConfig {
	private static ProGetConfig config = null;
	
	private GlobalConfig() { }
	
	/**
	 * Inject configuration for testing purposes
	 */
	public static void injectConfiguration(ProGetConfig value) {
		config = value;
	}
	
	public static boolean isProGetRequiredFieldsConfigured(boolean includeUsername) {
		if (config != null) {
			return true;
		}
		
		return getSharedDescriptor().isRequiredFieldsConfigured(includeUsername);
	}

	public static boolean isProGetApiKeyFieldConfigured() {
		if (config != null) {
			return true;
		}
		
		return getSharedDescriptor().isApiKeyConfigured();
	}

	public static ProGetConfig getProGetConfig() {
		if (config != null) {
			return config;
		}
		
		return getSharedDescriptor().getProGetConfig();
	}
	
	private static ProGetConfiguration.DescriptorImpl getSharedDescriptor() {
		return (ProGetConfiguration.DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(ProGetConfiguration.class);
	}

}
