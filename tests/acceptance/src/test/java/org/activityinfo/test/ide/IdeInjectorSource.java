package org.activityinfo.test.ide;

import com.google.inject.Guice;
import com.google.inject.Injector;
import cucumber.runtime.java.guice.ScenarioScope;
import cucumber.runtime.java.guice.impl.ScenarioModule;
import cucumber.runtime.java.guice.impl.SequentialScenarioScope;
import org.activityinfo.test.driver.DriverModule;
import org.activityinfo.test.sut.SystemUnderTest;
import org.activityinfo.test.webdriver.WebDriverModule;


/**
 * A Cucumber InjectorSource that will be automatically loaded by Cucumber's framework. This allows
 * individual features to be run from an IDE like IntelliJ.
 * 
 * The Acceptance Test Suite explicitly provides an Injector for each of the different permutations
 * of functional tests.
 */
public class IdeInjectorSource implements cucumber.runtime.java.guice.InjectorSource {

    private ScenarioScope scope = new SequentialScenarioScope();

    @Override
    public Injector getInjector() {
        return Guice.createInjector(
                new SystemUnderTest(), 
                new WebDriverModule(), 
                new DriverModule(),
                new ScenarioModule(scope));
    }
}
