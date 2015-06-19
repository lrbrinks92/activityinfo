package org.activityinfo.test.webdriver;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;


public class PhantomJsFactory implements PooledObjectFactory<PhantomJsInstance> {
    @Override
    public PooledObject<PhantomJsInstance> makeObject() throws Exception {
        PhantomJsInstance instance = new PhantomJsInstance();
        instance.start();
        
        return new DefaultPooledObject<>(instance);
    }

    @Override
    public boolean validateObject(PooledObject<PhantomJsInstance> p) {
        try {
            p.getObject().getWebDriver().getTitle();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<PhantomJsInstance> p) throws Exception {
        p.getObject().getWebDriver().navigate().to("about:blank");
        p.getObject().getWebDriver().manage().deleteAllCookies();
        p.getObject().clearHomeDir();
    }

    @Override
    public void passivateObject(PooledObject<PhantomJsInstance> p) throws Exception {
        p.getObject().getWebDriver().navigate().to("about:blank");
    }

    @Override
    public void destroyObject(PooledObject<PhantomJsInstance> p) throws Exception {
        p.getObject().stop();
    }

}
