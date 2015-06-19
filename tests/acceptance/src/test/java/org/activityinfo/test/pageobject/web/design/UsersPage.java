package org.activityinfo.test.pageobject.web.design;

import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.gxt.GxtGrid;


public class UsersPage {

    private FluentElement container;

    public UsersPage(FluentElement container) {
        this.container = container;
    }


    public GxtGrid grid() {
        return GxtGrid.findGrids(container).get(0);
    }

    
}
