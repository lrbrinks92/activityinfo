package org.activityinfo.test.ui;

import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.test.pageobject.gxt.GxtFormPanel;
import org.activityinfo.test.pageobject.web.ApplicationPage;
import org.activityinfo.test.pageobject.web.entry.DataEntryTab;
import org.activityinfo.test.pageobject.web.entry.GxtDataEntryDriver;
import org.activityinfo.test.pageobject.web.entry.LocationDialog;
import org.junit.Rule;
import org.junit.Test;

import static org.activityinfo.test.driver.Property.name;
import static org.activityinfo.test.driver.Property.property;


public class LocationDialogUiTest {

    public static final String CLUSTER_DATABASE = "NFI Cluster";
    public static final String DISTRIBUTION_FORM = "NFI Distribution";
    public static final String VILLAGE = "Village";

    @Rule
    public UiDriver driver = new UiDriver();


    @Test
    public void frenchCoordinates() throws Exception {

        driver.setLocale("fr");
        
        System.out.println(I18N.CONSTANTS.newSite());
        
        
        driver.loginAsAny();
        driver.setup().createDatabase(
                name(CLUSTER_DATABASE));
        
        driver.setup().createLocationType(
                name(VILLAGE), 
                property("database", CLUSTER_DATABASE),
                property("workflowId", "open"));
       
        driver.setup().createForm(name(DISTRIBUTION_FORM),
                property("database", CLUSTER_DATABASE),
                property("locationType", VILLAGE));

        ApplicationPage applicationPage = driver.applicationPage();
        DataEntryTab dataEntryTab = applicationPage.navigateToDataEntryTab();

        GxtDataEntryDriver dataEntry = dataEntryTab
                .navigateToForm(this.driver.alias(DISTRIBUTION_FORM))
                .newSubmission();

        LocationDialog locationDialog = dataEntry.getLocationDialog();

        locationDialog.locationInput().sendKeys("Mwenu Ditu");
        
        locationDialog.addNew();
        locationDialog.dragMarker(5, 5);

        GxtFormPanel.GxtField latitude = locationDialog.getFormPanel().findFieldByLabel(I18N.CONSTANTS.latitude());
        latitude.assertValid();
    }
    
}
