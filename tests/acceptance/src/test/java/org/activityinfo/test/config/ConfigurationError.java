package org.activityinfo.test.config;

/**
 * Error thrown when the tests cannot be set up because of a problem
 * with the configuration
 */
public class ConfigurationError extends Error {

    public ConfigurationError(String message) {
        super(message);
    }

    public ConfigurationError(String s, Exception e) {
        super(s, e);
    }
}
