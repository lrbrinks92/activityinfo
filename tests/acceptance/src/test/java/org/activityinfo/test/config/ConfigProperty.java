package org.activityinfo.test.config;

import com.google.common.base.Strings;

import java.io.File;


public class ConfigProperty {
    private String propertyKey;
    private String description;

    public ConfigProperty(String propertyKey, String description) {
        this.propertyKey = propertyKey;
        this.description = description;
    }

    public String get() {
        String path = System.getProperty(propertyKey);
        if(Strings.isNullOrEmpty(path)) {
            throw new ConfigurationError(String.format("Please specify %s using the system property '%s'", description,
                    propertyKey));
        }
        return path;
    }


    public boolean isPresent() {
        String value = System.getProperty(propertyKey);
        return !Strings.isNullOrEmpty(value);
    }
    
    public String getOr(String defaultValue) {
        String path = System.getProperty(propertyKey);
        if(Strings.isNullOrEmpty(path)) {
            return defaultValue;
        }
        return path;
    }
    


    public File getFile() {
        String path = get();

        File file = new File(path);
        if(!file.exists()) {
            throw new ConfigurationError(String.format(
                    "The file specified by the system property '%s' does not exist: %s", propertyKey,
                    file.getAbsolutePath()));
        }

        return file;
    }
}
