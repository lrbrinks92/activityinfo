package org.activityinfo.test.pageobject.web;

import org.activityinfo.test.pageobject.api.PageObject;
import org.activityinfo.test.pageobject.api.Path;
import org.activityinfo.test.sut.UserAccount;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.activityinfo.test.pageobject.api.WaitBuilder.anyElement;

@Path("login")
public class LoginPage extends PageObject {


    @FindBy(name="email")
    private WebElement emailInput;

    @FindBy(name="password")
    private WebElement passwordInput;

    @FindBy(id="loginButton")
    private WebElement loginButton;

    private static final By LOGIN_ERROR_MESSAGE = By.id("loginAlert");


    public LoginPage loginAs(UserAccount account) {
        emailInput.sendKeys(account.getEmail());
        passwordInput.sendKeys(account.getPassword());
        loginButton.click();
        return this;
    }

    public void andExpectSuccess() {
        waitFor(navigationTo(ApplicationPage.class)
                .butCheckForErrorMessage(LOGIN_ERROR_MESSAGE)
                .butFailIf(urlContains("unsupportedBrowser")));
    }


    public void assertErrorMessageIsVisible() {
        waitFor(anyElement(LOGIN_ERROR_MESSAGE)
                .butFailIf(browserNavigatesAway()));
    }
    
}
