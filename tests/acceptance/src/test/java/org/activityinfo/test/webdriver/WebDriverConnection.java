package org.activityinfo.test.webdriver;

import org.openqa.selenium.WebDriver;

/**
 * Provides control over the connection between the browser and 
 * the server.
 */
public interface WebDriverConnection extends WebDriver {
    
    public void setConnected(boolean connected);
    
    
}
