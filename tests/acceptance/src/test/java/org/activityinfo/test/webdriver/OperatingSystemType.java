package org.activityinfo.test.webdriver;


import com.google.common.base.Strings;

import java.util.Locale;

public enum OperatingSystemType {
    WINDOWS,
    OSX,
    LINUX,
    UNKNOWN;
    
    public static final OperatingSystemType host() {
        String osName = System.getProperty("os.name", "unknown").toLowerCase(Locale.ENGLISH);
        if ((osName.contains("mac")) || (osName.contains("darwin"))) {
            return OSX;
            
        } else if (osName.contains("windows")) {
            return WINDOWS;

        } else if (osName.contains("nux")) {
            return LINUX;
            
        } else {
            return UNKNOWN;
        }
    }
    
    public OperatingSystem version(String version) {
        return new OperatingSystem(this, version);
    }
    
    public OperatingSystem unknownVersion() {
        return new OperatingSystem(this, Version.UNKNOWN);
    }
}
