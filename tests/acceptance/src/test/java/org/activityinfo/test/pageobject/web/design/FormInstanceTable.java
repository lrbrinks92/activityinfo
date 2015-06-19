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

import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.gxt.ToolbarMenu;
import org.openqa.selenium.By;

/**
 * @author yuriyz on 05/13/2015.
 */
public class FormInstanceTable {

    private final FluentElement container;
    private ToolbarMenu toolbarMenu;

    public FormInstanceTable(FluentElement container) {
        this.container = container;

        toolbarMenu = new ToolbarMenu(container.findElement(By.className("cellTableHeader")));
    }

    public ToolbarMenu getToolbarMenu() {
        return toolbarMenu;
    }
}
