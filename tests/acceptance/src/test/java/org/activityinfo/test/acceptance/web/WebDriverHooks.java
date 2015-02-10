package org.activityinfo.test.acceptance.web;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.activityinfo.test.webdriver.*;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@ScenarioScoped
public class WebDriverHooks {


    @Inject
    private WebDriverSession session;
    
    @Inject
    private SessionReporter reporter;
    
    private Scenario scenario;

    @Before
    public void before(Scenario scenario) {
        this.scenario = scenario;
        if(scenario.getSourceTagNames().contains("@web")) {
            session.start(scenario);
        }
    }
    
    public void start(BrowserProfile profile) {
        session.start(scenario, profile);
    }

    @After
    public void after(Scenario scenario) {
        if(session.isRunning()) {
            try {
                reporter.finished(scenario);
            } finally {
                session.stop();
            }
        }
    }

    @Given("^my browser supports offline mode$")
    public void my_browser_supports_offline_mode() throws Throwable {
        start(new BrowserProfile(OperatingSystem.WINDOWS_7, BrowserVendor.CHROME));
    }

    @Given("^I am using (.*) on (.*)$")
    public void I_am_using_browser_version_on_os(String browser, String os) throws Throwable {
        start(new BrowserProfile(parseOs(os), parseVendor(browser), extractVersion(browser)));
    }


    private String extractVersion(String browser) {
        Matcher matcher = Pattern.compile("[\\d\\\\.]+").matcher(browser);
        if(matcher.find()) {
            return matcher.group(0);
        } else {
            return Version.UNKNOWN.toString();
        }
    }

    private BrowserVendor parseVendor(String browser) {
        browser = browser.toLowerCase();
        if(browser.contains("ie") || browser.contains("internet")) {
            return BrowserVendor.IE;
        } else {
            for(BrowserVendor vendor : BrowserVendor.values()) {
                if(browser.contains(vendor.name().toLowerCase())) {
                    return vendor;
                }
            }
        }
        throw new IllegalArgumentException("Could not parse browser type. Should be one of " +
                Arrays.toString(BrowserVendor.values()));
    }

    private OperatingSystem parseOs(String os) {
        os = os.toLowerCase();
        if(os.contains("win")) {
            if(os.contains("xp")) {
                return new OperatingSystem(OperatingSystemType.WINDOWS, "XP");
            } else {
                return OperatingSystemType.WINDOWS.version(extractVersion(os));
            }
        } else if(os.contains("linux")) {
            return OperatingSystemType.LINUX.unknownVersion();
        } else if(os.contains("mac") || os.contains("os x") || os.contains("osx")) {
            return OperatingSystemType.OSX.version(extractVersion(os));
        }
        throw new IllegalArgumentException(os);
    }
}
