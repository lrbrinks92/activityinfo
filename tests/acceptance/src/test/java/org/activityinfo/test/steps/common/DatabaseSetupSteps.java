package org.activityinfo.test.steps.common;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mysql.jdbc.StringUtils;
import cucumber.api.DataTable;
import cucumber.api.java.After;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.java.guice.ScenarioScoped;
import org.activityinfo.model.calc.AggregationMethod;
import org.activityinfo.model.type.FieldTypeClass;
import org.activityinfo.model.type.TypeRegistry;
import org.activityinfo.model.type.enumerated.EnumType;
import org.activityinfo.model.type.primitive.TextType;
import org.activityinfo.test.driver.*;
import org.activityinfo.test.driver.model.IndicatorLink;
import org.activityinfo.test.sut.Accounts;
import org.activityinfo.test.sut.UserAccount;

import javax.inject.Inject;
import java.util.*;

import static java.lang.String.format;
import static org.activityinfo.test.driver.Property.name;
import static org.activityinfo.test.driver.Property.property;

@ScenarioScoped
public class DatabaseSetupSteps {

    @Inject
    private ApplicationDriver driver;
    
    @Inject
    private Accounts accounts;
    
    private String currentDatabase;
    private String currentForm;
    
    private int targetIndex = 1;

    @After
    public final void cleanUp() throws Exception {
        driver.cleanup();
    }
    
    @Given("I have created a database \"(.*)\"")
    public void createDatabase(String databaseName) throws Exception {
        UserAccount account = accounts.any();
        driver.login(account);
        driver.setup().createDatabase(name(databaseName));
        this.currentDatabase = databaseName;
    }

    @Given("I have created a database \"(.*)\" in (.+)")
    public void createDatabase(String databaseName, String countryName) throws Exception {
        UserAccount account = accounts.any();
        driver.login(account);
        driver.setup().createDatabase(name(databaseName), property("country", countryName));
        this.currentDatabase = databaseName;
    }

    @Given("^I have created a form named \"(.*)\" in \"(.*)\"$")
    public void I_have_created_a_form_named_in(String formName, String databaseName) throws Throwable {
        driver.setup().createForm(name(formName), property("database", databaseName));
        
        this.currentForm = formName;
    }

    @Given("^I have created a published form named \"([^\"]*)\"$")
    public void I_have_created_a_published_form_named(String formName) throws Throwable {
        driver.setup().createForm(
                name(formName),
                property("database", getCurrentDatabase()),
                property("published", 1) // all are published
        );

        this.currentForm = formName;
    }

    @Given("^I have created a monthly form named \"([^\"]*)\"$")
    public void I_have_created_a_monthly_form_named(String formName) throws Throwable {
        driver.setup().createForm(name(formName),
                property("database", currentDatabase),
                property("reportingFrequency", "monthly"));
        
        currentForm = formName;

    }

    @Given("^I have created a form named \"([^\"]*)\" with location type \"([^\"]*)\"$")
    public void I_have_created_a_form_named_with_location_type(String name, String locationType) throws Throwable {
        driver.setup().createForm(
                name(name),
                property("locationType", locationType),
                property("database", getCurrentDatabase()));
    }

    @Given("^I have created a form named \"([^\"]*)\"$")
    public void I_have_created_a_form_named(String formName) throws Throwable {
        I_have_created_a_form_named_in(formName, getCurrentDatabase());
    }


    @Given("^I have created a form named \"([^\"]*)\" with the submissions:$")
    public void I_have_created_a_form_named_with_the_submissions(String formName, DataTable dataTable) throws Throwable {

        // Create the form
        I_have_created_a_form_named_in(formName, getCurrentDatabase());

        // Create each of the fields as a column
        List<String> columns = dataTable.getGherkinRows().get(0).getCells();
        Map<Integer, FieldTypeClass> columnTypeMap = Maps.newHashMap();
        for (int i = 0; i != columns.size(); ++i) {
            Optional<FieldTypeClass> columnType = createFieldForColumn(formName, dataTable, i);
            if (columnType.isPresent()) {
                columnTypeMap.put(i, columnType.get());
            }
        }
        
        // Submit the forms
        for (int row = 2; row < dataTable.getGherkinRows().size(); ++row) {
            submitRow(formName, dataTable, row, columnTypeMap);
        }
    }

    private static String resolveTypeName(String type) {
        if (type.equalsIgnoreCase("enum")) { // trick to not write long work enumerated in *.feature file
            type = EnumType.TYPE_CLASS.getId();
        } else if (type.equalsIgnoreCase("text")) { // trick to not write long work free_text in *.feature file
            type = TextType.TYPE_CLASS.getId();
        }
        return type;
    }

