package com.inedo.proget.jenkins;

public enum DownloadFormat {
    PACKAGE("pkg", "Package"),
    CONTENT_AS_ZIP("zip", "Content as ZIP"), 
    CONTENT_AS_TGZ("tgz", "Content as TGZ"),
    EXTRACT_CONTENT("unpack", "Unpack Content");
    
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

