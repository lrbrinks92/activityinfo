package org.activityinfo.test.webdriver;

import com.google.inject.Singleton;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.openqa.selenium.WebDriver;

import java.util.Collections;
import java.util.List;

@Singleton
public class PhantomJsProvider implements WebDriverProvider {

    public static final BrowserProfile BROWSER_PROFILE = new BrowserProfile(OperatingSystem.host(),
            BrowserVendor.CHROME, "phantom.js");

    private final GenericObjectPool<PhantomJsInstance> pool;

    public PhantomJsProvider() {
        pool = new GenericObjectPool<>(new PhantomJsFactory());
        pool.setMaxTotal(3);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                pool.close();
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
        try {
            return new PhantomJsPooledDriver(pool, pool.borrowObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