    /**
     * Creates form field from table column. Returns column type (field type).
     * @param form form name
     * @param dataTable table
     * @param columnIndex column index
     * @return column type (field type)
     * @throws Exception
     */
    private Optional<FieldTypeClass> createFieldForColumn(String form, DataTable dataTable, int columnIndex) throws Exception {
        String label = dataTable.getGherkinRows().get(0).getCells().get(columnIndex);
        if (isPredefinedField(label)) {
            return Optional.absent();
        }

        String type = resolveTypeName(dataTable.getGherkinRows().get(1).getCells().get(columnIndex));

        FieldTypeClass typeClass = TypeRegistry.get().getTypeClass(type);

        // Find set of distinct values
        Set<String> values = new HashSet<>();
        for (int row = 2; row < dataTable.getGherkinRows().size(); ++row) {
            String cellValue = dataTable.getGherkinRows().get(row).getCells().get(columnIndex);
            if (typeClass == EnumType.TYPE_CLASS) {
                values.addAll(StringUtils.split(cellValue, ",", true));
            } else {
                values.add(cellValue);
            }
        }

            if (typeClass == EnumType.TYPE_CLASS) {
                I_have_created_a_enumerated_field_with_options(label, Lists.newArrayList(values));
            } else {
                driver.setup().createField(
                        property("form", form),
                        property("name", label),
                        property("type", typeClass.getId()));
            }
        return Optional.of(typeClass);
    }

    private boolean isPredefinedField(String label) {
        return label.equalsIgnoreCase("partner") || label.equalsIgnoreCase("comments");
    }

    private void submitRow(String formName, DataTable dataTable, int row, Map<Integer, FieldTypeClass> columnTypeMap) throws Exception {
        List<String> headers = dataTable.getGherkinRows().get(0).getCells();
        List<String> columns = dataTable.getGherkinRows().get(row).getCells();
        List<FieldValue> fieldValues = new ArrayList<>();
        
        for(int column=0; column<columns.size();++column) {
            fieldValues.add(new FieldValue(headers.get(column), columns.get(column))
                    .setType(Optional.fromNullable(columnTypeMap.get(column))));
        }
        
        driver.setup().submitForm(formName, fieldValues, headers);
        
    }
    
    @Given("^I have created a (text|quantity) field \"([^\"]*)\" in \"([^\"]*)\"$")
    public void I_have_created_a_field_in(String fieldType, String fieldName, String formName) throws Throwable {
        driver.setup().createField(
                property("form", formName),
                property("name", fieldName),
                property("type", fieldType));
    }

    @And("^I have created a quantity field \"([^\"]*)\" in \"([^\"]*)\" with code \"([^\"]*)\"$")
    public void I_have_created_a_quantity_field_in_with_code(String fieldName, String formName, String fieldCode) throws Throwable {
        driver.setup().createField(
                property("form", formName),
                property("name", fieldName),
                property("type", "quantity"),
                property("code", fieldCode)
        );

        this.currentForm = formName;
    }

    @Given("^I have created a calculated field \"([^\"]*)\" in \"([^\"]*)\" with expression \"([^\"]*)\"$")
    public void I_have_created_a_calculated_field_in(String fieldName, String formName, String expression) throws Throwable {
        I_have_created_a_calculated_field_in_with_aggregation(fieldName, formName, expression, AggregationMethod.Sum.name());
    }

    @Given("^I have created a calculated field \"([^\"]*)\" in \"([^\"]*)\" with expression \"([^\"]*)\" with aggregation \"([^\"]*)\"$")
    public void I_have_created_a_calculated_field_in_with_aggregation(String fieldName, String formName, String expression, String aggregation) throws Throwable {
        driver.setup().createField(
                property("form", formName),
                property("name", fieldName),
                property("type", "quantity"),
                property("aggregation", AggregationMethod.valueOf(aggregation).code()),
                property("expression", expression),
                property("calculatedAutomatically", true));
    }


    @Given("^I have created a (text|quantity) field \"([^\"]*)\"$")
    public void I_have_created_a_field_in(String fieldType, String fieldName) throws Throwable {
        Preconditions.checkState(currentForm != null, "Create a form first");

        I_have_created_a_field_in(fieldType, fieldName, currentForm);
    }

    @Given("^I have created a enumerated field \"([^\"]*)\" with items:$")
    public void I_have_created_a_enumerated_field_with_options(String fieldName, List<String> items) throws Exception {
        Preconditions.checkState(currentForm != null, "No current form");

        driver.setup().createField(
                property("form", currentForm),
                property("name", fieldName),
                property("type", "enumerated"),
                property("items", items));
    }

