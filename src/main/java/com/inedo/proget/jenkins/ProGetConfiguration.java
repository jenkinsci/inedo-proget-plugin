package com.inedo.proget.jenkins;

import java.io.IOException;
import javax.servlet.ServletException;

import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.inedo.proget.api.ProGetApi;
import com.inedo.proget.api.ProGetConfig;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;

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
    	private String user;
        private Secret password;
        private String apiKey;
        
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

        public void setUser(String value) {
            user = value;
        }
        
        public void setPassword(String value) {
            password = Secret.fromString(value);
        }
        
        public void setApiKey(String value) {
            apiKey = value;
        }
        
        /**
         * Field getters
         */
        public String getUrl() {
            return url;
        }
        
        public String getUser() {
            return user;
        }
        
        public String getPassword() {
            return Secret.toString(password);
        }
        
        public String getApiKey() {
            return apiKey;
        }
        
        public boolean isRequiredFieldsConfigured(boolean includeUsername) {
        	if( url == null || url.trim().isEmpty()) {
    			return false;
    		}
        	
        	if (includeUsername) {
        		if( user == null || user.trim().isEmpty()) {
        			return false;
        		}
        		
        		if( password == null || Secret.toString(password).trim().isEmpty()) {
        			return false;
        		}
        	}
        	
    		return true;
        }

        public boolean isApiKeyConfigured() {
        	if( apiKey == null || apiKey.isEmpty() ) {
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
			@QueryParameter("user") final String user,
			@QueryParameter("password") final String password,
			@QueryParameter("apiKey") final String apiKey) throws IOException, ServletException {
	
			ProGetConfig config = new ProGetConfig();
			
			config.url = url;
			config.user = user;
			config.password = password;
			config.apiKey = apiKey;
			
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
			
    		return config;
		}
    }
}