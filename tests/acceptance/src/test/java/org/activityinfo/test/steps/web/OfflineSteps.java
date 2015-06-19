package org.activityinfo.test.steps.web;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.activityinfo.test.driver.ApplicationDriver;
import org.activityinfo.test.driver.OfflineMode;
import org.activityinfo.test.webdriver.ScreenShotLogger;
import org.activityinfo.test.webdriver.WebDriverSession;

import javax.inject.Inject;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@ScenarioScoped
public class OfflineSteps {

    @Inject
    private WebDriverSession session;

    @Inject
    private ApplicationDriver driver;


    @Inject
    private ScreenShotLogger logger;


    @Given("I have enabled offline mode$")
    public void I_have_enabled_offline_mode() {
        if(driver.getCurrentOfflineMode() != OfflineMode.OFFLINE) {
            driver.enableOfflineMode();
        }
    }


    @When("^I open the application without an internet connection$")
    public void I_open_the_application_without_an_internet_connection() throws Throwable {
        session.setConnected(false);
    }

    @When("^an internet connection becomes available$")
    public void an_internet_connection_becomes_available() throws Throwable {
        session.setConnected(true);
    }

    @And("^I synchronize with the server$")
    public void I_synchronize_with_the_server() throws Throwable {
        driver.synchronize();
    }

    @Then("^the application should be in offline mode$")
    public void the_application_should_be_in_offline_mode() throws Throwable {
        assertThat(driver.getCurrentOfflineMode(), equalTo(OfflineMode.OFFLINE));
    }
}
