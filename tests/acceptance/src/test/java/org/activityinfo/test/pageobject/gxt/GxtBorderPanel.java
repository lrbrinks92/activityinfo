package org.activityinfo.test.pageobject.gxt;

import org.activityinfo.test.pageobject.api.Style;
import org.openqa.selenium.WebElement;

public class GxtBorderPanel {

    private WebElement webElement;
    private Style style;

    public GxtBorderPanel(WebElement webElement) {
        this.webElement = webElement;
        style = new Style(webElement.getAttribute("style"));
    }

    public Style getStyle() {
        return style;
    }
}
