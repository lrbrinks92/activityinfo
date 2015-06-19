package org.activityinfo.test.webdriver;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.List;

/**
 * Runs tests against a local chrome browser.
 */
@Singleton
public class ChromeWebDriverProvider implements WebDriverProvider {
    
    
    private static final WebDriverPool POOL = initPool();
    
    private static final BrowserProfile PROFILE = new BrowserProfile(OperatingSystem.host(), BrowserVendor.CHROME);

    public ChromeWebDriverProvider() {
    }

    private static WebDriverPool initPool() {
        WebDriverPool pool = new WebDriverPool();
        pool.setMaxTotalSize(3);
        pool.setCreator(new Function<BrowserProfile, WebDriver>() {
            @Override
            public WebDriver apply(BrowserProfile input) {
                
                // Start a local http proxy that we can use to control the 
                // the connection's properties
                ProxyController proxyController = new ProxyController();
                proxyController.start();
                
                DesiredCapabilities capabilities = new DesiredCapabilities();
                capabilities.setCapability(CapabilityType.PROXY, proxyController.getWebDriverProxy());
                
                return new ProxiedWebDriver(new ChromeDriver(capabilities), proxyController);
            }
        });
        return pool;
    }

    @Override
    public List<? extends DeviceProfile> getSupportedProfiles() {
        return Lists.newArrayList();
    }

    @Override
    public boolean supports(DeviceProfile profile) {
        return true;
    }

    @Override
    public WebDriver start(String name, BrowserProfile profile) {
       return POOL.get(PROFILE);
    }
}
