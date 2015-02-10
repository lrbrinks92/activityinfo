package org.activityinfo.test.webdriver;

import com.google.common.base.Function;
import com.google.inject.Singleton;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import java.util.Collections;
import java.util.List;

@Singleton
public class PhantomJsProvider implements WebDriverProvider {

    public static final BrowserProfile BROWSER_PROFILE = new BrowserProfile(OperatingSystem.host(),
            BrowserVendor.CHROME, "phantom.js");
    
    private WebDriverPool pool = new WebDriverPool();

    public PhantomJsProvider() {
        pool = new WebDriverPool();
        pool.setMaxTotalSize(1);
        pool.setCreator(new Function<BrowserProfile, WebDriver>() {
            @Override
            public WebDriver apply(BrowserProfile input) {
                WebDriver driver = new PhantomJSDriver();
                driver.manage().window().setSize(new Dimension(1400,1000));

                return driver;
            }
        });
    }

    @Override
    public List<BrowserProfile> getSupportedProfiles() {
        return Collections.singletonList(BROWSER_PROFILE);
    }

    @Override
    public boolean supports(DeviceProfile profile) {
        if(!(profile instanceof BrowserProfile)) {
            return false;
        }
        BrowserProfile browser = (BrowserProfile) profile;
        switch(browser.getType()) {
            case CHROME:
            case SAFARI:
                return true;
            default:
                return false;
        }
    }

    @Override
    public WebDriver start(String name, BrowserProfile profile) {
        return pool.get(BROWSER_PROFILE);
    }

}
