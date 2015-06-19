package org.activityinfo.test.steps.web;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.activityinfo.test.driver.ApplicationDriver;
import org.activityinfo.test.pageobject.web.ApplicationPage;
import org.activityinfo.test.pageobject.web.LoginPage;
import org.activityinfo.test.sut.Accounts;
import org.activityinfo.test.sut.Server;
import org.activityinfo.test.sut.UserAccount;

import javax.inject.Inject;


@ScenarioScoped
public class LoginSteps {

    @Inject
    private Server server;

    @Inject
    private ApplicationPage applicationPage;

    @Inject
    private Accounts accounts;

    @Inject
    private LoginPage loginPage;

    @Inject
    private ApplicationDriver applicationDriver;
    

    @Given("^that the user \"([^\"]*)\" is not signed up$")
    public void that_the_user_is_not_signed_up(String email) throws Throwable {
        // NOOP for now
    }

    @Given("^that the user \"([^\"]*)\" is signed up$")
    public void that_the_user_is_signed_up(String email) throws Throwable {
        accounts.ensureAccountExists(email);
    }

    @When("^I login as \"([^\"]*)\" with my correct password$")
    public void I_login_as_with_my_password(String email) throws Throwable {
        UserAccount account = accounts.ensureAccountExists(email);

        loginPage.navigateTo().loginAs(account);
    }


    @When("^I login as \"([^\"]*)\" with password \"([^\"]*)\"$")
    public void I_login_as_with_password(String email, String password) throws Throwable {

        loginPage
            .navigateTo()
            .loginAs(new UserAccount(email, password));
    }

    @Then("^my dashboard should open$")
    public void my_dashboard_should_open() throws Throwable {
        applicationPage.dashboard().assertAtLeastOnePortletIsVisible();
    }

    @Then("^I should see an error alert$")
    public void I_should_see_an_error_alert() throws Throwable {
        loginPage.assertErrorMessageIsVisible();
    }

    @Given("^that I am logged in as \"([^\"]*)\"$")
    public void that_I_am_logged_in_as(String email) throws Throwable {
        UserAccount account = accounts.ensureAccountExists(email);
        loginPage
            .navigateTo()
            .loginAs(account)
            .andExpectSuccess();
    }

    @Then("^I should receive a message that my browser is not unsupported$")
    public void I_should_receive_a_message_that_my_browser_is_unsupported() throws Throwable {
        loginPage.assertBrowserUnsupportedPageIsVisible();
    }

    @When("^I logout$")
    public void I_logout() throws Throwable {
        applicationPage.openSettingsMenu().logout();
    }
}
