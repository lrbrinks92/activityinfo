package org.activityinfo.test.driver;


public class SyncRegion {
    private final String id;
    private final String version;

    public SyncRegion(String id, String currentVersion) {
        this.id = id;
        this.version = currentVersion;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }
}
