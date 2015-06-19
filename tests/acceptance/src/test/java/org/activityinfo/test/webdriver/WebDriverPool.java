package org.activityinfo.test.webdriver;

import com.google.common.base.Function;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringWebDriver;

import java.util.concurrent.TimeUnit;

/**
 * Maintains a pool of WebDriver instances that can be reused
 */
public class WebDriverPool {
    
    private GenericKeyedObjectPool<BrowserProfile, WebDriver> pool;
    private Function<BrowserProfile, WebDriver> creator;

    public WebDriverPool() {
        pool = new GenericKeyedObjectPool<>(new Factory());
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                pool.close();
            }
        });
    }
    
    public void setMaxTotalSize(int size) {
        pool.setMaxTotal(size);
        pool.setMaxIdlePerKey(size);
    }

    public void setCreator(Function<BrowserProfile, WebDriver> creator) {
        this.creator = creator;
    }

    private WebDriver createDriver(BrowserProfile profile) {
        WebDriver driver = creator.apply(profile);
        configureDriver(driver);
        return driver;
    }

    private static void configureDriver(WebDriver driver) {
        // increase timeout before NoSuchElementException
        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
    }

    public WebDriver get(BrowserProfile profile) {
        try {
            return new Wrapper(profile, pool.borrowObject(profile));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public class Factory implements KeyedPooledObjectFactory<BrowserProfile, WebDriver> {
    
        @Override
        public PooledObject<WebDriver> makeObject(BrowserProfile browserProfile) throws Exception {
            return new DefaultPooledObject<>(createDriver(browserProfile));
        }
        
        @Override
        public void destroyObject(BrowserProfile browserProfile, PooledObject<WebDriver> pooledObject) throws Exception {
            pooledObject.getObject().quit();    
        }
    
        @Override
        public boolean validateObject(BrowserProfile browserProfile, PooledObject<WebDriver> pooledObject) {
            try {
                pooledObject.getObject().getTitle();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    
        @Override
        public void activateObject(BrowserProfile browserProfile, PooledObject<WebDriver> pooledObject) throws Exception {
            pooledObject.getObject().manage().deleteAllCookies();
            pooledObject.getObject().navigate().to("about:blank");
        }
    
        @Override
        public void passivateObject(BrowserProfile browserProfile, PooledObject<WebDriver> pooledObject) throws Exception {
    
        }
    }

    public class Wrapper extends EventFiringWebDriver implements WebDriverConnection {
    
        private BrowserProfile profile;
    
        public Wrapper(BrowserProfile profile, WebDriver driver) {
            super(driver);
            this.profile = profile;
        }
    
        @Override
        public void quit() {
            pool.returnObject(profile, getWrappedDriver());
        }

        @Override
        public void setConnected(boolean connected) {
            ((WebDriverConnection) getWrappedDriver()).setConnected(connected);
        }
    }
}
