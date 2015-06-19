package org.activityinfo.test.webdriver;

import com.google.common.base.Strings;
import com.google.inject.Singleton;
import com.saucelabs.saucerest.SauceREST;
import org.activityinfo.test.config.ConfigProperty;
import org.activityinfo.test.config.ConfigurationError;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static java.lang.String.format;


@Singleton
public class SauceLabsDriverProvider implements WebDriverProvider {

    /**
     * Used to receive information from SauceReporter, 
     * which is not instantiated by Guice
     */
    static SauceLabsDriverProvider INSTANCE = null;

    public static final String JENKINS_SAUCE_USER_NAME = "SAUCE_USER_NAME";
    public static final String JENKINS_SAUCE_API_KEY = "SAUCE_API_KEY";

    public static final ConfigProperty JENKINS_SELENIUM_HOST = new ConfigProperty("SELENIUM_HOST",
            "The host name of the selenium server");

    public static final ConfigProperty JENKINS_SELENIUM_PORT = new ConfigProperty("SELENIUM_PORT",
            "The port of the selenium server");


    public static final ConfigProperty SAUCE_USERNAME = new ConfigProperty("sauce.username", "Sauce.io username");
    public static final ConfigProperty SAUCE_ACCESS_KEY = new ConfigProperty("sauce.accessKey", "Sauce.io access key");

    public static final ConfigProperty SAUCE_FAST = new ConfigProperty("SAUCE_FAST", "If true, enable sauce speed optimizations");


    private String userName;
    private String apiKey;


    public static boolean isEnabled() {
        return !Strings.isNullOrEmpty(System.getenv(JENKINS_SAUCE_USER_NAME)) ||
                SAUCE_USERNAME.isPresent();
    }

    @Inject
    public SauceLabsDriverProvider() {

        INSTANCE = this;

        // Provided by the Jenkins plugin
        userName = System.getenv(JENKINS_SAUCE_USER_NAME);
        apiKey = System.getenv(JENKINS_SAUCE_API_KEY);

        if(Strings.isNullOrEmpty(userName) ||
                Strings.isNullOrEmpty(apiKey)) {

            userName = SAUCE_USERNAME.get();
            apiKey = SAUCE_ACCESS_KEY.get();
        }
    }

    private URL getWebDriverServer() {

        String host = JENKINS_SELENIUM_HOST.getOr("ondemand.saucelabs.com");
        String port = JENKINS_SELENIUM_PORT.getOr("80");

        String url = format("http://%s:%s@%s:%s/wd/hub", userName, apiKey, host, port);

        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new ConfigurationError(format("Sauce labs remote address [%s] is malformed.", url), e);
        }
    }

    @Override
    public List<BrowserProfile> getSupportedProfiles() {
        try {
            return SaucePlatforms.fetchBrowsers();
        } catch (IOException e) {
            throw new RuntimeException("Could not fetch supported browser", e);
        }
    }

    @Override
    public boolean supports(DeviceProfile profile) {
        return profile instanceof BrowserProfile;
    }

    private String osName(BrowserProfile browser) {
        switch(browser.getOS().getType()) {
            case WINDOWS:
                return "Windows " + browser.getOS().getVersion();
            case OSX:
                return "OS X " + browser.getOS().getVersion();
            case LINUX:
                return "Linux";
            default:
                return null;
        }
    }

    @Override
    public WebDriver start(String name, BrowserProfile profile) {

        DesiredCapabilities capabilities = new DesiredCapabilities();
        if(!Strings.isNullOrEmpty(System.getenv("SELENIUM_BROWSER"))) {
            capabilities.setCapability(CapabilityType.BROWSER_NAME, System.getenv("SELENIUM_BROWSER"));
            capabilities.setCapability(CapabilityType.VERSION, System.getenv("SELENIUM_VERSION"));
            capabilities.setCapability(CapabilityType.PLATFORM, System.getenv("SELENIUM_PLATFORM"));

        } else if(profile != null) {
            capabilities.setCapability(CapabilityType.BROWSER_NAME, profile.getType().sauceId());
            capabilities.setCapability(CapabilityType.VERSION, profile.getVersion().toString());
            capabilities.setCapability(CapabilityType.PLATFORM, osName(profile));


        } else {
            capabilities.setCapability(CapabilityType.BROWSER_NAME, BrowserType.CHROME);
            capabilities.setCapability("name", name);
        }

        if(SAUCE_FAST.isPresent()) {
            capabilities.setCapability("record-video", false);
            capabilities.setCapability("record-screenshots", false);
        }

        RemoteWebDriver remoteWebDriver = new RemoteWebDriver(getWebDriverServer(), capabilities);
        System.out.println(format("SauceOnDemandSessionID=%s job-name=%s", remoteWebDriver.getSessionId(), name));
        
        return remoteWebDriver;
    }


    public SauceREST getRestClient() {
        return new SauceREST(userName, apiKey);
    }
}
