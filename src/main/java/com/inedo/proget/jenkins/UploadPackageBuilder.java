package com.inedo.proget.jenkins;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.inedo.proget.api.ProGet;
import com.inedo.proget.api.ProGetConfig;
import com.inedo.proget.api.ProGetPackageUtils;
import com.inedo.proget.domain.Feed;
import com.inedo.proget.domain.PackageMetadata;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;

/**
 * Uploads a universal package from ProGet.
 * 
 * File related code borrowed from https://github.com/jenkinsci/jenkins/blob/master/core/src/main/java/hudson/tasks/ArtifactArchiver.java
 *
 * @author Andrew Sumner
 */
public class UploadPackageBuilder extends Builder {
	private final String title;
	private final String description;
	private final String feedName;
	private final String groupName;
	private final String packageName;
	private final String version;
	private final String metadata;
	private final String artifacts;
	private String excludes;
	private boolean defaultExcludes;
	private boolean caseSensitive;
	
	// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
	@DataBoundConstructor
	public UploadPackageBuilder(String title, String description, String feedName, String groupName, String packageName, String version, String metadata, String artifacts) {
		this.title = title;
		this.description = description;
		this.feedName = feedName;
		this.groupName = groupName;
		this.packageName = packageName;
		this.version = version;
		this.metadata = metadata;
		this.artifacts = artifacts;
	}

	@DataBoundSetter public final void setExcludes(String excludes) {
        this.excludes = excludes;
    }
	
	@DataBoundSetter public final void setDefaultExcludes(boolean defaultExcludes) {
        this.defaultExcludes = defaultExcludes;
    }
	
    @DataBoundSetter public final void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    
	public String getTitle() {
		return title;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getFeedName() {
		return feedName;
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public String getPackageName() {
		return packageName;
	}
	
	public String getVersion() {
		return version;
	}
	
	public String getMetadata() {
		return metadata;
	}
	
	public String getArtifacts() {
		return artifacts;
	}
	
	public String getExcludes() {
		return excludes;
	}
	
	public boolean isDefaultExcludes() {
        return defaultExcludes;
    }
	
	public boolean isCaseSensitive() {
        return caseSensitive;
    }

	
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		ProGetHelper helper = new ProGetHelper(build, listener);
		
		if(artifacts.length()==0) {
			helper.error("Files to package not set");
            return false;
        }
		
		helper.info("Packaging Artifacts");
        
    	String includes = helper.expandVariable(this.artifacts);
    	FilePath ws = build.getWorkspace();
    	
    	//TODO allow custom base directory rather than default to workspace?
    	File baseDir = new File(ws.getRemote());
        
    	ProGetPackageUtils packageUtils = new ProGetPackageUtils();
    	
    	List<String> files = packageUtils.getFileList(baseDir, this);
          
		if (files.isEmpty()) {
	    	String msg = ws.validateAntFileMask(includes, FilePath.VALIDATE_ANT_FILE_MASK_BOUND);
	    	if(msg != null) {
	        	helper.error(msg);
	        	return false;
	        }
	    	
	    	helper.error("No files found matching Files to package setting");
	    	return false;
		} 
		
		PackageMetadata metadata = helper.getMetadata(this);
		if (metadata == null) {
			helper.error("Metadata is incorrectly formatted");
			return false;
		}
		
    	File pkg = packageUtils.createPackage(baseDir, files, metadata);
		
		new ProGet(helper).uploadPackage(feedName, pkg);
        
        return true;
	}
	
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		private ProGet proget = null;
		private String connectionError = "";
		private Boolean isProGetAvailable = null;
		
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
		
		public boolean isConnectionError() {
			getIsProGetAvailable();
			
			return !connectionError.isEmpty();
		}
		
		public String getConnectionError() {
			getIsProGetAvailable();
			
    		return connectionError;
    	}
		
		/**
    	 * Check if can connect to ProGet - if not prevent any more calls
    	 */
    	public boolean getIsProGetAvailable() {
    		if (isProGetAvailable == null) {
    			ProGetConfig config = ProGetHelper.getProGetConfig();
    			proget = new ProGet(new ProGetHelper());
        		
            	if (config.apiKey == null || config.apiKey.isEmpty()) {
            		isProGetAvailable = false;
            		connectionError = "";
            	} else {
            		try {
                    	proget.checkConnection();
                    	isProGetAvailable = true;
                		connectionError = "";
                    } catch (Exception ex) {
                    	isProGetAvailable = false;
                    	connectionError = ex.getClass().getName() + ": " + ex.getMessage();
                    	
                    	System.err.println(connectionError);
                    }   
            	}
    		}
        	
        	return isProGetAvailable;
    	}
    	
    	public ListBoxModel doFillFeedNameItems() throws IOException {
        	ListBoxModel items = new ListBoxModel();
        	
        	if (!getIsProGetAvailable()) {
        		items.add("", "");
        		
        		return items;
        	}
        	
        	Feed[] feeds = proget.getFeeds();
            
        	for (Feed feed : feeds) {
        		items.add(feed.Feed_Name);
			}
        	
            return items;
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

        public FormValidation doCheckTitle(@QueryParameter String value) throws IOException, ServletException {
        	return checkFieldLength(value, false);
        }

        public FormValidation doCheckFeedName(@QueryParameter String value) throws IOException, ServletException {
        	return checkFieldLength(value, true);
        }
        
        public FormValidation doCheckGroupName(@QueryParameter String value) throws IOException, ServletException {
        	return checkFieldLength(value, true);
        }
        
        public FormValidation doCheckPackageName(@QueryParameter String value) throws IOException, ServletException {
        	return checkFieldLength(value, true);
        }

        public FormValidation doCheckVersion(@QueryParameter String value) throws IOException, ServletException {
        	return checkFieldLength(value, true);
        }

        private FormValidation checkFieldLength(String value, Boolean required) {
        	if (required && value.length() == 0)
                return FormValidation.error("This setting is required");
        
            if (value.length() > 50)
                return FormValidation.error("No more than 50 characters allowed");

            return FormValidation.ok();
		}

	}
}
