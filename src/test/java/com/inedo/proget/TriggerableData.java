package com.inedo.proget;

import com.inedo.proget.Triggerable;

/**
 * Data to inject into TriggerBuildHelper for testing
 * 
 * @author Andrew Sumner
 */
public class TriggerableData implements Triggerable {
	public boolean waitTillBuildCompleted = false;
	public boolean printLogOnFailure = false;
	public boolean setBuildVariables = false;
	public boolean preserveVariables = false;
	public String variables = null;
	public boolean enableReleaseDeployable = false;
	public String deployableId = null;
	public String applicationId;
	public String releaseNumber;
	public String buildNumber;

	public TriggerableData(String applicationId, String releaseNumber, String buildNumber) {
		this.applicationId = applicationId;
		this.releaseNumber = releaseNumber;
		this.buildNumber = buildNumber;
	}
	
	// Getters
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
	
	// Setters
	public TriggerableData setWaitTillBuildCompleted(boolean value) {
		waitTillBuildCompleted = value;
		return this;
	}

	public TriggerableData setPrintLogOnFailure(boolean value) {
		printLogOnFailure = value;
		return this;
	}
	
	public TriggerableData setSetBuildVariables(boolean value) {
		setBuildVariables = value;
		return this;
	}
	
	public TriggerableData setPreserveVariables(boolean value) {
		preserveVariables = value;
		return this;
	}
	
	public TriggerableData setVariables(String value) {
		variables = value;
		return this;
	}

	public TriggerableData setEnableReleaseDeployable(boolean value) {
		enableReleaseDeployable = value;
		return this;
	}
	
	public TriggerableData setDeployableId(String value) {
		deployableId = value;
		return this;
	}
	
	public TriggerableData setApplicationId(String value) {
		applicationId = value;
		return this;
	}

	public TriggerableData setReleaseNumber(String value) {
		releaseNumber = value;
		return this;
	}
	
	public TriggerableData setBuildNumber(String value) {
		buildNumber = value;
		return this;
	}
}