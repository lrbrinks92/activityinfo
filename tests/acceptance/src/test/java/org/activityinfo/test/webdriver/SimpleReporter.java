package org.activityinfo.test.webdriver;

import com.google.common.io.Files;
import cucumber.api.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import javax.inject.Inject;
import java.io.File;

public class SimpleReporter implements SessionReporter {
    
    private WebDriverSession session;
    private static int SCREENSHOT_INDEX = 1;
    private Scenario scenario;

    @Inject
    public SimpleReporter(WebDriverSession session) {
        this.session = session;
    }

    @Override
    public void start(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    public void finished(Scenario scenario) {
        if(scenario.isFailed()) {
            screenshot();
        }
    }

    @Override
    public void screenshot() {
        try {
            WebDriver driver = session.getDriver();
            if (driver instanceof TakesScreenshot) {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                scenario.embed(screenshot, "image/png");
            }
        } catch(Exception e) {
            System.out.println("Exception saving screenshot: " + e.getMessage());
        }
    }


    private File targetDir() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath+"../../target");
        if(!targetDir.exists()) {
            targetDir.mkdir();
        }
        return targetDir;
    }
}
