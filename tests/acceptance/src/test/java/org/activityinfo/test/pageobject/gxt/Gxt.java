package org.activityinfo.test.pageobject.gxt;


import org.openqa.selenium.By;

public class Gxt {


    public static By borderContainer() {
        return By.xpath("div[contains(@class, 'x-border-panel')]");
    }

    public static final String BORDER_LAYOUT_CONTAINER = "x-border-layout-ct";

    public static final String COLUMN_LAYOUT_CONTAINER = "x-column-layout-ct";

    public static final String PORTLET = "x-portlet";
    
    public static final By WINDOW = By.className("x-window");
    
    public static By button(String label) {
        return By.xpath(String.format("//button[text() = '%s']", label));
    }

}
