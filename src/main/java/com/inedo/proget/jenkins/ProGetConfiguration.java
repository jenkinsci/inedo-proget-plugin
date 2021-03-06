package com.inedo.proget.jenkins;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.google.common.base.Strings;
import com.inedo.proget.api.ProGetApi;
import com.inedo.proget.api.ProGetConfig;
import com.inedo.proget.jenkins.utils.JenkinsConsoleLogWriter;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

/**
 * Has fields for global configuration
 * 
 * @author Andrew Sumner
 */
public class ProGetConfiguration extends GlobalConfiguration {

    @Extension
    public static final class DescriptorImpl extends Descriptor<GlobalConfiguration> {

        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String url;
        private String apiKey;
        private String user;
        private Secret password;
        private boolean logApiRequests;
        private boolean trustAllCertificates;
        
        public DescriptorImpl() {
            super(ProGetConfiguration.class);
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
        
            req.bindJSON(this, formData);
            save();
            return super.configure(req,formData);
        }

        @Override
        public String getDisplayName() {
            return "ProGet Plugin";
        }

        /**
         * Field setters
         */
        public void setUrl(String value) {
            url = value;
        }

        public void setApiKey(String value) {
            apiKey = value;
        }

        public void setUser(String value) {
            user = value;
        }
        
        public void setPassword(Secret value) {
            password = value;
        }
        
        public void setLogApiRequests(boolean logApiRequests) {
            this.logApiRequests = logApiRequests;
        }

        public void setTrustAllCertificates(boolean value) {
            trustAllCertificates = value;
        }
        
        /**
         * Field getters
         */
        public String getUrl() {
            return url;
        }
        
        public String getApiKey() {
            return apiKey;
        }

        public String getUser() {
            return user;
        }
        
        public Secret getPassword() {
            return password;
        }
        
        public boolean getLogApiRequests() {
            return logApiRequests;
        }

        public boolean getTrustAllCertificates() {
            return trustAllCertificates;
        }
        
        public boolean isRequiredFieldsConfigured() {
            if (url == null || url.trim().isEmpty()) {
                return false;
            }

            return true;
        }

        public boolean isUserNameConfigured() {
            if (user == null || user.trim().isEmpty()) {
                return false;
            }

            if (Strings.isNullOrEmpty(Secret.toString(password))) {
                return false;
            }

            return true;
        }

        public boolean isApiKeyConfigured() {
            if (apiKey == null || apiKey.isEmpty()) {
                return false;
            }

            return true;
        }

        /**
         * Performs on-the-fly validation of the form field 'url'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckUrl(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a URL");
            
            if (!value.startsWith("http"))
                return FormValidation.warning("Is this a valid URL?");
            
            return FormValidation.ok();
        }
        
        /**
         *  ProGet connection test
         */
        public FormValidation doTestConnection(
                @QueryParameter("url") final String url,
                @QueryParameter("apiKey") final String apiKey,
                @QueryParameter("user") final String user,
                @QueryParameter("password") final Secret password,
                @QueryParameter("trustAllCertificates") final boolean trustAllCertificates) throws IOException, ServletException {

            ProGetConfig config = new ProGetConfig();

            config.url = url;
            config.apiKey = apiKey;
            config.user = user;
            config.password = Secret.toString(password);
            config.trustAllCertificates = trustAllCertificates;

            ProGetApi proget = new ProGetApi(config, new JenkinsConsoleLogWriter());

            try {
                proget.canConnect();
            } catch (Exception ex) {
                return FormValidation.error("Failed. Please check the configuration: " + ex.getClass().getName() + " - " + ex.getMessage());
            }

            return FormValidation.ok("Success. Connection with ProGet verified.");
        }

        public ProGetConfig getProGetConfig() {
            ProGetConfig config = new ProGetConfig();

            config.url = url;
            config.user = user;
            config.password = Secret.toString(password);
            config.apiKey = apiKey;
            config.logApiRequests = logApiRequests;
            config.trustAllCertificates = trustAllCertificates;

            return config;
        }
    }
}