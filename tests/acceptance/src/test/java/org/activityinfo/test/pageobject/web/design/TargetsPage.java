package org.activityinfo.test.pageobject.web.design;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.gxt.GxtGrid;
import org.activityinfo.test.pageobject.gxt.GxtModal;
import org.activityinfo.test.pageobject.gxt.ToolbarMenu;
import org.activityinfo.test.pageobject.gxt.GxtTree;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import static org.activityinfo.test.pageobject.api.XPathBuilder.withText;


public class TargetsPage {

    private ToolbarMenu toolbarMenu;
    private FluentElement container;

    public TargetsPage(FluentElement container) {
        this.container = container;
        this.toolbarMenu = new ToolbarMenu(container.findElement(By.className("x-toolbar-ct")));;
    }

    public GxtModal add() {
        container.waitUntil(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {
                container.find().button(withText("Add")).first().click();
                return container.root().exists(By.className(GxtModal.CLASS_NAME));
            }
        });
        return new GxtModal(container);
    }

    public ToolbarMenu getToolbarMenu() {
        return toolbarMenu;
    }

    public GxtGrid targetGrid() {
        return GxtGrid.findGrids(container).get(0);
    }
    
    public GxtGrid valueGrid() {
        return GxtGrid.findGrids(container).get(1);
    }
    
    public void select(String targetName) {
        // Select the target from the upper grid
        GxtGrid grid = targetGrid();
        grid.waitUntilAtLeastOneRowIsLoaded();
        grid.findCell(targetName).click();
    }
    
    public void setValue(String indicatorName, String value) {
        expandTree(indicatorName); // expand tree first
        GxtGrid.GxtCell cell = valueGrid().findCell(indicatorName, "value");
        cell.edit(value);
    }
    
    public void setValue(String indicatorName, Double value) {
        expandTree(indicatorName); // expand tree first
        valueGrid().findCell(indicatorName, "value").edit(Double.toString(value));
    }

    /**
     * 
     * @return true if the user is currently visiting the targets page
     */
    public boolean isCurrentPage() {
        return container.getCurrentUri().getPath().contains("#targets/");
    }

    public void expandTree(String indicatorName) {
        GxtTree tree = GxtTree.treeGrid(container);
        try {
            tree.waitUntil(new Predicate<GxtTree>() {
                @Override
                public boolean apply(GxtTree tree) {
                    Optional<GxtTree.GxtNode> root = tree.firstRootNode();
                    boolean loaded = root.isPresent() && root.get().joint().firstIfPresent().isPresent();
                    return !loaded;
                }
            });
            tree.search(indicatorName).get().select();
        } catch (WebDriverException e) { // revisit it later
            // unknown error: cannot focus element on key down
        }
        Preconditions.checkState(tree.firstRootNode().get().joint().firstIfPresent().isPresent());
    }
}

