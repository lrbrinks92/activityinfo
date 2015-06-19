package org.activityinfo.test.pageobject.gxt;

import org.activityinfo.test.pageobject.api.FluentElement;
import org.openqa.selenium.By;

import static org.activityinfo.test.pageobject.api.XPathBuilder.withClass;
import static org.activityinfo.test.pageobject.api.XPathBuilder.withText;
import static org.activityinfo.test.pageobject.api.XPathBuilder.containingText;

public class GxtPanel {

    private final FluentElement panel;

    public static GxtPanel find(FluentElement container, String heading) {
        FluentElement panel = container.find().span(withText(heading)).ancestor().div(withClass("x-panel")).waitForFirst();
        return new GxtPanel(panel);
    }

    public static GxtPanel findStartsWith(FluentElement container, String headingStartsWith) {
        FluentElement panel = container.find().span(containingText(headingStartsWith)).ancestor().div(withClass("x-panel")).waitForFirst();
        return new GxtPanel(panel);
    }

    public GxtPanel(FluentElement panel) {
        this.panel = panel;
    }
    
    public GxtTree tree() {
        return GxtTree.tree(panel.findElement(By.className("x-tree3")));
    }

    public GxtTree treeGrid() {
        return GxtTree.treeGrid(panel.findElement(By.className("x-treegrid")));
    }

    public ToolbarMenu toolbarMenu() {
        return new ToolbarMenu(panel.findElement(By.className("x-toolbar-ct")));
    }
}
