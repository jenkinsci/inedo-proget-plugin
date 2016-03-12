package com.inedo.proget.jenkins;

import hudson.Launcher;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import com.inedo.http.LogWriter;
import com.inedo.proget.api.ProGet;
import com.inedo.proget.api.ProGetPackageUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Downloads a universal package from ProGet.
 *
 * @author Andrew Sumner
 */
public class DownloadPackageBuilder extends Builder implements LogWriter {
	private static final String LOG_PREFIX = "[ProGet] "; 
	private PrintStream logger = null;
	
	private final String feedName;
	private final String groupName;
	private final String packageName;
	private final String version;
	private final String downloadFolder;
	private final boolean unpack;

	// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
	@DataBoundConstructor
	public DownloadPackageBuilder(String feedName, String groupName, String packageName, String version, String downloadFolder, boolean unpack) {
		this.feedName = feedName;
		this.groupName = groupName;
		this.packageName = packageName;
		this.version = version;
		this.downloadFolder = downloadFolder;
		this.unpack = unpack;
	}

	
	public String getFeedName() {
		return feedName;
	}
	
	public String getPackageName() {
		return packageName;
	}
	
	public String getDownloadFolder() {
		return downloadFolder;
	}

	public boolean getUnpack() {
		return unpack;
	}
	
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException {
		logger = listener.getLogger();
		
		if (!ProGetHelper.validateProGetConfig()) {
			writeLogMessage("Please configure ProGet Plugin global settings");
			return false;
		}
		
		ProGet proget = new ProGet(ProGetHelper.getProGetConfig(listener.getLogger()), this);
		
		String downloadTo = ProGetHelper.expandVariable(build, listener, downloadFolder);
		writeLogMessage("Download to " + downloadTo);
		
		
		File downloaded = proget.downloadPackage(feedName, groupName, packageName, version, downloadTo);
		
		if (unpack) {
			writeLogMessage("Unpack " + downloaded.getName());
			ProGetPackageUtils.unpackContent(downloaded);
			downloaded.delete();
		}
		
		return true;
	}

	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		public DescriptorImpl() {
			super(DownloadPackageBuilder.class);
		}
		
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			// Indicates that this builder can be used with all kinds of project types
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Download ProGet Package";
		}
	}

	@Override
	public void writeLogMessage(String message) {
		logger.println(LOG_PREFIX + message);
	}
}
