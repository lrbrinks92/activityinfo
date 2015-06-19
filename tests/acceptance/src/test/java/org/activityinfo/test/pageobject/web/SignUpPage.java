package org.activityinfo.test.pageobject.web;


import com.google.common.base.Predicate;
import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.sut.Server;
import org.activityinfo.test.sut.UserAccount;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import javax.inject.Inject;

public class SignUpPage {


    public static final By NAME_INPUT = By.name("name");
    public static final By EMAIL_INPUT = By.name("email");
    public static final By ORGANIZATION_INPUT = By.name("organization");
    public static final By TITLE_INPUT = By.name("jobtitle");
    public static final By SUBMIT_BUTTON = By.xpath("//button[@type='submit']");

    private final Server server;
    private final FluentElement page;

    @Inject
    public SignUpPage(WebDriver webDriver, Server server) {
        this.page = new FluentElement(webDriver);
        this.server = server;
    }

    public SignUpPage navigateTo() {
        page.navigate().to(server.path("signUp"));
        return this;
    }

    public SignUpPage signUp(UserAccount account) {
        page.waitFor(NAME_INPUT).sendKeys(account.nameFromEmail());
        page.findElement(ORGANIZATION_INPUT).sendKeys(account.domainFromEmail());
        page.findElement(TITLE_INPUT).sendKeys("Specialist");
        page.findElement(EMAIL_INPUT).sendKeys(account.getEmail());
        page.findElement(SUBMIT_BUTTON).click();
        page.waitUntil(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                return input.getCurrentUrl().equals(server.path("signUp/sent"));
            }
        });
        return this;
    }
}
