package org.activityinfo.test.pageobject.web.reports;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import cucumber.api.DataTable;
import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.gxt.*;

import java.util.List;

public class PivotTableEditor {
    
    private FluentElement container;

    public PivotTableEditor(FluentElement container) {
        this.container = container;
    }

    private GxtTree indicatorTree() {
        return GxtPanel.find(container, "Indicators").tree();
    }

    private GxtTree dimensionTree() {
        return GxtPanel.find(container, "Dimensions").tree();
    }

    private GxtGrid dataTable() {
        return GxtGrid.waitForGrids(container).first().get();
    }


    public void selectMeasure(String name) {
        Optional<GxtTree.GxtNode> node = indicatorTree().waitUntilLoaded().search(name);
        if(!node.isPresent()) {
            throw new AssertionError("No such measure '" + name + "'");
        }
        node.get().setChecked(true);
    }


    public void selectDimensions(List<String> rowDimensions, List<String> columnDimensions) {
        dimensionTree().setChecked(Iterables.concat(rowDimensions, columnDimensions));
    }
    
    public DataTable extractData() {

        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Gxt.waitUntilMaskDisappear(container);
        GxtGrid grid = dataTable();
        return grid.extractData();
    }

    public DrillDownDialog drillDown(String cellText) {
        dataTable().findCell(cellText).doubleClick();
        return new DrillDownDialog(container);
    }

    public GxtModal clickButton(String buttonName) {
        Gxt.buttonClick(container, buttonName);

        GxtModal modal = GxtModal.waitForModal(container);
        if (modal.getTitle().equals("Save")) {
            GxtFormPanel.GxtField field = new GxtFormPanel.GxtField(modal.getWindowElement());
            field.fill("Report");
            modal.accept(I18N.CONSTANTS.ok());

            Gxt.buttonClick(container, buttonName);
            modal = GxtModal.waitForModal(container); // renew
        }

        return modal;
    }

    public ReportEditorBar reportBar() {
        return new ReportEditorBar(container);
    }
}
