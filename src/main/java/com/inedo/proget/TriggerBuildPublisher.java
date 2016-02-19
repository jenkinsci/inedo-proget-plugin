package com.inedo.proget;

import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * 
 * The TriggerBuildPostBuildStep will trigger a build in BuildMaster for a selected application and release
 * as a post-build action, after the build has completed.
 * 
 * @author Andrew Sumner 
 */
@SuppressWarnings("unchecked")
public class TriggerBuildPublisher extends Recorder implements Triggerable {
	private final boolean waitTillBuildCompleted;
	private final boolean printLogOnFailure;
	private final boolean setBuildVariables;
	private final boolean preserveVariables;
	private final String variables;
	private final boolean enableReleaseDeployable;
	private final String deployableId;
	private final String applicationId;
	private final String releaseNumber;
	private final String buildNumber;

	// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
	@DataBoundConstructor
	public TriggerBuildPublisher(JSONObject waitTillBuildCompleted, JSONObject setBuildVariables, JSONObject enableReleaseDeployable, String applicationId, String releaseNumber, String buildNumber) {
		if (waitTillBuildCompleted != null) { 
			this.waitTillBuildCompleted = true; 
			this.printLogOnFailure = "true".equalsIgnoreCase(waitTillBuildCompleted.getString("printLogOnFailure"));
		} else { 
			this.waitTillBuildCompleted = false; 
			this.printLogOnFailure = false;
		}
		
		if (setBuildVariables != null) {
			this.setBuildVariables = true;
			this.preserveVariables = "true".equalsIgnoreCase(setBuildVariables.getString("preserveVariables"));
			this.variables = setBuildVariables.getString("variables");
		} else {
			this.setBuildVariables = false;
			this.preserveVariables = false;
			this.variables = null;
		}

		if (enableReleaseDeployable != null) { 
			this.enableReleaseDeployable = true; 
			this.deployableId = enableReleaseDeployable.getString("deployableId");
		} else { 
			this.enableReleaseDeployable = false; 
			this.deployableId = null;
		}
		
		this.applicationId = applicationId;
		this.releaseNumber = releaseNumber;
		this.buildNumber = buildNumber;
	}

	public boolean getWaitTillBuildCompleted() {
		return waitTillBuildCompleted;
	}

	public boolean getPrintLogOnFailure() {
		return printLogOnFailure;
	}
	
	public boolean getSetBuildVariables() {
		return setBuildVariables;
	}
	
	public boolean getPreserveVariables() {
		return preserveVariables;
	}
	
	public String getVariables() {
		return variables;
	}

	public boolean getEnableReleaseDeployable() {
		return enableReleaseDeployable;
	}
	
	public String getDeployableId() {
		return deployableId;
	}
	
	public String getFeedName() {
		return applicationId;
	}

	public String getReleaseNumber() {
		return releaseNumber;
	}
	
	public String getBuildNumber() {
		return buildNumber;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		return ProGetHelper.triggerBuild(build, listener, this);
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	@Extension // This indicates to Jenkins that this is an implementation of an extension point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		public DescriptorImpl() {
			super(TriggerBuildPublisher.class);
		}

		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			// Indicates that this builder can be used with all kinds of project types
			return true;
		}

//		@Override
//		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
//			return req.bindJSON(TriggerBuildPostBuildStep.class, formData);
//		}

		@Override
		public String getDisplayName() {
			return "Trigger BuildMaster Build";
		}
		
		// TODO jelly expandableTextbox does not support form validation currently so this does nothing: 
		// https://github.com/jenkinsci/jenkins/blob/master/core/src/main/resources/lib/form/expandableTextbox.jelly
		public FormValidation doCheckVariables(@QueryParameter String value) {
			try {
				ProGetHelper.getVariablesList(value);
			} catch (Exception e) {
                return FormValidation.error(e.getMessage());
            }
            
            return FormValidation.ok();
		}
		
		public String getDefaultBuildNumber() {
			return ProGetHelper.DEFAULT_BUILD_NUMBER;
		}
	}
}
