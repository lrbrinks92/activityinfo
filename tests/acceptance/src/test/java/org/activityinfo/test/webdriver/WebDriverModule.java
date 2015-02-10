package org.activityinfo.test.webdriver;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.openqa.selenium.WebDriver;


public class WebDriverModule extends AbstractModule {
    @Override
    protected void configure() {


        if(SauceLabsDriverProvider.isEnabled()) {
            System.out.println("Using SauceLabs as WebDriver");
            bind(WebDriverProvider.class).to(SauceLabsDriverProvider.class);
            bind(SessionReporter.class).to(SauceReporter.class);
        } else {
            System.out.println("Using PhantomJS as WebDriver");
            bind(WebDriverProvider.class).to(PhantomJsProvider.class);
            bind(SessionReporter.class).to(SimpleReporter.class);
        }

    }

    @ScenarioScoped 
    @Provides
    public WebDriver provideDriver(WebDriverSession session) {
        return session.getDriver();
    }
}
