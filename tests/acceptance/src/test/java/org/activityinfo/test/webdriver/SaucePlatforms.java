package org.activityinfo.test.webdriver;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import gherkin.deps.com.google.gson.Gson;
import gherkin.deps.com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;


public class SaucePlatforms {


    public static List<SupportedPlatform> fetch() throws IOException {
        URL url = new URL("https://saucelabs.com/rest/v1/info/platforms/webdriver");
        try(Reader in = new InputStreamReader(url.openStream(), Charsets.UTF_8)) {
            Gson gson = new Gson();
            return Arrays.asList(gson.fromJson(in, SupportedPlatform[].class));
        }
    }

    public static List<BrowserProfile> fetchBrowsers() throws IOException {
        List<BrowserProfile> profiles = Lists.newArrayList();
        for(SupportedPlatform platform : fetch()) {
            if(platform.isBrowser()) {
                BrowserProfile profile;
                try {
                    profiles.add(platform.toBrowserProfile());
                } catch (Exception e) {
                    System.err.println(String.format("Exception parsing profile '%s': %s",
                            platform.toString(), e.getMessage()));
                }
            }
        }
        return profiles;
    }

    public static void main(String[] args) throws IOException {
        List<SupportedPlatform> platforms = SaucePlatforms.fetch();
        for (SupportedPlatform browserProfile : platforms) {
            try {
                System.out.println(browserProfile + " => " + browserProfile.toBrowserProfile());
            } catch(Exception e) {
                System.out.println(browserProfile + " => " + e.getMessage());
            }
        }
    }



    private static class SupportedPlatform {

        @SerializedName("short_version")
        private String shortVersion;

        @SerializedName("api_name")
        private String apiName;

        @SerializedName("long_version")
        private String longVersion;

        @SerializedName("latest_stable_version")
        private String latestStableVersion;

        private String os;
        
        public BrowserVendor getBrowserVendor() {
            switch(apiName) {
                case "chrome":
                    return BrowserVendor.CHROME;
                case "firefox":
                    return BrowserVendor.FIREFOX;
                case "safari":
                    return BrowserVendor.SAFARI;
                case "internet explorer":
                    return BrowserVendor.IE;
                default:
                    return null;
            }
        }
        
        public OperatingSystem parseOS() {
            String name = os;
            String version = "";
            int nameEnd = os.indexOf(' ');
            if(nameEnd != -1) {
                name = os.substring(0, nameEnd);
                version = os.substring(nameEnd+1);
            }
            switch(name) {
                case "Windows":
                    return OperatingSystemType.WINDOWS.version(winVersion(version));
                case "Linux":
                    return OperatingSystemType.LINUX.unknownVersion();
                case "Mac":
                    return OperatingSystemType.OSX.version(version);
                default:
                    throw new UnsupportedOperationException(os);
            }
        }

        private String winVersion(String version) {
            /*
            For Windows XP, we use Windows Server 2003 R2.
            For Windows 7, we use Windows Server 2008 R2.
            For Windows 8, we use Windows Server 2012.
            For Windows 8.1, we use Windows Server 2012 R2.
             */
            if(version.equals("2012 R2")) {
                return "8.1";
            } else if(version.equals("2012")) {
                return "8";
            } else if(version.startsWith("2008")) {
                return "7";
            } else if(version.startsWith("2003")) {
                return "XP";
            }
            return version;
        }
        
        public BrowserProfile toBrowserProfile() {
            return new BrowserProfile(parseOS(), getBrowserVendor(), shortVersion);
        }

        public boolean isBrowser() {
            return getBrowserVendor() != null;
        }
        
        public String toString() {
            return os + " " + apiName + " " + longVersion + " [" + latestStableVersion + "]";
        }
    }
    
    
    
}
