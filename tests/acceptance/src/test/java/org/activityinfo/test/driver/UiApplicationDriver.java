package org.activityinfo.test.driver;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import cucumber.api.DataTable;
import cucumber.runtime.java.guice.ScenarioScoped;
import gherkin.formatter.model.DataTableRow;
import org.activityinfo.test.driver.model.IndicatorLink;
import org.activityinfo.test.pageobject.bootstrap.BsFormPanel;
import org.activityinfo.test.pageobject.bootstrap.BsModal;
import org.activityinfo.test.pageobject.gxt.GxtGrid;
import org.activityinfo.test.pageobject.gxt.GxtModal;
import org.activityinfo.test.pageobject.gxt.GxtTree;
import org.activityinfo.test.pageobject.web.ApplicationPage;
import org.activityinfo.test.pageobject.web.LoginPage;
import org.activityinfo.test.pageobject.web.design.DesignPage;
import org.activityinfo.test.pageobject.web.design.DesignTab;
import org.activityinfo.test.pageobject.web.design.LinkIndicatorsPage;
import org.activityinfo.test.pageobject.web.design.TargetsPage;
import org.activityinfo.test.pageobject.web.entry.DataEntryTab;
import org.activityinfo.test.pageobject.web.entry.DetailsEntry;
import org.activityinfo.test.pageobject.web.entry.HistoryEntry;
import org.activityinfo.test.pageobject.web.reports.DrillDownDialog;
import org.activityinfo.test.pageobject.web.reports.PivotTableEditor;
import org.activityinfo.test.sut.UserAccount;
import org.joda.time.LocalDate;
import org.junit.Assert;

import javax.inject.Inject;
import java.io.File;
import java.util.*;


@ScenarioScoped
public class UiApplicationDriver extends ApplicationDriver {

    private ApiApplicationDriver apiDriver;
    private LoginPage loginPage;

    private AliasTable aliasTable;

    private UserAccount currentUser = null;

    private ApplicationPage applicationPage;
    private Object currentPage;
    
    private String currentDatabase;
    private String currentForm;
    
    @Inject
    public UiApplicationDriver(ApiApplicationDriver apiDriver,
                               LoginPage loginPage,
                               AliasTable aliasTable) {
        super(aliasTable);
        this.apiDriver = apiDriver;
        this.loginPage = loginPage;
        this.aliasTable = aliasTable;
    }

    @Override
    public void login() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void login(UserAccount account) {
        if (currentUser == null || !currentUser.getEmail().equals(account.getEmail())) {
            currentUser = account;
            applicationPage = null;

            // defer actually logging in until the first command so that we can
            // let the setup steps execute first
            this.currentUser = account;

            setup().login(account);
        }
    }

    public void ensureLoggedIn() {
        if (applicationPage == null) {
            applicationPage = loginPage.navigateTo().loginAs(currentUser).andExpectSuccess();
            applicationPage.waitUntilLoaded();
        }
    }

    @Override
    public ApplicationDriver setup() {
        return apiDriver;
    }


    @Override
    public void submitForm(String formName, List<FieldValue> values) throws Exception {
        ensureLoggedIn();

        currentForm = formName;

        Map<String, FieldValue> valueMap = FieldValue.toMap(values);

        DataEntryTab dataEntryTab = applicationPage.navigateToDataEntryTab();
        dataEntryTab.navigateToForm(aliasTable.getAlias(formName));

        DataEntryDriver driver = dataEntryTab.newSubmission();

        fillForm(valueMap, driver);

        driver.submit();
    }

