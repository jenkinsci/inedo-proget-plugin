package com.inedo.proget.jenkins.utils;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Common Jenkins tasks. 
 * 
 * @author Andrew Sumner
 */
public class JenkinsHelper {
    private final Run<?, ?> build;
    private final TaskListener listener;
    private JenkinsLogWriter logWriter = null;

    /**
     * For unit tests as they don't have access to the build or listener
     */
    public JenkinsHelper() {
        this.build = null;
        this.listener = null;
        this.logWriter = new JenkinsConsoleLogWriter();
    }

    public JenkinsHelper(Run<?, ?> build, TaskListener listener) {
        this.build = build;
        this.listener = listener;
        this.logWriter = new JenkinsTaskLogWriter(listener);
    }

    public String expandVariable(String variable) {
        if (build == null) {
            return variable;
        }

        if (variable == null || variable.isEmpty()) {
            return variable;
        }

        String expanded = variable;

        try {
            // Pipeline script doesn't support getting environment variables
            if (build instanceof AbstractBuild) {
                expanded = build.getEnvironment(listener).expand(variable);
            }
        } catch (Exception e) {
            getLogWriter().info("Exception thrown expanding '" + variable + "' : " + e.getClass().getName() + " " + e.getMessage());
        }

        return expanded;
    }

    public void injectEnvrionmentVariable(String key, String value) {
        if (build == null) {
            return;
        }

        build.addAction(new VariableInjectionAction(key, value));
    }

    public JenkinsLogWriter getLogWriter() {
        return logWriter;
    }

    public static void fail(String value) {
        throw new RuntimeException(value);
    }
}
