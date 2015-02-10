package org.activityinfo.test.pageobject.gxt;


import org.openqa.selenium.By;

public class Gxt {


    public static By borderContainer() {
        return By.xpath("div[contains(@class, 'x-border-panel')]");
    }

    public static final String BORDER_LAYOUT_CONTAINER = "x-border-layout-ct";

    public static final String COLUMN_LAYOUT_CONTAINER = "x-column-layout-ct";

    public static final String PORTLET = "x-portlet";

}
