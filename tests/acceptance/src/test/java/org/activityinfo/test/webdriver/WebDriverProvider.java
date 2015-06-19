package org.activityinfo.test.webdriver;


import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.BrowserType;

import java.util.List;

public interface WebDriverProvider {
    
    List<? extends DeviceProfile> getSupportedProfiles();
    
    boolean supports(DeviceProfile profile);

    WebDriver start(String name, BrowserProfile profile);

}
