package com.inedo.proget.jenkins;

import java.io.Serializable;

public class PutDetails implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public final String feedName;
    public final String include;
    public final String exclude;
    public final boolean defaultExcludes;
    public final boolean caseSensitive;

    public PutDetails (UploadPackageBuilder settings, JenkinsHelper helper) {
        this.feedName = settings.getFeedName();
        this.include = helper.expandVariable(settings.getArtifacts());
        this.exclude = helper.expandVariable(settings.getExcludes());
        this.defaultExcludes = settings.isDefaultExcludes();
        this.caseSensitive = settings.isCaseSensitive();
    }
}