    @Given("^I have submitted a \"([^\"]*)\" form with:$")
    public void I_have_submitted_a_form_with(String formName, List<FieldValue> values) throws Throwable {
        driver.setup().submitForm(formName, values);
    }


    @Given("^I have submitted a \"([^\"]*)\" form with partner (.+) with monthly reports:$")
    public void I_have_submitted_a_form_with_partner(String formName, String partner, DataTable dataTable) throws Throwable {

        List<String> headers = dataTable.getGherkinRows().get(0).getCells();
        if(!headers.get(0).equalsIgnoreCase("month")) {
            throw new AssertionError(format("First column of table must be the 'month', found: %s", headers.get(0)));
        }
        
        List<MonthlyFieldValue> fieldValues = new ArrayList<>();
        
        for(int row=1;row < dataTable.getGherkinRows().size();++row) {
            List<String> cells = dataTable.getGherkinRows().get(row).getCells();
            
            String parts[] = cells.get(0).split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            
            for(int column=1; column<cells.size();++column) {
                MonthlyFieldValue fieldValue = new MonthlyFieldValue();
                fieldValue.setMonth(month);
                fieldValue.setYear(year);
                fieldValue.setField(headers.get(column));
                fieldValue.setValue(cells.get(column));
                fieldValues.add(fieldValue);
            }
        }

        driver.setup().submitForm(formName, partner, fieldValues);
    }
    

    @Given("^I have added partner \"([^\"]*)\" to \"([^\"]*)\"$")
    public void I_have_added_partner_to(String partnerName, String databaseName) throws Throwable {
        driver.setup().addPartner(partnerName, databaseName);
    }

    @Given("^I have added partner \"([^\"]*)\"$")
    public void I_have_added_partner_to(String partnerName) throws Throwable {
        I_have_added_partner_to(partnerName, getCurrentDatabase());
    }

    @Given("^I have created the project \"([^\"]*)\"$")
    public void I_have_created_the_project(String project) throws Throwable {
        driver.setup().createProject(
                property("name", project),
                property("database", getCurrentDatabase()));
    }

    @Given("^I have created a target named \"([^\"]*)\" for database \"([^\"]*)\"$")
    public void I_have_created_a_target_named_for_database(String targetName, String database) throws Throwable {
        driver.setup().createTarget(
                property("database", database),
                property("name", targetName));
    }

    private String getCurrentDatabase() {
        Preconditions.checkState(currentDatabase != null, "There has been no database mentioned yet");
        return currentDatabase;
    }

    @When("^I create a target named \"([^\"]*)\" for database \"([^\"]*)\"$")
    public void I_create_a_target_named_for_database_with(String targetName, String databaseName) throws Throwable {
        driver.createTarget(
                property("database", databaseName),
                property("name", targetName));

    }

    @When("^I create a target named \"([^\"]*)\"$")
    public void I_create_a_target(String targetName) throws Throwable {
        driver.createTarget(
                property("database", getCurrentDatabase()),
                property("name", targetName));

    }
     
   
    @When("^I create a target named \"([^\"]*)\" for partner (.*) in database (.*)$")
    public void I_create_a_target_for_partner(String targetName, String partnerName, String databaseName) throws Throwable {
        driver.createTarget(
                property("database", databaseName),
                property("partner", partnerName),
                property("name", targetName));
    }


    @When("^I create a target named \"([^\"]*)\" for partner \"([^\"]*)\" and project \"([^\"]*)\"$")
    public void I_create_a_target_named_for_partner_and_project(String target, String partner, String project) throws Throwable {
        driver.createTarget(property("database", getCurrentDatabase()),
                property("name", target),
                property("partner", partner),
                property("project", project));
    }


    @When("^I create a target for project \"([^\"]*)\" with values:$")
    public void I_create_a_target_for_project_with_values(String project, List<FieldValue> values) throws Throwable {
        String targetName = nextTargetName();

        driver.createTarget(
                property("name", targetName),
                property("database", getCurrentDatabase()),
                property("project", project));
        driver.setTargetValues(targetName, values);

    }

    @When("^I set the targets of \"(.*)\" to:$")
    public void I_set_the_targets_of_to(String targetName, List<FieldValue> values) throws Throwable {
        driver.setTargetValues(targetName, values);
    }

    @When("^I create a target with values:$")
    public void I_create_a_target(List<FieldValue> targetValues) throws Throwable {
        String targetName = nextTargetName();
        driver.createTarget(
                property("database", getCurrentDatabase()),
                property("name", targetName));
        driver.setTargetValues(targetName, targetValues);
    }

    private String nextTargetName() {
        return "target" + (targetIndex++);
    }

