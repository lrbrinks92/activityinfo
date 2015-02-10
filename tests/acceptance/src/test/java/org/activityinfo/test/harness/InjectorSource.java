package org.activityinfo.test.harness;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import cucumber.runtime.java.guice.ScenarioScope;
import cucumber.runtime.java.guice.impl.ScenarioModule;
import cucumber.runtime.java.guice.impl.SequentialScenarioScope;
import org.activityinfo.test.config.ConfigProperty;
import org.activityinfo.test.sut.SystemUnderTest;
import org.activityinfo.test.webdriver.*;
import org.activityinfo.test.sut.Accounts;
import org.activityinfo.test.sut.Server;
import org.openqa.selenium.WebDriver;

import static org.activityinfo.test.sut.PredefinedAccounts.fromResource;


public class InjectorSource implements cucumber.runtime.java.guice.InjectorSource {

    private ScenarioScope scope = new SequentialScenarioScope();

    @Override
    public Injector getInjector() {
        return Guice.createInjector(
                new SystemUnderTest(), 
                new WebDriverModule(), 
                new ScenarioModule(scope));
    }
}
