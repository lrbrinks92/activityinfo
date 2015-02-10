package org.activityinfo.test.pageobject.gxt;


import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class GxtBorderPanelContainer {

    private WebElement element;

    private WebElement north;
    private WebElement south;
    private WebElement east;
    private WebElement west;

    public GxtBorderPanelContainer(WebElement element) {
        this.element = element;
        List<WebElement> children = element.findElements(By.xpath("div[contains(@class, 'x-border-panel')]"));
        for (WebElement child : children) {
            String style = child.getAttribute("style");

        }
    }

}
