package com.inedo.proget.jenkins;

import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.ServletException;

import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.inedo.proget.api.ProGet;
import com.inedo.proget.api.ProGetConfig;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.util.ListBoxModel;

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
//    	private String authentication;
//    	private String domain;
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

//        public void setAuthentication(String value) {
//			this.authentication = value;
//		}
//
//        public void setDomain(String value) {
//			this.domain = value;
//		}
        
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
        
//        public String getAuthentication() {
//			return authentication;
//		}
//
//        public String getDomain() {
//			return domain;
//		}

        public String getUser() {
            return user;
        }
        
        public String getPassword() {
            return Secret.toString(password);
        }
        
        public String getApiKey() {
            return apiKey;
        }
        
        public boolean validatePluginConfiguration() {
        	if( url == null || apiKey == null ||
    			url.isEmpty() || apiKey.isEmpty() ) {
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
        
//        public FormValidation doCheckDomain(@QueryParameter String value, @QueryParameter String authentication) throws IOException, ServletException {
//            if (value.length() == 0 && "ntlm".equals(authentication))
//                return FormValidation.error("Domain is required");
//            
//            if (value.length() > 0 && "none".equals(authentication))
//                return FormValidation.warning("Value will be ignored as only valid for NTLM authentication method");
//            
//            return FormValidation.ok();
//        }

        public FormValidation doCheckUser(@QueryParameter String value, @QueryParameter String authentication) throws IOException, ServletException {
            if (value.length() == 0 && !"none".equals(authentication))
                return FormValidation.error("User is required");

            if (value.length() > 0 && "none".equals(authentication))
                return FormValidation.warning("Value will be ignored as no authentication method selected");

            return FormValidation.ok();
        }

        public FormValidation doCheckPassword(@QueryParameter String value, @QueryParameter String authentication) throws IOException, ServletException {
            if (value.length() == 0 && !"none".equals(authentication))
                return FormValidation.error("Password is required");

            if (value.length() > 0 && "none".equals(authentication))
                return FormValidation.warning("Value will be ignored as no authentication method selected");

            return FormValidation.ok();
        }

        public ListBoxModel doFillAuthenticationItems() {
            ListBoxModel items = new ListBoxModel();
        
            items.add(ConnectionType.NONE.getLabel(), ConnectionType.NONE.getId());
            items.add(ConnectionType.BASIC.getLabel(), ConnectionType.BASIC.getId());
            items.add(ConnectionType.NTLM.getLabel(), ConnectionType.NTLM.getId());
        	
            return items;
        }
        
        /**
         *  ProGet connection test
         */
		public FormValidation doTestConnection(
			@QueryParameter("url") final String url,
//			@QueryParameter("authentication") final String authentication,
//			@QueryParameter("domain") final String domain,
			@QueryParameter("user") final String user,
			@QueryParameter("password") final String password,
			@QueryParameter("apiKey") final String apiKey) throws IOException, ServletException {
	
			ProGetConfig config = new ProGetConfig();
			
			config.url = url;
//			config.authentication = authentication;
//			config.domain = domain;
			config.user = user;
			config.password = password;
			config.apiKey = apiKey;
			
			ProGet proget = new ProGet(config, null);
			
			try {
				proget.checkConnection();
			} catch (Exception ex) {
				return FormValidation.error("Failed. Please check the configuration. " + ex.getClass().getName() + ": " + ex.getMessage());
			}
			
			return FormValidation.ok("Success. Connection with ProGet verified.");			
		}

		public ProGetConfig getProGetConfig() {
			ProGetConfig config = new ProGetConfig();
   		 
			config.url = url;
//			config.authentication = authentication;
//			config.domain = domain;
			config.user = user;
			config.password = Secret.toString(password);
			config.apiKey = apiKey;
			
    		return config;
		}

		public ProGetConfig getProGetConfig(PrintStream logger) {
			ProGetConfig config = getProGetConfig();
			config.printStream = logger;
			return config;
		}
    }
}