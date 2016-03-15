package com.inedo.proget.jenkins;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.inedo.proget.api.ProGet;
import java.io.File;
import java.io.IOException;

/**
 * Uploads a universal package from ProGet.
 * 
 * File related code borrowed from https://github.com/jenkinsci/jenkins/blob/master/core/src/main/java/hudson/tasks/ArtifactArchiver.java
 *
 * @author Andrew Sumner
 */
public class UploadPackageBuilder extends Builder {
	private final String feedName;
	private final String packageName;
	/**
     * Comma- or space-separated list of patterns of files/directories to be archived.
     */
	private final String artifacts;
	private String excludes = "";
	private final String metadata;
	/**
     * Possibly null 'excludes' pattern as in Ant.
     */
	
	// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
	@DataBoundConstructor
	public UploadPackageBuilder(String feedName, String packageName, String artifacts, String excludes, String metadata) {
		this.feedName = feedName;
		this.packageName = packageName;
		this.artifacts = artifacts;
		this.excludes = excludes;
		this.metadata = metadata;
	}

	
	public String getFeedName() {
		return feedName;
	}
	
	public String getPackageName() {
		return packageName;
	}
	
	public String getArtifacts() {
		return artifacts;
	}

	public String getMetadata() {
		return metadata;
	}
	
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		ProGetHelper helper = new ProGetHelper(build, listener);
		
		if(artifacts.length()==0) {
			helper.error("Files to package not set");
            return false;
        }
		
		helper.info("Packaging Artifacts");
        
    	boolean caseSensitive = false;
    	String includes = helper.expandVariable(this.artifacts);
    	FilePath ws = build.getWorkspace();
    	
    	File baseDir = new File(ws.getRemote());
        
        ProGet proget = new ProGet(helper);
        
		File pkg = proget.createPackage(baseDir, includes, excludes, caseSensitive);
		
		if (pkg == null) {
	    	String msg = ws.validateAntFileMask(includes, FilePath.VALIDATE_ANT_FILE_MASK_BOUND);
	    	if(msg != null) {
	        	helper.error(msg);
	        	return false;
	        }
	    	
	    	helper.error("No files found matching Files to package setting");
	    	return false;
		}    	
		
		proget.uploadPackage(feedName, pkg);
        
        return true;
	}
	
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		public DescriptorImpl() {
			super(UploadPackageBuilder.class);
		}
		
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			// Indicates that this builder can be used with all kinds of project types
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Upload ProGet Package";
		}
		
		/**
         * Performs on-the-fly validation of the file mask wildcard, when the artifacts
         * textbox or the caseSensitive checkbox are modified
         */
        public FormValidation doCheckArtifacts(@AncestorInPath AbstractProject<?, ?> project,
                @QueryParameter String value,
                @QueryParameter(value = "caseSensitive") String caseSensitive)
                throws IOException {
            if (project == null) {
                return FormValidation.ok();
            }
            
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }
	}
}
