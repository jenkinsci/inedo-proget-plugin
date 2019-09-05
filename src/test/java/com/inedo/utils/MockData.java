package com.inedo.utils;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import hidden.jth.org.apache.http.entity.InputStreamEntity;

public enum MockData {
    FEED("Feed.json"), 
    FEEDS("Feeds.json"),
    PACKAGES("Packages.json"),
    PACKAGE_VERSIONS("PackageVersions.json");

    private static final String RESOURCE_PACKAGE = "/com/inedo/proget/mockdata/";
    private String resourceName;

    private MockData(String name) {
        this.resourceName = name;
    }

    public InputStreamEntity getInputSteam() {
        return new InputStreamEntity(this.getClass().getResourceAsStream(RESOURCE_PACKAGE + resourceName));
    }

    public String getAsString() {
        try {
            return IOUtils.toString(this.getClass().getResourceAsStream(RESOURCE_PACKAGE + resourceName));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load resource " + resourceName, e);
        }
    }
}
