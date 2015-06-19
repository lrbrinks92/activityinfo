package org.activityinfo.test.acceptance;

import com.google.inject.Guice;
import com.google.inject.Injector;
import cucumber.api.CucumberOptions;
import cucumber.runtime.java.guice.impl.ScenarioModule;
import cucumber.runtime.java.guice.impl.SequentialScenarioScope;
import org.activityinfo.test.driver.DriverModule;
import org.activityinfo.test.sut.SystemUnderTest;
import org.activityinfo.test.webdriver.WebDriverModule;
import org.junit.runner.RunWith;

/**
 * Runs Functional Tests against the API
 */
@RunWith(CucumberAndGuice.class)
@CucumberOptions(strict = true, tags = "@api",
    plugin = {
            "json:build/cucumber-report-api.json" },
    glue = {
        "org.activityinfo.test.steps.json",
        "org.activityinfo.test.steps.common" })
public class FunctionalApiAT {

    public static Injector getInjector() {
        return Guice.createInjector(
                new SystemUnderTest(),
                new DriverModule("api"),
                new WebDriverModule(),
                new ScenarioModule(new SequentialScenarioScope()));
    }
}
