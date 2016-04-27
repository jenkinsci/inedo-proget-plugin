package com.inedo.proget.jenkins;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.ComboBoxModel;
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
import com.inedo.proget.api.ProGetPackageUtils;
import com.inedo.proget.api.ProGetPackageUtils.ZipItem;
import com.inedo.proget.domain.Feed;
import com.inedo.proget.domain.PackageMetadata;
import com.inedo.proget.domain.ProGetPackage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;

/**
 * Uploads a universal package from ProGet.
 * 
 * File related code borrowed from https://github.com/jenkinsci/jenkins/blob/master/core/src/main/java/hudson/tasks/ArtifactArchiver.java
 *
 * @author Andrew Sumner
 */
public class UploadPackageBuilder extends Builder {
	private final String feedName;
	private final String groupName;
	private final String packageName;
	private final String version;
	private final String artifacts;
	private String excludes;
	private boolean defaultExcludes;
	private boolean caseSensitive;
	private String title;
	private String description;
	private String icon;
	private String metadata;
	private String dependencies;
	
	// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
	@DataBoundConstructor
	public UploadPackageBuilder(String feedName, String groupName, String packageName, String version, String artifacts) {
		this.feedName = feedName;
		this.groupName = groupName;
		this.packageName = packageName;
		this.version = version;
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
    
    @DataBoundSetter public void setTitle(String title) {
		this.title = title;
	}

    @DataBoundSetter public void setDescription(String description) {
		this.description = description;
	}

    @DataBoundSetter public void setIcon(String icon) {
		this.icon = icon;
	}

    @DataBoundSetter public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

    @DataBoundSetter public void setDependencies(String dependencies) {
		this.dependencies = dependencies;
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

	public String getTitle() {
		return title;
	}
	
	public String getDescription() {
		return description;
	}

	public String getIcon() {
		return icon;
	}

	public String getMetadata() {
		return metadata;
	}
	
	public String getDependencies() {
		return dependencies;
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
    	
    	//base directory is workspace
    	File baseDir = new File(ws.getRemote());
        
    	ProGetPackageUtils packageUtils = new ProGetPackageUtils();
    	
    	List<ZipItem> files = packageUtils.getFileList(baseDir, this);
          
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
		private String connectionWarning = "";
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
			return "ProGet Package Upload";
		}
		
		public boolean isConnectionError() {
			getIsProGetAvailable();
			
			return !connectionError.isEmpty();
		}
		
		public String getConnectionError() {
    		return connectionError;
    	}
		
		public boolean isConnectionWarning() {
			getIsProGetAvailable();
			
			return !connectionWarning.isEmpty();
		}
		
		public String getConnectionWarning() {
			return connectionWarning;
    	}
		
		/**
    	 * Check if can connect to ProGet - if not prevent any more calls
    	 */
    	public boolean getIsProGetAvailable() {
    		if (isProGetAvailable != null) {
    			return isProGetAvailable;
    		}
    		
			ProGetHelper helper = new ProGetHelper();
			
			if (!helper.isProGetRequiredFieldsConfigured(true)) {
				connectionError = "Please configure ProGet Plugin global settings";
				isProGetAvailable = false;
				return false;
			}
			
			proget = new ProGet(null);

			try {
            	proget.canConnect();
			} catch (Exception ex) {
            	connectionError = "Unable to connect to Proget, please check the global settings: " + ex.getClass().getName() + " - " + ex.getMessage();
            	isProGetAvailable = false;
            	return false;
            }   

			if (!helper.isProGetApiKeyFieldConfigured()) {
				connectionWarning = "The ApiKey has not been configured in global settings, some features have been disabled.";
				isProGetAvailable = false;
			} else {
	    		connectionError = "";
	        	isProGetAvailable = true;
			}

        	return isProGetAvailable;
    	}
    	
    	public ListBoxModel doFillFeedNameItems() throws IOException {
        	if (!getIsProGetAvailable()) {
        		return null;
        	}
        	
        	Set<String> set = new TreeSet<String>();
        	ListBoxModel items = new ListBoxModel();
        	Feed[] feeds = proget.getFeeds();
            
        	for (Feed feed : feeds) {
        		set.add(feed.Feed_Name);
			}
        	
        	for (String value : set) {
        		items.add(value);
			}
        	
            return items;
        }
    	
    	public ComboBoxModel doFillGroupNameItems(@QueryParameter String feedName) throws IOException {
        	if (!getIsProGetAvailable()) {
        		return null;
        	}
        	
        	Set<String> set = new TreeSet<String>();
        	ComboBoxModel items = new ComboBoxModel();
        	Feed feed = proget.getFeed(feedName);
    		ProGetPackage[] packages = proget.getPackages(feed.Feed_Id);
    		
        	for (ProGetPackage pkg : packages) {
        		set.add(pkg.Group_Name);
			}
        	
        	items.add("");
        	for (String value : set) {
        		items.add(value);
			}
        	
            return items;
        }
    	
    	public ComboBoxModel doFillPackageNameItems(@QueryParameter String feedName, @QueryParameter String groupName) throws IOException {
        	if (!getIsProGetAvailable()) {
        		return null;
        	}
        	
        	Set<String> set = new TreeSet<String>();
        	ComboBoxModel items = new ComboBoxModel();
        	Feed feed = proget.getFeed(feedName);
    		ProGetPackage[] packages = proget.getPackages(feed.Feed_Id);
    		
        	for (ProGetPackage pkg : packages) {
        		if (pkg.Group_Name.equals(groupName)) {
        			set.add(pkg.Package_Name);
        		}
			}
        	
        	items.add("");
        	for (String value : set) {
        		items.add(value);
			}
        	
            return items;
        }
    	
    	private static final boolean REQUIRED = true;
    	private static final boolean OPTIONAL = false;
    	
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

        public FormValidation doCheckFeedName(@QueryParameter String value) throws IOException, ServletException {
        	return checkFieldLength(value, REQUIRED);
        }
        
        public FormValidation doCheckGroupName(@QueryParameter String value) throws IOException, ServletException {
        	return checkFieldLength(value, REQUIRED);
        }
        
        public FormValidation doCheckPackageName(@QueryParameter String value) throws IOException, ServletException {
        	return checkFieldLength(value, REQUIRED);
        }

        public FormValidation doCheckVersion(@QueryParameter String value) throws IOException, ServletException {
        	return checkFieldLength(value, REQUIRED);
        }

        public FormValidation doCheckTitle(@QueryParameter String value) throws IOException, ServletException {
        	return checkFieldLength(value, OPTIONAL);
        }

        private FormValidation checkFieldLength(String value, Boolean required) {
        	if (required && value.length() == 0)
                return FormValidation.error("This setting is required");
        
            if (value.length() > 50)
                return FormValidation.error("No more than 50 characters allowed");

            return FormValidation.ok();
		}

//TODO Raise with developer list, is it possible to get workspace from configuration page to test getFileList()?
//        /**
//         *  ProGet connection test
//         */
//		public FormValidation doTestFindFiles(
//			@QueryParameter("artifacts") final String artifacts,
//			@QueryParameter("excludes") final String excludes,
//			@QueryParameter("defaultExcludes") final String defaultExcludes,
//			@QueryParameter("caseSensitive") final String caseSensitive) throws IOException, ServletException {
//	
//			
//			
//	    	FilePath ws = build.getWorkspace();
//	    	
//	    	//base directory is workspace
//	    	File baseDir = new File(ws.getRemote());
//	        
//	    	ProGetPackageUtils packageUtils = new ProGetPackageUtils();
//	    	
//	    	List<String> files = packageUtils.getFileList(baseDir, this);
//	          
//			if (files.isEmpty()) {
//		    	String msg = ws.validateAntFileMask(includes, FilePath.VALIDATE_ANT_FILE_MASK_BOUND);
//		    	if(msg != null) {
//		        	helper.error(msg);
//		        	return false;
//		        }
//		    	
//		    	helper.error("No files found matching Files to package setting");
//		    	return false;
//			} 
//
//			try {
//				pkg.getFileList(baseFolder, settings)
//			} catch (Exception ex) {
//            	return FormValidation.error("Failed. Please check the configuration: " + ex.getClass().getName() + " - " + ex.getMessage());
//			}
//			
//			return FormValidation.ok("Success. Connection with ProGet verified.");			
//		}
	}
}
