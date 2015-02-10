package org.activityinfo.test.pageobject.web;

import org.activityinfo.test.pageobject.api.PageObject;
import org.activityinfo.test.pageobject.gxt.Gxt;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.activityinfo.test.pageobject.api.WaitBuilder.anyElement;
import static org.activityinfo.test.pageobject.api.WaitBuilder.withClass;


public class Dashboard extends PageObject {

    @FindBy(className = Gxt.COLUMN_LAYOUT_CONTAINER)
    private WebElement container;

    public void assertThatAtLeastOnePortletIsVisible() {
        waitFor(anyElement(withClass(Gxt.PORTLET)));
    }
}
