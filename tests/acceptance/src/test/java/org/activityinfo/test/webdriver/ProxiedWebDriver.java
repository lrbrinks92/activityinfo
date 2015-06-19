package org.activityinfo.test.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringWebDriver;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ProxiedWebDriver extends EventFiringWebDriver implements WebDriverConnection {

    private static final Logger LOGGER = Logger.getLogger(ProxiedWebDriver.class.getName());
    
    private ProxyController proxyController;
    

    public ProxiedWebDriver(WebDriver driver, ProxyController proxyController) {
        super(driver);
        this.proxyController = proxyController;
    }

    @Override
    public void quit() {
        try {
            proxyController.stop();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to shutdown proxy: " + e.getMessage(), e);
        }
        super.quit();
    }

    @Override
    public void setConnected(boolean connected) {
        proxyController.setConnected(connected);
    }
}
