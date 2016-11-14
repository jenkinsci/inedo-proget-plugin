package com.inedo.proget.jenkins;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.security.MasterToSlaveCallable;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.inedo.proget.api.ProGetApi;
import com.inedo.proget.api.ProGetConfig;
import com.inedo.proget.api.ProGetPackager;
import com.inedo.proget.api.ProGetPackager.ZipItem;
import com.inedo.proget.domain.Feed;
import com.inedo.proget.domain.PackageMetadata;
import com.inedo.proget.domain.ProGetPackage;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Scanner;
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
		JenkinsHelper helper = new JenkinsHelper(build, listener);
		
		if (!GlobalConfig.isProGetRequiredFieldsConfigured(true)) {
            helper.getLogWriter().error("Please configure ProGet Plugin global settings");
            return false;
        }
		
		if(artifacts.length()==0) {
			helper.getLogWriter().error("Files to package not set");
            return false;
        }
		
		PackageMetadata metadata = buildMetadata(helper);
        if (metadata == null) {
            helper.getLogWriter().error("Metadata is incorrectly formatted");
            return false;
        }

        return launcher.getChannel().call(new PutPackage(
                listener, 
                GlobalConfig.getProGetConfig(),
                build.getWorkspace(),
                new PutDetails(this, helper),
                metadata));
    }
	
	// Define what should be run on the slave for this build
    private static class PutPackage extends MasterToSlaveCallable<Boolean, IOException> {
        private final BuildListener listener;
        private ProGetConfig config;
        private FilePath workspace;
        private PutDetails settings;
        private PackageMetadata metadata;
        
        public PutPackage(final BuildListener listener, ProGetConfig config, FilePath workspace, PutDetails settings, PackageMetadata metadata) {
            this.listener = listener;
            this.config = config;
            this.workspace = workspace;
            this.settings = settings;
            this.metadata = metadata;
        }

        public Boolean call() throws IOException {
            JenkinsLogWriter logWriter = new JenkinsTaskLogWriter(listener);
            
            logWriter.info("Packaging Artifacts");
                        
            File baseDir = new File(workspace.getRemote());
            
            ProGetPackager packageUtils = new ProGetPackager();
            
            List<ZipItem> files = packageUtils.getFileList(baseDir, settings.include, settings.exclude, settings.defaultExcludes, settings.caseSensitive);
              
            if (files.isEmpty()) {
                String msg;
                
                try {
                    msg = workspace.validateAntFileMask(settings.include, FilePath.VALIDATE_ANT_FILE_MASK_BOUND);
                } catch (InterruptedException e) {
                    throw new IOException("Invalid ANT file mask: " + settings.include, e);
                }
                
                if(msg != null) {
                    logWriter.error(msg);
                    return false;
                }
                
                logWriter.error("No files found matching Files to package setting '" + settings.include + "'");
                return false;
            } 
            
            File pkg = packageUtils.createPackage(baseDir, files, metadata);
            
            new ProGetApi(config, logWriter).uploadPackage(settings.feedName, pkg);
            
            return true;
        }

        private static final long serialVersionUID = 1L;
    }
	
	public PackageMetadata buildMetadata(JenkinsHelper helper) {
		PackageMetadata metadata = new PackageMetadata();

		metadata.group = helper.expandVariable(getGroupName());
		metadata.packageName = helper.expandVariable(getPackageName());
		metadata.version = helper.expandVariable(getVersion());
		metadata.title = getTitle();
		metadata.description = getDescription();
		metadata.icon = getIcon();
				
		if (getMetadata() != null) {
			try (Scanner scanner = new Scanner(getMetadata())) {
				while (scanner.hasNextLine()){
					String line = scanner.nextLine().trim();
					
					if (line.isEmpty()) {
						continue;
					}
					
					int pos = line.indexOf("=");
					
					if (pos > 0) {
						String name = line.substring(0, pos).trim();
					    String value = line.substring(pos + 1).trim().replace("\\", "\\\\");
					    
					    metadata.extendedAttributes.put(name, helper.expandVariable(value));
				    } else {
				    	return null;
					}
			    } 
			}
		}
		
		if (getDependencies() != null) {
			try (Scanner scanner = new Scanner(getDependencies())) {
				while (scanner.hasNextLine()){
					String dependency = scanner.nextLine().trim();
					
					if (!dependency.isEmpty()) {
					    metadata.dependencies.add(dependency);
					}
			    } 
			}
		}
		
		return metadata;
	}
	    
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		private ProGetApi proget = null;
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
    		
			if (!GlobalConfig.isProGetRequiredFieldsConfigured(true)) {
				connectionError = "Please configure ProGet Plugin global settings";
				isProGetAvailable = false;
				return false;
			}
			
			proget = new ProGetApi(new JenkinsConsoleLogWriter());

			try {
            	proget.canConnect();
			} catch (Exception ex) {
            	connectionError = "Unable to connect to Proget, please check the global settings: " + ex.getClass().getName() + " - " + ex.getMessage();
            	isProGetAvailable = false;
            	return false;
            }   

			if (!GlobalConfig.isProGetApiKeyFieldConfigured()) {
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
        	int countDots = 0;
        	
        	int pos = value.indexOf(".");
        	while (pos > -1) {
        		countDots ++;
        		pos = value.indexOf(".", pos + 1);
        	}
        	
        	if (countDots != 2) {
       			return FormValidation.error("Version must be in a three-part dot format eg 0.0.0");
        	}
        	
            return FormValidation.ok();
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
