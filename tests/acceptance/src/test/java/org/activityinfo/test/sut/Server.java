package org.activityinfo.test.sut;

/**
 * Provides information about and access to the deployment of the
 * server currently under test
 */
public class Server {

    private final String rootUrl;

    public Server(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getRootUrl() {
        return path("");
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
