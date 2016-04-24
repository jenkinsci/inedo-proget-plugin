package com.inedo.proget.jenkins;

import hudson.Launcher;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.inedo.proget.api.ProGet;
import com.inedo.proget.api.ProGetConfig;
import com.inedo.proget.api.ProGetPackageUtils;
import com.inedo.proget.domain.Feed;
import com.inedo.proget.domain.ProGetPackage;
import com.inedo.proget.domain.Version;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Downloads a universal package from ProGet.
 *
 * @author Andrew Sumner
 */
public class DownloadPackageBuilder extends Builder {
	private final String feedName;
	private final String groupName;
	private final String packageName;
	private final String version;
	private final String downloadFolder;
	private final String downloadFormat;
	
	// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
	@DataBoundConstructor
	public DownloadPackageBuilder(String feedName, String groupName, String packageName, String version, String downloadFolder, String downloadFormat) {
		this.feedName = feedName;
		this.groupName = groupName;
		this.packageName = packageName;
		this.version = version;
		this.downloadFolder = downloadFolder;
		this.downloadFormat = downloadFormat;
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
	
	public String getDownloadFolder() {
		return downloadFolder;
	}
	
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException {
		ProGetHelper helper = new ProGetHelper(build, listener);
		
		if (!helper.validateProGetConfig()) {
			return false;
		}
		
		ProGet proget = new ProGet(helper);
		
		String downloadTo = helper.expandVariable(downloadFolder);
		helper.info("Download to " + new File(downloadTo).getAbsolutePath());
		
		try {
			DownloadFormat format = DownloadFormat.valueOf(downloadFormat);
			File downloaded = proget.downloadPackage(feedName, groupName, packageName, version, downloadTo, format);
					
			if (format == DownloadFormat.EXTRACT_CONTENT) {
				helper.info("Unpack " + downloaded.getName());
				ProGetPackageUtils.unpackContent(downloaded);
				downloaded.delete();
			}
		} catch (IOException e) {
			helper.info("Error: " + e.getMessage());
			return false;
		}
		
		return true;
	}

	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		private ProGet proget = null;
		private String connectionError = "";
		private Boolean isProGetAvailable = null;
		
		public DescriptorImpl() {
			super(DownloadPackageBuilder.class);
		}
		
		public String defaultFolder() {
			return "${WORKSPACE}";
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
    	
    	public ListBoxModel doFillGroupNameItems(@QueryParameter String feedName) throws IOException {
        	if (!getIsProGetAvailable()) {
        		return null;
        	}
        	
        	Set<String> set = new TreeSet<String>();
        	ListBoxModel items = new ListBoxModel();
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
    	
    	public ListBoxModel doFillPackageNameItems(@QueryParameter String feedName, @QueryParameter String groupName) throws IOException {
        	if (!getIsProGetAvailable()) {
        		return null;
        	}
        	
        	Set<String> set = new TreeSet<String>();
        	ListBoxModel items = new ListBoxModel();
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
    	
    	public ListBoxModel doFillVersionItems(@QueryParameter String feedName, @QueryParameter String groupName, @QueryParameter String packageName) throws IOException {
        	if (!getIsProGetAvailable()) {
        		return null;
        	}
        	
        	Set<String> set = new TreeSet<String>();
        	ListBoxModel items = new ListBoxModel();
        	Feed feed = proget.getFeed(feedName);
    		Version[] versions = proget.getPackageVersions(feed.Feed_Id, groupName, packageName);
        	
    		
    		for (Version version : versions) {
        		set.add(version.Version_Text);
			}
        	
    		items.add("");
    		items.add("Latest");
        	for (String value : set) {
        		items.add(value);
			}
        	
            return items;
        }
    	
    	public ListBoxModel doFillDownloadFormatItems() throws IOException {
        	ListBoxModel items = new ListBoxModel();
        	
        	for (DownloadFormat format : DownloadFormat.values()) {
        		items.add(format.getDisplay(), format.getFormat());
			}
        	
            return items;
        }
	}
	
	public enum DownloadFormat {
		PACKAGE("pkg", "Package"),
		EXTRACT_CONTENT("unpack", "Unpack Content"),
		CONTENT_AS_ZIP("zip", "Content as ZIP"), 
		CONTENT_AS_TGZ("tgz", "Content as TGZ");
		
		private final String format;
		private final String display;
		
		private DownloadFormat(String format, String display) {
			this.format = format;
			this.display = display;
		}
		
		public String getFormat() {
			return format;
		}
		
		public String getDisplay() {
			return display;
		}
		
		public static DownloadFormat fromFormat(String format) {
			for (DownloadFormat search : DownloadFormat.values()) {
				if (search.getFormat().equals(format)) {
					return search;
				}
			}
			
			throw new IllegalArgumentException("Unknown format " + format);
		}
	}
}
