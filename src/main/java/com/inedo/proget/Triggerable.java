package com.inedo.proget;

public interface Triggerable {
	public boolean getWaitTillBuildCompleted();
	public boolean getPrintLogOnFailure();
	public boolean getSetBuildVariables();
	public boolean getPreserveVariables();
	public String getVariables();
	public boolean getEnableReleaseDeployable();
	public String getDeployableId();
	public String getFeedName();
	public String getReleaseNumber();
	public String getBuildNumber();

}
