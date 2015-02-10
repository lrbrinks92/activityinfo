package org.activityinfo.test.pageobject.web;

import org.activityinfo.test.pageobject.api.PageObject;
import org.activityinfo.test.pageobject.api.Path;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;


@Path("/")
public class SettingsMenu extends PageObject {
    
    @FindBy(xpath = "//div[contains(text(), 'offline')]")
    private WebElement offlineActionLink;
    
    @FindBy(xpath = "//div[contains(text(), 'Logout')]")
    private WebElement logoutLink;
    
    
    public void enableOfflineMode() {
        offlineActionLink.click();
    }
}
