package org.activityinfo.test.driver;


import com.google.common.collect.Lists;
import cucumber.api.DataTable;
import cucumber.api.PendingException;
import org.activityinfo.test.driver.model.IndicatorLink;
import org.activityinfo.test.pageobject.web.design.LinkIndicatorsPage;
import org.activityinfo.test.pageobject.web.entry.DetailsEntry;
import org.activityinfo.test.pageobject.web.entry.HistoryEntry;
import org.activityinfo.test.sut.UserAccount;
import org.json.JSONException;

import java.io.File;
import java.util.List;

public abstract class ApplicationDriver {

    private final AliasTable aliasTable;

    public ApplicationDriver(AliasTable aliasTable) {
        this.aliasTable = aliasTable;
    }

    /**
     * Login as any user
     */
    public abstract void login();
    
    public abstract void login(UserAccount account);

    /**
     * @return an implementation of ApplicationDriver suitable for setting up a test scenario
     */
    public ApplicationDriver setup() {
        return this;
    }
    
    public final void createDatabase(Property... properties) throws Exception {
        createDatabase(new TestObject(aliasTable, properties));
    }
    
    public void enableOfflineMode() {
        throw new UnsupportedOperationException();
    }
    
    public OfflineMode getCurrentOfflineMode() {
        throw new UnsupportedOperationException();
    }

    protected void createDatabase(TestObject database) throws Exception {
        throw new PendingException();
    }

    public final void createForm(Property... properties) throws Exception {
        createForm(new TestObject(aliasTable, properties));
    }

    protected void createForm(TestObject form) throws Exception {
        throw new PendingException();
    }

    public void createField(Property... properties) throws Exception {
        createField(new TestObject(aliasTable, properties));
    }

    public void createField(TestObject testObject) throws Exception {
        throw new PendingException();
    }

    public void submitForm(String formName, List<FieldValue> values) throws Exception {
        throw new PendingException();
    }

    public void submitForm(String formName, List<FieldValue> values, List<String> headers) throws Exception {
        throw new PendingException();
    }

    public void submitForm(String formName, String partner, List<MonthlyFieldValue> fieldValues) throws JSONException, Exception {
        throw new PendingException();
    }


    public void delete(ObjectType objectType, String name) throws Exception {
        throw new PendingException();
    }

    public void delete(ObjectType objectType, Property... properties) throws Exception {
        delete(objectType, new TestObject(aliasTable, properties));
    }

    public void delete(ObjectType objectType, TestObject testObject) throws Exception {
        throw new PendingException();
    }

    public void addPartner(String partnerName, String databaseName) throws  Exception {
        throw new PendingException();
    }

    public void createTarget(Property... properties) throws Exception {
        createTarget(new TestObject(aliasTable, properties));
    }

    protected void createTarget(TestObject target) throws Exception {
        throw new PendingException();
    }

    public void setTargetValues(String targetName, List<FieldValue> values) throws Exception {
        throw new PendingException();
    }

    public final void createProject(Property... properties) throws Exception {
        createProject(new TestObject(aliasTable, properties));
    }

    protected void createProject(TestObject project) throws Exception {
        throw new PendingException();
    }

    public DataTable pivotTable(String measure, List<String> rowDimensions) throws Exception {
        return pivotTable(Lists.newArrayList(measure), rowDimensions);
    }

    public DataTable pivotTable(List<String> measure, List<String> rowDimensions) throws Exception {
        throw new PendingException();
    }

    public final void grantPermission(Property... properties) throws Exception {
        grantPermission(new TestObject(aliasTable, properties));
    }

    public DataTable drillDown(String cellValue) {
        throw new PendingException();
    }

    protected void grantPermission(TestObject permission) throws Exception {
        throw new PendingException();
    }

    public void cleanup() throws Exception {
        
    }

    public final void createLocationType(Property... properties) throws Exception {
        createLocationType(new TestObject(aliasTable, properties));
    }

    protected void createLocationType(TestObject testObject) throws Exception {
        throw new PendingException();
    }

    public final void createLocation(Property... properties) throws Exception {
        createLocation(new TestObject(aliasTable, properties));
        
    }

    protected void createLocation(TestObject testObject) throws Exception {
        throw new PendingException();
    }

    public void assertVisible(ObjectType objectType, boolean exists, Property... properties) {
        assertVisible(objectType, exists, new TestObject(aliasTable, properties));
    }

    public void assertVisible(ObjectType objectType, boolean exists, TestObject testObject) {
        throw new PendingException();
    }

    public void synchronize() {
        throw new UnsupportedOperationException();
    }

    public int countFormSubmissions(String formName) {
        throw new PendingException();
    }

    public File exportForm(String formName) throws Exception {
        throw new PendingException();
    }
    
    public File exportDatabase(String databaseName) throws Exception {
        throw new PendingException();
    }

    /**
     * @return the text of the history of the last site we modified
     */
    public List<HistoryEntry> getSubmissionHistory() {
        throw new PendingException();
    }

    public void updateSubmission(List<FieldValue> values) throws InterruptedException, Exception {
        throw new PendingException();
    }

    public DetailsEntry getDetails() {
        throw new PendingException();
    }

    public AliasTable getAliasTable() {
        return aliasTable;
    }


    public void cloneDatabase(TestObject testObject) {
        throw new PendingException();
    }

    public void assertTargetValues(String targetName, List<FieldValue> targetValues) {
        throw new PendingException();
    }

    public LinkIndicatorsPage getLinkIndicatorPage() {
        throw new PendingException();
    }

    public void createLinkIndicators(List<IndicatorLink> linkedIndicatorRows) {
        throw new PendingException();
    }

    public void assertLinkedIndicatorsMarked(List<IndicatorLink> linkedIndicatorRows, boolean marked) {
        throw new PendingException();
    }

    public void assertDataEntryTableForForm(String formName, DataTable expectedTable) {
        throw new PendingException();
    }
}
