package org.activityinfo.test.pageobject.web;

import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.gxt.Gxt;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;


public class Dashboard {

    private final FluentElement container;

    public Dashboard(FluentElement container) {
        this.container = container;
    }
    public void assertAtLeastOnePortletIsVisible() {
        container.waitUntil(ExpectedConditions.visibilityOfElementLocated(By.className(Gxt.PORTLET)));
    }
}