    private void fillForm(Map<String, FieldValue> valueMap, DataEntryDriver driver) throws InterruptedException {
        while (driver.nextField()) {
            System.out.println("label = " + driver.getLabel());
            switch (driver.getLabel()) {
                case "Partner":
                    if (valueMap.containsKey("partner")) {
                        driver.select(aliasTable.getAlias(valueMap.get("partner").getValue()));
                    }
                    break;
                case "Start Date":
                    if (valueMap.containsKey("Start Date")) {
                        driver.fill(LocalDate.parse(valueMap.get("Start Date").getValue()));
                    } else {
                        driver.fill(new LocalDate(2014, 1, 1));
                    }
                    break;
                case "End Date":
                    if (valueMap.containsKey("End Date")) {
                        driver.fill(LocalDate.parse(valueMap.get("End Date").getValue()));
                    } else {
                        driver.fill(new LocalDate(2014, 1, 1));
                    }
                    break;
                case "Comments":
                    if (valueMap.containsKey("comments")) {
                        driver.fill(valueMap.get("comments").getValue());
                    }
                    break;
                default:
                    String testHandle = aliasTable.getTestHandleForAlias(driver.getLabel());
                    if (valueMap.containsKey(testHandle)) {
                        String value = valueMap.get(testHandle).getValue();
                        if (value.matches("^\\d+$")) {
                            driver.fill(value);
                        } else {
                            driver.fill(aliasTable.getAlias(value));
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void enableOfflineMode() {
        ensureLoggedIn();

        applicationPage
                .openSettingsMenu()
                .enableOfflineMode();

        applicationPage.assertOfflineModeLoads();
    }

    @Override
    public OfflineMode getCurrentOfflineMode() {
        ensureLoggedIn();

        return applicationPage.getOfflineMode();
    }

    @Override
    public void synchronize() {
        ensureLoggedIn();

        applicationPage.openSettingsMenu().synchronizeNow();
        applicationPage.assertOfflineModeLoads();
    }

    @Override
    public int countFormSubmissions(String formName) {
        ensureLoggedIn();

        DataEntryTab dataEntryTab = applicationPage.navigateToDataEntryTab();
        currentPage = dataEntryTab.navigateToForm(aliasTable.getAlias(formName));
        
        return dataEntryTab.getCurrentSiteCount();
    }

    @Override
    public File exportForm(String formName) {
        ensureLoggedIn();
        
        DataEntryTab dataEntryTab = applicationPage.navigateToDataEntryTab();
        currentPage = dataEntryTab.navigateToForm(aliasTable.getAlias(formName));
        
        return dataEntryTab.export();
    }

    @Override
    public List<HistoryEntry> getSubmissionHistory() {
        Preconditions.checkState(currentForm != null, "No current form");

        DataEntryTab dataEntryTab = applicationPage.navigateToDataEntryTab();
        currentPage = dataEntryTab.navigateToForm(aliasTable.getAlias(currentForm));
        
        dataEntryTab.selectSubmission(0);
        return dataEntryTab.changes();
    }

    @Override
    public DetailsEntry getDetails() {
        Preconditions.checkState(currentForm != null, "No current form");

        DataEntryTab dataEntryTab = applicationPage.navigateToDataEntryTab();
        currentPage = dataEntryTab.navigateToForm(aliasTable.getAlias(currentForm));
        dataEntryTab.selectSubmission(0);

        return dataEntryTab.details();
    }

    @Override
    public void cloneDatabase(TestObject testObject) {
        ensureLoggedIn();

        BsModal dialog = applicationPage.navigateToDesignTab().newDatabase();

        dialog.form().findFieldByLabel("Use an existing database as a template").getElement().click();
        dialog.click("Next »", "Create a new database");

        dialog.form().findFieldByLabel("Choose a database to copy").select(testObject.getAlias("sourceDatabase"));
        dialog.click("Next »", "Create a new database");

        dialog.form().findFieldByLabel("Name").fill(testObject.getAlias("targetDatabase"));
        dialog.form().findFieldByLabel("Country").select("Rdc");
        dialog.form().findFieldByLabel("Options").select("Copy partners");
        dialog.form().findFieldByLabel("Options").select("Copy user permissions");
        dialog.click("Create", "Create a new database");
    }

    @Override
    public void updateSubmission(List<FieldValue> values) throws Exception {

        DataEntryTab dataEntryTab = applicationPage.navigateToDataEntryTab();
        currentPage = dataEntryTab.navigateToForm(aliasTable.getAlias(currentForm));

        dataEntryTab.selectSubmission(0);

        DataEntryDriver driver = dataEntryTab.updateSubmission();

        fillForm(FieldValue.toMap(values), driver);

        driver.submit();
    }

    private TargetsPage navigateToTargetSetupFor(String database) {
        ensureLoggedIn();

        currentDatabase = database;

        if(currentPage instanceof TargetsPage) {
            return (TargetsPage) currentPage;
        }
        
        return applicationPage
                    .navigateToDesignTab()
                    .selectDatabase(database)
                    .targets();
    }
    
    @Override
    public void createTarget(TestObject target) throws Exception {
        TargetsPage targetPage = navigateToTargetSetupFor(target.getAlias("database"));

        GxtModal dialog = targetPage.add();

        dialog.form().fillTextField("Name", target.getAlias());
        dialog.form().fillDateField("from", new LocalDate(2014, 1, 1));
        dialog.form().fillDateField("to", new LocalDate(2014, 12, 31));

        if (target.has("partner")) {
            dialog.form().select("Partner", target.getAlias("partner"));
        }
        if (target.has("project")) {
            dialog.form().select("Project", target.getAlias("project"));
        }

        dialog.accept();
    }


    @Override
    public void setTargetValues(String targetName, List<FieldValue> values) throws Exception {
        Preconditions.checkState(currentDatabase != null, "No current database");
        TargetsPage targetPage = navigateToTargetSetupFor(currentDatabase);
        targetPage.select(aliasTable.getAlias(targetName));
        for(FieldValue value : values) {
            targetPage.setValue(aliasTable.getAlias(value.getField()), value.getValue());   
        }
    }


    @Override
    public DataTable pivotTable(List<String> measures, List<String> rowDimension) {
        ensureLoggedIn();

        PivotTableEditor pivotTable = applicationPage
                .navigateToReportsTab()
                .createPivotTable();

        currentPage = pivotTable;

        for (String measure : measures) {
            pivotTable.selectMeasure(aliasTable.getAlias(measure));
        }

        pivotTable.selectDimensions(rowDimension, Collections.<String>emptyList());
        return pivotTable.extractData();
    }

    @Override
    public DataTable drillDown(String cellValue) {
        Preconditions.checkState(currentPage instanceof PivotTableEditor, "No pivot results. Please pivot data first before using drill down.");

        PivotTableEditor pivotTable = (PivotTableEditor) currentPage;
        DrillDownDialog drillDown = pivotTable.drillDown(cellValue);

        DataTable dataTable = drillDown.table().waitUntilReloadedSilently().extractData(false);
        drillDown.close();
        return dataTable;
    }

    @Override
    public void assertVisible(ObjectType objectType, boolean exists, TestObject testObject) {
        ensureLoggedIn();

        String name = testObject.getAlias("name");

        DesignTab designTab = applicationPage.navigateToDesignTab();
        designTab.selectDatabase(testObject.getAlias("database"));

        if (objectType == ObjectType.LOCATION_TYPE || objectType == ObjectType.FORM) {
            Optional<GxtTree.GxtNode> node = designTab.design().getDesignTree().search(name);

            if (exists) {
                Assert.assertTrue(objectType.name() + " with name '" + name + "' is not present.", node.isPresent());
            } else {
                Assert.assertTrue(objectType.name() + " with name '" + name + "' is present.", !node.isPresent());
            }
        } else if (objectType == ObjectType.PARTNER) {
            GxtGrid grid = designTab.partners().grid();
            grid.findCell(testObject.getAlias("name"));

        } else if (objectType == ObjectType.FORM_FIELD) {
            DesignPage designPage = designTab.design();
            designPage.getDesignTree().search(name).get().select();
            designPage.getToolbarMenu().clickButton("Open Table");

            designTab.formInstanceTable().getToolbarMenu().clickButton("New");

            BsModal dialog = designTab.formInstance();
            BsFormPanel.BsField bsField = (BsFormPanel.BsField) dialog.form().findFieldByLabel(testObject.getString("formFieldName"));

            List<String> items = testObject.getAliasList(testObject.getStringList("items"));
            if (!bsField.itemLabels().containsAll(items)) {
                throw new AssertionError("Not all elements formfield elements are present, expected: "
                        + Joiner.on(",").join(items) + ", actual: " + Joiner.on(",").join(bsField.itemLabels()));
            }
        }
    }

    public void assertTargetValues(String targetName, List<FieldValue> targetValues) {
        Preconditions.checkState(currentDatabase != null, "No current database");
        TargetsPage targetPage = navigateToTargetSetupFor(currentDatabase);
        targetPage.select(aliasTable.getAlias(targetName));

        for (FieldValue value : targetValues) {
            targetPage.expandTree(value.getField());
            targetPage.valueGrid().findCell(value.getValue());
        }
    }

    @Override
    public void delete(ObjectType objectType, TestObject testObject) throws Exception {
        switch (objectType) {
            case LOCATION_TYPE:
                deleteLocationType(testObject);
                break;
            case TARGET:
                deleteTarget(testObject);
                break;
            default:
                throw new IllegalArgumentException(String.format("Invalid object type '%s'", objectType));
        }
    }

    private void deleteTarget(TestObject testObject) {
        DesignTab designTab = applicationPage.navigateToDesignTab();
        designTab.selectDatabase(testObject.getAlias("database"));

        TargetsPage targetsPage = designTab.targets();
        targetsPage.select(testObject.getAlias("name"));
        targetsPage.getToolbarMenu().clickButton("Delete");

    }

    private void deleteLocationType(TestObject testObject) {
        DesignTab designTab = applicationPage.navigateToDesignTab();
        designTab.selectDatabase(testObject.getAlias("database"));

        DesignPage designPage = designTab.design();
        designPage.getDesignTree().select(testObject.getAlias("name"));
        designPage.getToolbarMenu().clickButton("Delete");
    }

    @Override
    public LinkIndicatorsPage getLinkIndicatorPage() {
        ensureLoggedIn();

        return applicationPage.navigateToDesignTab().linkIndicators();
    }

    @Override
    public void createLinkIndicators(List<IndicatorLink> linkedIndicatorRows) {
        LinkIndicatorsPage linkIndicatorsPage = getLinkIndicatorPage();
        linkIndicatorsPage.getSourceDb().waitUntilAtLeastOneRowIsLoaded();

        for (IndicatorLink row : linkedIndicatorRows) {

            linkIndicatorsPage.getSourceDb().findCell(aliasTable.getAlias(row.getSourceDb())).click();
            linkIndicatorsPage.getTargetDb().findCell(aliasTable.getAlias(row.getDestDb())).click();

            GxtGrid sourceIndicator = linkIndicatorsPage.getSourceIndicator().waitUntilAtLeastOneRowIsLoaded();
            GxtGrid targetIndicator = linkIndicatorsPage.getTargetIndicator().waitUntilAtLeastOneRowIsLoaded();

            sourceIndicator.findCell(aliasTable.getAlias(row.getSourceIndicator())).click();
            targetIndicator.findCell(aliasTable.getAlias(row.getDestIndicator())).click();

            linkIndicatorsPage.clickLinkButton();
        }
    }

    public void assertLinkedIndicatorsMarked(List<IndicatorLink> linkedIndicatorRows, boolean marked) {
        LinkIndicatorsPage linkIndicatorsPage = getLinkIndicatorPage();
        linkIndicatorsPage.getSourceDb().waitUntilAtLeastOneRowIsLoaded();

        for (IndicatorLink row : linkedIndicatorRows) {

            Preconditions.checkState(linkIndicatorsPage.getSourceDb().findCell(aliasTable.getAlias(row.getSourceDb())).hasIcon(), marked);
            Preconditions.checkState(linkIndicatorsPage.getTargetDb().findCell(aliasTable.getAlias(row.getDestDb())).hasIcon(), marked);

            GxtGrid sourceIndicator = linkIndicatorsPage.getSourceIndicator().waitUntilAtLeastOneRowIsLoaded();
            GxtGrid targetIndicator = linkIndicatorsPage.getTargetIndicator().waitUntilAtLeastOneRowIsLoaded();

            Preconditions.checkState(sourceIndicator.findCell(aliasTable.getAlias(row.getSourceIndicator())).hasIcon(), marked);
            Preconditions.checkState(targetIndicator.findCell(aliasTable.getAlias(row.getDestIndicator())).hasIcon(), marked);
        }
    }

    public void assertDataEntryTableForForm(String formName, DataTable expectedTable) {
        ensureLoggedIn();

        DataEntryTab dataEntryTab = applicationPage.navigateToDataEntryTab();
        currentPage = dataEntryTab.navigateToForm(aliasTable.getAlias(formName));

        List<DetailsEntry> detailsEntries = collectDetails(dataEntryTab);
        for (DetailsEntry entry : detailsEntries) {
            aliasTable.deAlias(entry.getFieldValues());
        }

        assertTableEquals(expectedTable, detailsEntries);
    }

    public static void assertTableEquals(DataTable expectedTable, List<DetailsEntry> detailsEntries) {
        List<DataTableRow> matchedRows = Lists.newArrayList();
        List<DetailsEntry> matchedDetailsEntries = Lists.newArrayList();
        for (int i = 1; i < expectedTable.getGherkinRows().size(); i++) {
            DataTableRow row = expectedTable.getGherkinRows().get(i);
            if (matchedRows.contains(row)) {
                continue;
            }
            for (DetailsEntry detailsEntry : detailsEntries) {
                if (matchedDetailsEntries.contains(detailsEntry)) {
                    continue;
                }
                if (equals(expectedTable.getGherkinRows().get(0).getCells(), row, detailsEntry.getFieldValues())) {
                    matchedRows.add(row);
                    matchedDetailsEntries.add(detailsEntry);
                    break;
                }
            }
        }

        if (matchedRows.size() != (expectedTable.getGherkinRows().size() - 1)) { // -1 because of first row is header
            List<DataTableRow> notMatched = Lists.newArrayList(expectedTable.getGherkinRows());
            notMatched.remove(expectedTable.getGherkinRows().get(0)); // remove header
            notMatched.removeAll(matchedRows);

            String notMatchedString = "";
            for (DataTableRow row : notMatched) {
                notMatchedString += Joiner.on(" | ").join(row.getCells()) + "\n";
            }

            String detailsEntriesString = "";
            for (DetailsEntry entry : detailsEntries) {
                detailsEntriesString += Joiner.on(" | ").join(entry.getFieldValues()) + "\n";
            }
            throw new AssertionError("Data entry table does not match. Expected: \n"
                    + expectedTable + "\n But got: \n" + detailsEntriesString + "\n Not matched rows:\n" + notMatchedString);
        }
    }

    public static boolean equals(List<String> columns, DataTableRow gherkinRow, List<FieldValue> values) {
        Set<Integer> matchedCellIndexes = Sets.newHashSet();
        for (FieldValue value : values) {

            for (int column = 0; column < gherkinRow.getCells().size(); column++) {
                if (matchedCellIndexes.contains(column)) {
                    continue;
                }

                String cell = gherkinRow.getCells().get(column);
                if (cell.isEmpty() ||
                        (cell.equals(value.getValue()) && columns.get(column).equals(value.getField()))) {
                    matchedCellIndexes.add(column);
                }
            }
        }

        return matchedCellIndexes.size() == gherkinRow.getCells().size();
    }

    private List<DetailsEntry> collectDetails(DataEntryTab dataEntryTab) {
        List<DetailsEntry> result = Lists.newArrayList();
        int row = 0;
        try {
            while (true) {
                dataEntryTab.selectSubmission(row);
                result.add(dataEntryTab.details());
                row++;
                if (row > 10000) { // safe escape
                    throw new AssertionError("Failed to fetch details for submissions on Data Entry tab.");
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            // no rows anymore
        }
        return result;
    }

    @Override
    public void cleanup() throws Exception {
        setup().cleanup();
    }
}
