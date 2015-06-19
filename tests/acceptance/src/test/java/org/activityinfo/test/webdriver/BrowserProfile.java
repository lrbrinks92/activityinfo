package org.activityinfo.test.webdriver;


import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import org.openqa.selenium.remote.BrowserType;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class BrowserProfile implements DeviceProfile, Serializable {
    
    
    @Nonnull
    private final OperatingSystem os;

    @Nonnull
    private final BrowserVendor browser;

    @Nonnull
    private final Version browserVersion;

    private final Set<String> tags;
    
    public BrowserProfile(@Nonnull OperatingSystem os, @Nonnull BrowserVendor browser, @Nonnull String browserVersion) {
        this.os = os;
        this.browser = browser;
        this.browserVersion = new Version(browserVersion);
        this.tags = Collections.emptySet();
    }

    public BrowserProfile(OperatingSystem os, BrowserVendor browser) {
        this(os, browser, Version.UNKNOWN.toString());
    }

    public OperatingSystem getOS() {
        return os;
    }

    public BrowserVendor getType() {
        return browser;
    }

    public Version getVersion() {
        return browserVersion;
    }

    @Override
    public Serializable getId() {
        return this;
    }

    @Override
    public String getName() {
        return Joiner.on(" ").join(Arrays.asList(os.toString(), browser, browserVersion));
    }

    @Override
    public Set<String> getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BrowserProfile that = (BrowserProfile) o;

        if (browser != that.browser) return false;
        if (!browserVersion.equals(that.browserVersion)) return false;
        if (os != that.os) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = os.hashCode();
        result = 31 * result + browser.hashCode();
        result = 31 * result + browserVersion.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BrowserProfile{" +
                "os=" + os +
                ", browser=" + browser +
                ", browserVersion='" + browserVersion + '\'' +
                ", tags=" + tags +
                '}';
    }
}
