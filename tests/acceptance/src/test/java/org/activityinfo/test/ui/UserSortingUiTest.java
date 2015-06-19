package org.activityinfo.test.ui;

import org.activityinfo.test.pageobject.gxt.GxtGrid;
import org.activityinfo.test.pageobject.web.ApplicationPage;
import org.junit.Rule;
import org.junit.Test;

import static org.activityinfo.test.driver.Property.property;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.junit.Assert.assertThat;


public class UserSortingUiTest {
    
    @Rule
    public UiDriver driver = new UiDriver();
    
    @Test
    public void test() throws Exception {

        String database = "My db";
        
        driver.loginAsAny();
        driver.setup().createDatabase(property("name", database));
        
        String[] partners = new String[] { "ARC", "NRC", "RI" };
        for(String partner : partners) {
            driver.setup().addPartner(partner, database);
        }

        for(int i=0;i<300;++i) {
            String name = ('A' + (i % 26)) + " Jones";
            String user = String.format("user%d@example.com", i);
            driver.setup().grantPermission(
                    property("name", name),
                    property("partner", partners[i % partners.length]),
                    property("database", database),
                    property("user", user),
                    property("permissions", "View"));
        }

        ApplicationPage app = driver.applicationPage();
        GxtGrid userGrid = app.navigateToDesignTab().selectDatabase(driver.alias(database)).users().grid();
        userGrid.waitUntilAtLeastOneRowIsLoaded();
        
        // Verify that partners are sorted over the three several pages
        // and not just within the current page
        userGrid.sortBy("partner");
        assertThat(userGrid.columnValues("partner"), everyItem(equalTo(driver.alias("ARC"))));
    }
}
