package org.activityinfo.test.webdriver;

import com.google.common.base.Function;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringWebDriver;


public class WebDriverPool {
    
    private GenericKeyedObjectPool<BrowserProfile, WebDriver> pool;
    private Function<BrowserProfile, WebDriver> creator;

    public WebDriverPool() {
        pool = new GenericKeyedObjectPool<>(new Factory());
        Runtime.getRuntime().addShutdownHook(new Thread() {
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
        return creator.apply(profile);
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
        }
    
        @Override
        public void passivateObject(BrowserProfile browserProfile, PooledObject<WebDriver> pooledObject) throws Exception {
    
        }
    }

    public class Wrapper extends EventFiringWebDriver {
    
        private BrowserProfile profile;
    
        public Wrapper(BrowserProfile profile, WebDriver driver) {
            super(driver);
            this.profile = profile;
        }
    
        @Override
        public void quit() {
            pool.returnObject(profile, getWrappedDriver());
        }
    }
}
