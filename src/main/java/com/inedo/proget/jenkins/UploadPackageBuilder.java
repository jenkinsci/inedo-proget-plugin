package com.inedo.proget.jenkins;

import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * Uploads a universal package from ProGet.
 *
 * @author Andrew Sumner
 */
public class UploadPackageBuilder extends Builder {
	private final String feedName;
	private final String packageName;
	private final String sourceFiles;
	private final String metadata;

	// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
	@DataBoundConstructor
	public UploadPackageBuilder(String feedName, String packageName, String sourceFiles, String metadata) {
		this.feedName = feedName;
		this.packageName = packageName;
		this.sourceFiles = sourceFiles;
		this.metadata = metadata;
	}

	
	public String getFeedName() {
		return feedName;
	}
	
	public String getPackageName() {
		return packageName;
	}
	
	public String getSourceFiles() {
		return sourceFiles;
	}

	public String getMetadata() {
		return metadata;
	}
	
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		return false; //ProGetHelper.triggerBuild(build, listener, this);
	}

	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		public DescriptorImpl() {
			super(UploadPackageBuilder.class);
		}

//		@Override
//		public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
//			return req.bindJSON(TriggerBuildBuildStep.class, formData);
//		}
		
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			// Indicates that this builder can be used with all kinds of project types
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Upload ProGet Package";
		}
		
		// TODO jelly expandableTextbox does not support form validation currently so this does nothing: 
		// https://github.com/jenkinsci/jenkins/blob/master/core/src/main/resources/lib/form/expandableTextbox.jelly
		public FormValidation doCheckVariables(@QueryParameter String value) {
			try {
//				ProGetHelper.getVariablesList(value);
			} catch (Exception e) {
                return FormValidation.error(e.getMessage());
            }
            
            return FormValidation.ok();
		}
	}
}
