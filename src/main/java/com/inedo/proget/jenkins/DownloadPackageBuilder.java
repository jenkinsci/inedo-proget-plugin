package com.inedo.proget.jenkins;

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.inedo.proget.api.ProGetApi;
import com.inedo.proget.api.ProGetConfig;
import com.inedo.proget.api.ProGetPackager;
import com.inedo.proget.domain.Feed;
import com.inedo.proget.domain.PackageVersion;
import com.inedo.proget.domain.ProGetPackage;
import com.inedo.proget.jenkins.utils.JenkinsConsoleLogWriter;
import com.inedo.proget.jenkins.utils.JenkinsHelper;
import com.inedo.proget.jenkins.utils.JenkinsLogWriter;
import com.inedo.proget.jenkins.utils.JenkinsTaskLogWriter;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ComboBoxModel;
import hudson.util.ListBoxModel;
import jenkins.security.MasterToSlaveCallable;
import jenkins.tasks.SimpleBuildStep;

/**
 * Downloads a universal package from ProGet.
 * 
 * See https://github.com/jenkinsci/pipeline-plugin/blob/master/DEVGUIDE.md#user-content-build-wrappers-1 for tips on 
 * Jenkins pipeline support 
 *
 * @author Andrew Sumner
 */
public class DownloadPackageBuilder extends Builder implements SimpleBuildStep {
    private final String feedName;
    private final String groupName;
    private final String packageName;
    private final String version;
    private final String downloadFormat;
    private final String downloadFolder;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public DownloadPackageBuilder(String feedName, String groupName, String packageName, String version, String downloadFormat, String downloadFolder) {
        this.feedName = feedName;
        this.groupName = groupName;
        this.packageName = packageName;
        this.version = version;
        this.downloadFormat = downloadFormat;
        this.downloadFolder = downloadFolder;
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

    public String getDownloadFormat() {
        return downloadFormat;
    }

    public String getDownloadFolder() {
        return downloadFolder;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        JenkinsHelper helper = new JenkinsHelper(run, listener);
        
        if (!GlobalConfig.isRequiredFieldsConfigured()) {
            throw new AbortException("Please configure ProGet Plugin global settings");
        }

        ProGetConfig config = GlobalConfig.getProGetConfig();
        
        String downloadTo = helper.expandVariable(downloadFolder);
        helper.getLogWriter().info("Download package to " + new File(downloadTo).getAbsolutePath());

        String downloaded = launcher.getChannel().call(new GetPackage(
                listener,
                config,
                helper.expandVariable(feedName),
                helper.expandVariable(groupName),
                helper.expandVariable(packageName),
                helper.expandVariable(version),
                downloadFormat,
                downloadTo));

        if (!downloaded.isEmpty()) {
            helper.injectEnvrionmentVariable("PROGET_FILE", downloaded);
        }
    }

    // Define what should be run on the slave for this build
    private static class GetPackage extends MasterToSlaveCallable<String, IOException> {
        private final TaskListener listener;
        private ProGetConfig config;
        private final String feedName;
        private final String groupName;
        private final String packageName;
        private final String version;
        private final String downloadFormat;
        private final String downloadFolder;

        public GetPackage(final TaskListener listener, ProGetConfig config, String feedName, String groupName, String packageName, String version, String downloadFormat,
                String downloadFolder) {
            this.listener = listener;
            this.config = config;
            this.feedName = feedName;
            this.groupName = groupName;
            this.packageName = packageName;
            this.version = version;
            this.downloadFormat = downloadFormat;
            this.downloadFolder = downloadFolder;
        }

        public String call() throws IOException {
            JenkinsLogWriter logWriter = new JenkinsTaskLogWriter(listener);
            
            ProGetApi proget = new ProGetApi(config, logWriter);
            DownloadFormat format = DownloadFormat.fromFormat(downloadFormat);
            File downloaded = proget.downloadPackage(feedName, groupName, packageName, version, downloadFolder, format);
                    
            if (format == DownloadFormat.EXTRACT_CONTENT) {
                logWriter.info("Unpack " + downloaded.getName());
                ProGetPackager.unpackContent(downloaded);
                downloaded.delete();
            } else {
                return downloaded.getName();
            }
            
            return "";
        }

        private static final long serialVersionUID = 1L;
    }

    @Symbol("downloadProgetPackage")
    @Extension
    // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private ProGetApi proget = null;
        private String connectionError = "";
        private String connectionWarning = "";
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
            return "ProGet Package Download";
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

            // if (!GlobalConfig.isRequiredFieldsConfigured(false)) {
            // connectionError = "Please configure ProGet Plugin global settings";
            // isProGetAvailable = false;
            // return false;
            // }

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

            SortedSet<String> set = new TreeSet<>();
            ListBoxModel items = new ListBoxModel();
            Feed[] feeds = proget.getFeeds();

            for (Feed feed : feeds) {
                set.add(feed.Feed_Name);
            }

            items.add("");
            for (String value : set) {
                items.add(value);
            }
            
            return items;
        }

        public ComboBoxModel doFillGroupNameItems(@QueryParameter String feedName) throws IOException {
            if (!getIsProGetAvailable()) {
                return null;
            }

            SortedSet<String> set = new TreeSet<>();
            ComboBoxModel items = new ComboBoxModel();

            if (feedName != null && !feedName.isEmpty()) {
                Feed feed = proget.getFeed(feedName);
                ProGetPackage[] packages = proget.getPackages(feed.Feed_Id);

                for (ProGetPackage pkg : packages) {
                    set.add(pkg.Group_Name);
                }
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

            SortedSet<String> set = new TreeSet<>();
            ComboBoxModel items = new ComboBoxModel();

            if (feedName != null && !feedName.isEmpty()) {
                Feed feed = proget.getFeed(feedName);
                ProGetPackage[] packages = proget.getPackages(feed.Feed_Id, groupName);

                for (ProGetPackage pkg : packages) {
                    set.add(pkg.Package_Name);
                }
            }

            items.add("");
            for (String value : set) {
                items.add(value);
            }

            return items;
        }

        public ComboBoxModel doFillVersionItems(@QueryParameter String feedName, @QueryParameter String groupName, @QueryParameter String packageName) throws IOException {
            if (!getIsProGetAvailable()) {
                return null;
            }

            SortedSet<String> set = new TreeSet<>();
            ComboBoxModel items = new ComboBoxModel();

            if (feedName != null && !feedName.isEmpty()) {
                Feed feed = proget.getFeed(feedName);
                PackageVersion[] versions = proget.getPackageVersions(feed.Feed_Id, groupName, packageName);

                for (PackageVersion version : versions) {
                    set.add(version.Version_Text);
                }
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
}
