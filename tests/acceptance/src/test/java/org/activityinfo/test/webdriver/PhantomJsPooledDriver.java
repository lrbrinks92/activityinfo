package org.activityinfo.test.webdriver;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.openqa.selenium.support.events.EventFiringWebDriver;


public class PhantomJsPooledDriver extends EventFiringWebDriver implements WebDriverConnection {

    private GenericObjectPool<PhantomJsInstance> pool;
    private PhantomJsInstance instance;

    public PhantomJsPooledDriver(GenericObjectPool<PhantomJsInstance> pool, PhantomJsInstance instance) {
        super(instance.getWebDriver());
        this.pool = pool;
        this.instance = instance;
    }

    @Override
    public void quit() {
        pool.returnObject(instance);
    }

    @Override
    public void setConnected(boolean connected) {
        
    }
}
