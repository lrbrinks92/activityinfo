package org.activityinfo.test.webdriver;


import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.activityinfo.test.config.ConfigProperty;
import org.activityinfo.test.sut.Server;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.HasInputDevices;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;

import javax.inject.Inject;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.google.common.io.Files.write;

/**
 * Stores state related to an individual WebDriver session, lazily creating a WebDriver instance
 * if actually used
 */
@ScenarioScoped
public class WebDriverSession {

    private static final ConfigProperty COVERAGE_REPORT_DIR = new ConfigProperty("gwt.coverage.report.dir",
            "Directory to write gwt coverage results.");

    private final WebDriverProvider provider;
    private WebDriver driver;
    private WebDriver proxy;
    private Server server;
    private String testName;
    private WebDriverConnection webDriverConnection;
    private BrowserProfile browserProfile = new BrowserProfile(OperatingSystem.WINDOWS_7, BrowserVendor.CHROME);

    @Inject
    public WebDriverSession(WebDriverProvider provider, Server server) {
        this.provider = provider;
        this.server = server;
        this.proxy =  (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{ WebDriver.class, TakesScreenshot.class, HasInputDevices.class, JavascriptExecutor.class },
                new WebDriverProxy());
    }

    public void beforeTest(String testName) {
        this.testName = testName;
    }

    private void startSession() {
        this.driver = provider.start(testName, browserProfile);
    }

    public WebDriver getDriver() {
        return proxy;
    }

    public SessionId getSessionId() {
        Preconditions.checkState(driver != null, "WebDriver is not started");
        RemoteWebDriver remoteWebDriver = (RemoteWebDriver) driver;
        return remoteWebDriver.getSessionId();
    }

    public boolean isRunning() {
        return driver != null;
    }

    public void stop() {
        if(driver != null) {
            if(COVERAGE_REPORT_DIR.isPresent()) {
                recordCoverage();
            }
            driver.quit();
            driver = null;
        }
    }

    private void recordCoverage()  {
        
        Preconditions.checkState(testName != null, "No test name is set");
        
        try {
            File outputDir = COVERAGE_REPORT_DIR.getDir();
            File reportFile = new File(outputDir, testName +".json");

            // Trigger the 'onLoad' event which should write the statistics to local storage
            driver.navigate().to(server.path("coverage.html"));

            // Retrieve the results from local storage
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String json = (String) js.executeScript(String.format(
                    "return localStorage.getItem('%s');", "gwt_coverage"));

            if (!Strings.isNullOrEmpty(json)) {
                write(json, reportFile, Charsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception retrieving coverage results", e);
        }
    }

    public void setBrowserProfile(BrowserProfile browserProfile) {
        this.browserProfile = browserProfile;
    }

    public BrowserProfile getBrowserProfile() {
        return browserProfile;
    }

    public void setConnected(boolean connected) {
        if(driver instanceof WebDriverConnection) {
            ((WebDriverConnection) driver).setConnected(connected);
        }
    }

    private class WebDriverProxy implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(driver == null) {
                startSession();
            }
            try {
                return method.invoke(driver, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

    }

}
