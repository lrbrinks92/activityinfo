package org.activityinfo.test.pageobject.web.design;
/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.gxt.GxtGrid;

/**
 * @author yuriyz on 06/03/2015.
 */
public class LinkIndicatorsPage {

    private static final int EXPECTED_GRIDS_COUNT = 4;

    private FluentElement container;

    public LinkIndicatorsPage(FluentElement container) {
        this.container = container;

        FluentIterable<GxtGrid> grids = GxtGrid.findGrids(container);
        Preconditions.checkState(grids.size() == EXPECTED_GRIDS_COUNT, "Failed to find link indicator grids. Grids found: " +
                grids.size() + " expected: " + EXPECTED_GRIDS_COUNT );
    }

    public void clickLinkButton() {
        container.find().button().clickWhenReady();
    }

    public GxtGrid getSourceDb() {
        return GxtGrid.findGrids(container).get(0);
    }

    public GxtGrid getSourceIndicator() {
        return GxtGrid.findGrids(container).get(2);
    }

    public GxtGrid getTargetDb() {
        return GxtGrid.findGrids(container).get(1);
    }

    public GxtGrid getTargetIndicator() {
        return GxtGrid.findGrids(container).get(3);
    }
}
