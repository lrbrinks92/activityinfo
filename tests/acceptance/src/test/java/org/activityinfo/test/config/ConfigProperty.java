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
            path = findEnvironmentVariable();
        }
        if(Strings.isNullOrEmpty(path)) {
            throw new ConfigurationError(String.format("Please specify %s using the system property '%s'", description,
                    propertyKey));
        }
        return path;
    }

    private String findEnvironmentVariable() {

        // Try exact match first
        String value = System.getenv(propertyKey);
        if(!Strings.isNullOrEmpty(value)) {
            return value;
        }
        
        for (String envName : System.getenv().keySet()) {
            if(envName.replace("_", "").toLowerCase().equalsIgnoreCase(propertyKey)) {
                return System.getenv(envName);
            }
        }
        return null;
    }


    public boolean isPresent() {
        String value = System.getProperty(propertyKey);
        return !Strings.isNullOrEmpty(value);
    }

    /**
     * Returns property value if not null and not empty, otherwise default value is returned.
     *
     * @param defaultValue default value
     * @return property value if not null and not empty, otherwise default value is returned.
     */
    public String getOr(String defaultValue) {
        String path = System.getProperty(propertyKey);
        if (Strings.isNullOrEmpty(path)) {
            return defaultValue;
        }
        return path;
    }

    /**
     * Returns property value if present (not null), otherwise default value is returned.
     *
     * @param defaultValue default value
     * @return property value if present (not null), otherwise default value is returned.
     */
    public String getIfPresent(String defaultValue) {
        String path = System.getProperty(propertyKey);
        if (path == null) {
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


    public File getDir() {
        String path = get();

        File file = new File(path);
        if(file.exists() && !file.isDirectory()) {
            throw new ConfigurationError(String.format("The system property '%s' should refer to a directory," +
                    " not a file.", propertyKey));
        }
        if(!file.exists()) {
            boolean created = file.mkdirs();
            if(!created) {
                throw new ConfigurationError(String.format("Could not create the directory '%s' at '%s'", 
                        propertyKey, file));
            }
        }
        return file;
    }
}
