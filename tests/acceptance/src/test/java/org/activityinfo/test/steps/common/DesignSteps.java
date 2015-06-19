package org.activityinfo.test.steps.common;
/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.activityinfo.test.driver.ApplicationDriver;
import org.activityinfo.test.driver.ObjectType;
import org.activityinfo.test.driver.Property;
import org.activityinfo.test.driver.TestObject;

import javax.inject.Inject;
import java.util.List;

/**
 * @author yuriyz on 05/12/2015.
 */
@ScenarioScoped
public class DesignSteps {

    @Inject
    private ApplicationDriver driver;

    @When("^I have cloned a database \"([^\"]*)\" with name \"([^\"]*)\"$")
    public void I_have_cloned_a_database_with_name(String sourceDatabase, String targetDatabase) throws Throwable {
        driver.cloneDatabase(new TestObject(driver.getAliasTable(), new Property("sourceDatabase", sourceDatabase), new Property("targetDatabase", targetDatabase)));
    }

    @Then("^\"([^\"]*)\" database has \"([^\"]*)\" partner$")
    public void database_has_partner(String databaseName, String partnerName) throws Throwable {
        driver.assertVisible(ObjectType.PARTNER, true,
                new TestObject(driver.getAliasTable(), new Property("database", databaseName), new Property("name", partnerName)));
    }

    @Then("^\"([^\"]*)\" database has \"([^\"]*)\" form$")
    public void database_has_form(String databaseName, String formName) throws Throwable {
        driver.assertVisible(ObjectType.FORM, true,
                new Property("name", formName),
                new Property("database", databaseName)
        );
    }

    @Then("^\"([^\"]*)\" form has \"([^\"]*)\" form field with values in database \"([^\"]*)\":$")
    public void form_has_form_field_with_values(String formName, String formFieldName, String database, List<String> items) throws Throwable {
        driver.assertVisible(ObjectType.FORM_FIELD, true,
                new Property("name", formName),
                new Property("database", database),
                new Property("formFieldName", formFieldName),
                new Property("items", items));
    }
}
