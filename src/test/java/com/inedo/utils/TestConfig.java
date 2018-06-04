package com.inedo.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import com.inedo.proget.api.ProGetConfig;

/**
 * Reads and supplies properties from the config.properties file that are required by the framework.
 *
 * This class can be extended by an AppConfig class to provide application specific properties.
 *
 * @author Andrew Sumner
 */
public class TestConfig {
    /** Name of the property file */
    protected static final String CONFIG_FILE = "test.properties";

    private static boolean useMockServer;
    private static String url;
    private static String apiKey;
    private static String username;
    private static String password;
    private static boolean trustAllCertificates;

    /** Ensure properties have been loaded before any property is used. */
    static {
        synchronized (TestConfig.class) {
            loadProperties();
        }
    }

    /** Prevent this class from being constructed. */
    protected TestConfig() { }

    private static void loadProperties() {
        Properties prop = loadFile(CONFIG_FILE);

        useMockServer = Boolean.valueOf(getProperty(prop, "useMockServer"));

        url = getOptionalProperty(prop, "url");
        apiKey = getOptionalProperty(prop, "apiKey");
        username = getOptionalProperty(prop, "username");
        password = getOptionalProperty(prop, "password");
        trustAllCertificates = Boolean.valueOf(getOptionalProperty(prop, "trustAllCertificates"));
    }

    /**
     * Read properties from file, will ignoring the case of properties.
     *
     * @param filename Name of file to read, expected that it will be located in the projects root folder
     * @return {@link Properties}
     */
    protected static Properties loadFile(final String filename) {
        Properties prop = new Properties();

        if (!new File(filename).exists()) {
            return prop;
        }

        try (InputStream input = new FileInputStream(filename);) {
            prop.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read properties file.", e);
        }

        return prop;
    }

    /**
     * Get the property for the current environment, if that is not found it will look for "default.{@literal <key>}".
     *
     * @param properties	A set of properties
     * @param key	Id of the property to look up
     * @return 		Property value if found, throws exception if not found
     */
    protected static String getProperty(Properties properties, String key) {
        String value = retrieveProperty(properties, key);

        if (value == null) {
            throw new RuntimeException(String.format("Unable to find property %s", key));
        }

        return value;
    }

    /**
     * Get the property for the current environment, if that is not found it will look for "default.{@literal <key>}".
     *
     * @param properties	A set of properties
     * @param key	Id of the property to look up
     * @return 		Property value if found, empty string if not found
     */
    protected static String getOptionalProperty(Properties properties, String key) {
        return retrieveProperty(properties, key);
    }

    /**
     * Get the property for the current environment, if that is not found it will look for "default.{@literal <key>}".
     *
     * @param properties	A set of properties
     * @param key			Id of the property to look up
     * @param defaultValue	value to use if property is not found
     * @return 		Property value if found, defaultValue if not found
     */
    protected static String getOptionalProperty(Properties properties, String key, String defaultValue) {
        String value = retrieveProperty(properties, key);

        if (value.isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    private static String retrieveProperty(Properties properties, String key) {
        // Get setting if set for user
        String prefix = System.getProperty("user.name").toLowerCase();
        String value = properties.getProperty(prefix + "." + key);

        // Get setting if set for environment
//        if (value == null && environment != null) {
//            prefix = environment;
//            value = properties.getProperty(prefix + "." + key);
//        }

        // Get default setting
        if (value == null) {
            value = properties.getProperty(key);
        }

        if (value != null) {
            value = value.trim();
        } else {
            value = "";
        }

        return value;
    }

    public static boolean useMockServer() {
        return useMockServer;
    }

    public static ProGetConfig getProGetConfig() {
        ProGetConfig config = new ProGetConfig();

        config.url = url;
        config.apiKey = apiKey;
        config.user = username;
        config.password = password;
        config.trustAllCertificates = trustAllCertificates;

        return config;
    }
}