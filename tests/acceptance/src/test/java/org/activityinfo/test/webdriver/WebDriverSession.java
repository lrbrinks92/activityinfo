package org.activityinfo.test.webdriver;


import com.google.common.base.Preconditions;
import cucumber.api.Scenario;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;

import javax.inject.Inject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@ScenarioScoped
public class WebDriverSession {

    private final WebDriverProvider provider;
    private WebDriver driver;
    private WebDriver proxy;
    private Scenario scenario;

    @Inject
    public WebDriverSession(WebDriverProvider provider) {
        this.provider = provider;
        this.proxy =  (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{ WebDriver.class, TakesScreenshot.class },
                new WebDriverProxy());
    }

    public WebDriver getDriver() {
        return proxy;
    }

    public void start(Scenario scenario, BrowserProfile profile) {
        Preconditions.checkState(driver == null, "WebDriver is already started");
        
        this.scenario = scenario;
        this.driver = provider.start(scenario.getId(), profile);
    }

    public void start(Scenario scenario) {
        start(scenario, null);
    }
    
    public SessionId getSessionId() {
        Preconditions.checkState(driver != null, "WebDriver is not started");
        RemoteWebDriver remoteWebDriver = (RemoteWebDriver) driver;
        return remoteWebDriver.getSessionId();
    }

    public BrowserVendor getBrowserType() {
        RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;
        String browserName = remoteDriver.getCapabilities().getBrowserName().toLowerCase();
        switch(browserName) {
            case "phantomjs":
            case "chrome":
                return BrowserVendor.CHROME;
            case "internet explorer":
                return BrowserVendor.IE;
            case "firefox":
                return BrowserVendor.FIREFOX;
            case "safari":
                return BrowserVendor.SAFARI;
        }
        throw new UnsupportedOperationException("browserName: " + browserName);
    }
    
    public boolean isRunning() {
        return driver != null;
    }
    
    public void stop() {
        if(driver != null) {
            driver.quit();
            driver = null;
        }
    }

    private class WebDriverProxy implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(driver == null) {
                throw new IllegalStateException("WebDriver was not started");
            }
            return method.invoke(driver, args);
        }
    }
}
