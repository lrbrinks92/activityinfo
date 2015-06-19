package org.activityinfo.test.webdriver;

/**
* Created by alex on 26-1-15.
*/
public enum BrowserVendor {
    IE("internet explorer", "ie"),
    SAFARI("safari"),
    CHROME("chrome"),
    FIREFOX("firefox"),
    OPERA("opera");

    private String sauceId;
    private String tag;

    BrowserVendor(String sauceId) {
        this(sauceId, sauceId);
    }

    BrowserVendor(String sauceId, String tag) {
        this.sauceId = sauceId;
        this.tag = tag;
    }

    public String sauceId() {
        return sauceId;
    }
    
    public String tag() {
        return tag;
    }
}
