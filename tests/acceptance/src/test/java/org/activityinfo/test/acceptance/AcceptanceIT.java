package org.activityinfo.test.acceptance;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        format = {"pretty", "html:target/cucumber-html-report", "json:target/cucumber-report.json"},
        tags = "~@manual")
public class AcceptanceIT {



}
