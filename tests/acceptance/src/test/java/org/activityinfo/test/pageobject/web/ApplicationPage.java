package org.activityinfo.test.pageobject.web;

import com.google.common.base.Optional;
import org.activityinfo.test.pageobject.api.PageObject;
import org.activityinfo.test.pageobject.api.Path;
import org.activityinfo.test.pageobject.gxt.Gxt;
import org.activityinfo.test.webdriver.SessionReporter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Interface to the single-pageobject application
 */
@Path("/")
public class ApplicationPage extends PageObject {

    @FindBy(xpath = "//div[text() = 'ActivityInfo']/following-sibling::div[2]")
    private WebElement settingsButton;
    
    /**
     * The outermost pageobject container
     */
    @FindBy(className = Gxt.BORDER_LAYOUT_CONTAINER)
    private WebElement pageContainer;

    @Inject
    private SessionReporter logger;

    public <T> T assertCurrentPageIs(Class<T> applicationPageClass) {
        return binder.create(pageContainer,  applicationPageClass);
    }
    
    public SettingsMenu openSettingsMenu() {
        logger.screenshot();
        try {
            settingsButton.click();
        } catch(Exception ignored) {
            
        }
        return binder.waitFor(SettingsMenu.class);
    }
    
    public OfflineMode getOfflineMode() {
        
        return waitFor("offline status", 30, new Callable<Optional<OfflineMode>>() {
            @Override
            public Optional<OfflineMode> call() throws Exception {
                List<WebElement> elements = driver.findElements(By.className("x-status-text"));
                for(WebElement element : elements) {
                    if(element.getText().contains("Working online") || element.getText().contains("Last sync")) {
                        return Optional.of(OfflineMode.ONLINE);

                    } else if(element.getText().contains("Working offline")) {
                        return Optional.of(OfflineMode.OFFLINE);
                    }
                }
                return Optional.absent();
            }
        });
    }

    public void assertOfflineModeLoads() {
        waitFor("offline status", 60 * 5, new Callable<Optional<Boolean>>() {
            @Override
            public Optional<Boolean> call() throws Exception {
                List<WebElement> elements = driver.findElements(By.className("x-status-text"));
                for (WebElement element : elements) {
                    if (element.getText().contains("Working offline")) { 
                        return Optional.of(true);

                    } else if (element.getText().contains("Sync error")) {
                        throw new AssertionError(element.getText());
                    } else if (element.getText().contains("%")) {
                        System.out.println(element.getText());
                    }
                }
                return Optional.absent();
            }
        });
    }
}
