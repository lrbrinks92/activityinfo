package org.activityinfo.test.steps.common;

import com.google.common.collect.Lists;
import cucumber.api.DataTable;
import cucumber.api.java.en.Then;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.activityinfo.test.driver.AliasTable;
import org.activityinfo.test.driver.ApplicationDriver;

import javax.inject.Inject;
import java.util.List;

@ScenarioScoped
public class AnalysisSteps {

    @Inject
    private ApplicationDriver driver;
    
    @Inject
    private AliasTable aliasTable;

    @Then("^aggregating the indicators (.*) by (.*) should yield:$")
    public void aggregating_the_indicators_by_should_yield(String indicators, String dimensions, DataTable expected) throws Throwable {
        DataTable actual = driver.pivotTable(parseStringList(indicators), parseStringList(dimensions));
        expected.unorderedDiff(aliasTable.deAlias(actual));
    }
   
    @Then("^aggregating the indicator \"([^\"]*)\" by (.*) should yield:$")
    public void aggregating_the_indicator_by_should_yield(String indicatorName, String dimensions, DataTable expected) throws Throwable {
        DataTable actual = driver.pivotTable(indicatorName, parseStringList(dimensions));
        expected.diff(aliasTable.deAlias(actual));
    }

    private List<String> parseStringList(String stringList) {
        String[] parts = stringList.split("and");
        List<String> strings = Lists.newArrayList();
        if(parts.length == 1) {
            strings.add(parts[0]);
        } else if(parts.length == 2) {
            String[] items = parts[0].split(",");
            for (String item : items) {
                strings.add(item.trim());
            }
            strings.add(parts[1].trim());
        }
        return strings;
    }

    @Then("^drill down on \"([^\"]*)\" should yield:$")
    public void drill_down_on_should_yield(String cellValue, DataTable expected) throws Throwable {
        DataTable actual = driver.drillDown(cellValue);
        expected.unorderedDiff(aliasTable.deAlias(actual));
    }
}