    @When("^I create a target for partner (.*) with values:$")
    public void I_create_for_partner(String partnerName, List<FieldValue> targetValues) throws Throwable {
        String targetName = nextTargetName();
        driver.createTarget(
                property("database", getCurrentDatabase()),
                property("partner", partnerName),
                property("name", targetName));
        driver.setTargetValues(targetName, targetValues);
    }

    @Given("^I have granted ([^ ]+) permission to (.+) on behalf of \"(.+)\"$")
    public void I_have_granted_permission(String accountEmail, String permission, String partner) throws Throwable {
        driver.setup().grantPermission(
                property("database", getCurrentDatabase()),
                property("user", accountEmail),
                property("permissions", permission),
                property("partner", partner));    
    }

    @Given("^I have created a location type \"([^\"]*)\"$")
    public void I_have_created_a_location_type(String name) throws Throwable {
        driver.setup().createLocationType(
                property("database", getCurrentDatabase()),
                property("name", name));
    }

    @Given("^I have created a location \"([^\"]*)\" in \"([^\"]*)\" with code \"([^\"]*)\"$")
    public void I_have_created_a_location_in_with_code(String name, String locationType, String code) throws Throwable {
        driver.setup().createLocation(
                property("name", name),
                property("locationType", locationType),
                property("code", code));
    }

    @Then("^Location type \"(.*?)\" should be visible\\.$")
    public void location_type_should_be_visible(String locationTypeName) throws Throwable {
        driver.assertVisible(ObjectType.LOCATION_TYPE, true,
                new Property("name", locationTypeName),
                new Property("database", getCurrentDatabase())
        );
    }

    @Then("^Form \"(.*?)\" should be visible\\.$")
    public void form_should_be_visible(String formName) throws Throwable {
        driver.assertVisible(ObjectType.FORM, true,
                new Property("name", formName),
                new Property("database", getCurrentDatabase())
        );
    }

    @When("^I have removed the location type \"(.*?)\"$")
    public void i_have_removed_the_location_type_in(String locationTypeName) throws Throwable {
        driver.delete(ObjectType.LOCATION_TYPE,
                new Property("name", locationTypeName),
                new Property("database", getCurrentDatabase())
        );
    }

    @And("^I have removed the target \"([^\"]*)\"$")
    public void I_have_removed_the_target(String targetName) throws Throwable {
        driver.delete(ObjectType.TARGET,
                new Property("name", targetName),
                new Property("database", getCurrentDatabase())
        );
    }

    @Then("^Location type \"(.*?)\" is no longer visible\\.$")
    public void location_type_should_disappear_from_tree(String locationTypeName) throws Throwable {
        driver.assertVisible(ObjectType.LOCATION_TYPE, false,
                new Property("name", locationTypeName),
                new Property("database", getCurrentDatabase())
        );
    }

    @Given("^I have imported (\\d+) locations into \"([^\"]*)\"$")
    public void I_have_imported_locations_into(int locationCount, String locationType) throws Throwable {
        for(int i=0;i<locationCount;++i) {
            driver.setup().createLocation(
                    property("locationType", locationType),
                    property("name", "Location " + i),
                    property("code", "LOC" + i));
        }
    }

    @And("^I open a new session as (.+)$")
    public void I_open_a_new_session_as_(String user) throws Throwable {
        driver.login(accounts.ensureAccountExists(user));
    }

    @Given("^I haven not defined any targets$")
    public void I_haven_not_defined_any_targets() throws Throwable {
        // noop!
    }

    @Then("^selecting target \"([^\"]*)\" shows:$")
    public void selecting_target_shows(String targetName, List<FieldValue> targetValues) throws Throwable {
        driver.assertTargetValues(targetName, targetValues);
    }

    @When("^I link indicators:$")
    public void I_link_indicators(List<IndicatorLink> linkedIndicatorRows) throws Throwable {
        driver.createLinkIndicators(linkedIndicatorRows);
    }

    @Then("^Linked indicators marked by icon:$")
    public void Linked_indicators_marked_by_icon(List<IndicatorLink> linkedIndicatorRows) throws Throwable {
        driver.assertLinkedIndicatorsMarked(linkedIndicatorRows, true);
    }

    @When("^selecting \"([^\"]*)\" as the source link database$")
    public void selecting_as_the_source_link_database(String databaseName) throws Throwable {
        driver.getLinkIndicatorPage().getSourceDb().findCell(driver.getAliasTable().getAlias(databaseName)).click();
    }

    @Then("^source indicator link database shows:$")
    public void source_indicator_link_database_shows(DataTable expectedTable) throws Throwable {
        DataTable dataTable = driver.getLinkIndicatorPage().getSourceIndicator().extractData(false);
        driver.setup().getAliasTable().deAlias(dataTable).unorderedDiff(expectedTable);
    }
}
