package org.activityinfo.test.acceptance;

import com.google.inject.Guice;
import com.google.inject.Injector;
import cucumber.api.CucumberOptions;
import cucumber.runtime.java.guice.impl.ScenarioModule;
import cucumber.runtime.java.guice.impl.SequentialScenarioScope;
import org.activityinfo.test.driver.DriverModule;
import org.activityinfo.test.sut.SystemUnderTest;
import org.activityinfo.test.webdriver.SauceLabsDriverProvider;
import org.activityinfo.test.webdriver.WebDriverModule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.RunWith;

/**
 * Runs Functional Tests against the User Interface
 */
@RunWith(CucumberAndGuice.class)
@CucumberOptions(strict = true, tags = { "@web", "@cross-browser" },
        plugin = {
                "json:target/cucumber-report.json" },
        glue = {
                "org.activityinfo.test.steps.web",
                "org.activityinfo.test.steps.common" })
public class CrossBrowserAT {


    public static Injector getInjector() {
        
        if(!SauceLabsDriverProvider.isEnabled()) {
            throw new AssumptionViolatedException("Cross browser tests require Sauce Labs connection");
        }
        
        return Guice.createInjector(
                new SystemUnderTest(),
                new WebDriverModule(),
                new DriverModule("web"),
                new ScenarioModule(new SequentialScenarioScope()));
    }
}
