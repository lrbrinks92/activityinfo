package org.activityinfo.test.pageobject.gxt;
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
import org.activityinfo.test.pageobject.api.FluentElements;
import org.openqa.selenium.By;

/**
 * @author yuriyz on 04/02/2015.
 */
public class ToolbarMenu {

    private FluentElement menu;

    public ToolbarMenu(FluentElement menu) {
        this.menu = menu;
    }

    public void clickButton(String buttonName) {
        FluentElements buttons = menu.findElements(By.tagName("button"));
        for (FluentElement button : buttons) {
            if (button.text().equals(buttonName)) {
                button.clickWhenReady();
                return;
            }
        }
    }
}
