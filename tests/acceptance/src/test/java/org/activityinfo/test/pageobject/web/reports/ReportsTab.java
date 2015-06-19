package org.activityinfo.test.pageobject.web.reports;

import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.gxt.GalleryView;

public class ReportsTab {
    private FluentElement container;

    public ReportsTab(FluentElement container) {
        this.container = container;
    }


    public PivotTableEditor createPivotTable() {
        GalleryView.find(container).select("Pivot Tables");
        return new PivotTableEditor(container);
    }
}
