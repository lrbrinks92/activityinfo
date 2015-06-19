package org.activityinfo.test.driver;

public class SyncUpdate {
    private String version;
    private long byteCount;
    private boolean complete;

    public String getVersion() {
        return version;
    }

    public long getByteCount() {
        return byteCount;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setByteCount(long byteCount) {
        this.byteCount = byteCount;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
