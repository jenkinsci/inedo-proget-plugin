package com.inedo.proget.jenkins;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;

import jenkins.model.Jenkins;

import java.util.Scanner;

import com.inedo.http.LogWriter;
import com.inedo.proget.api.ProGetConfig;
import com.inedo.proget.domain.PackageMetadata;

/**
 * Does the real work of Trigger a BuildMaster build, has been seperated out from the Builder and Publisher actions
 * so that the code can be shared between them. 
 * 
 * @author Andrew Sumner
 */
public class ProGetHelper implements LogWriter {
	private static final String LOG_PREFIX = "[ProGet] "; 
	private static ProGetConfig config = null;
	private final AbstractBuild<?, ?> build;
	private final TaskListener listener;
	
	public ProGetHelper() {
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
	
	public boolean isProGetRequiredFieldsConfigured() {
		if (config != null) {
			return true;
		}
		
		return getSharedDescriptor().isRequiredFieldsConfigured();
	}

	public boolean isProGetApiKeyFieldConfigured() {
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
	
	public String expandVariable(String variable) {
		if (variable == null || variable.isEmpty()) {
			return variable;
		}
		
		String expanded = variable;
		
		try {
			if (build != null) {
				expanded = build.getEnvironment(listener).expand(variable);
			}
		} catch (Exception e) {
			info("Exception thrown expanding '" + variable + "' : " + e.getClass().getName() + " " + e.getMessage());
		}
		
		return expanded;
	}
	
	public static void injectEnvrionmentVariable(AbstractBuild<?, ?> build, String key, String value) {
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
	
	public PackageMetadata getMetadata(UploadPackageBuilder settings) {
		PackageMetadata metadata = new PackageMetadata();

		metadata.group = expandVariable(settings.getGroupName());
		metadata.packageName = expandVariable(settings.getPackageName());
		metadata.version = expandVariable(settings.getVersion());
		metadata.title = settings.getTitle();
		metadata.description = settings.getDescription();		
		
		try (Scanner scanner = new Scanner(settings.getMetadata())) {
			while (scanner.hasNextLine()){
				String line = scanner.nextLine();
				
				int pos = line.indexOf("=");
				
				if (pos > 0) {
					String name = line.substring(0, pos).trim();
				    String value = line.substring(pos + 1).trim();
				    
				    metadata.extendedAttributes.put(name, expandVariable(value));
			    } else {
			    	return null;
				}
		    } 
		}
		
		return metadata;
	}
}
