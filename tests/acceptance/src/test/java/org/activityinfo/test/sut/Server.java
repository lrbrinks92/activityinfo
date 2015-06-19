package org.activityinfo.test.sut;

import org.activityinfo.test.config.ConfigProperty;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Provides information about and access to the deployment of the
 * server currently under test
 */
public class Server {


    public static final ConfigProperty TEST_URL = new ConfigProperty("test.url", "Root URL to Test");

    private static final String LOCAL_URL = "http://localhost:8080/";
    
    private final String rootUrl;

    public Server() {
        this.rootUrl = TEST_URL.getOr(LOCAL_URL);
    }
    
    public Server(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getRootUrl() {
        return path("");
    }
    
    public URI getRootUri() {
        try {
            return new URI(getRootUrl());
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    public String path(String path) {
        String url = rootUrl;
        if(!url.endsWith("/")) {
            url += "/";
        }
        if(path.startsWith("/")) {
            url += path.substring(1);
        } else {
            url += path;
        }
        return url;
    }

    /**
     *
     * @return the URL of the application
     */
    public String appUrl() {
        return path("");
    }
}
