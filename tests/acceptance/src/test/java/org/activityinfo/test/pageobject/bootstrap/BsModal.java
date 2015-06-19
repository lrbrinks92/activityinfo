package org.activityinfo.test.pageobject.bootstrap;
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

import com.google.common.base.Predicate;
import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.api.XPathBuilder;
import org.activityinfo.test.pageobject.web.components.Form;
import org.activityinfo.test.pageobject.web.components.ModalDialog;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static org.activityinfo.test.pageobject.api.XPathBuilder.withClass;

/**
 * @author yuriyz on 05/12/2015.
 */
public class BsModal extends ModalDialog {

    public static final String CLASS_NAME = "modal-dialog";

    private FluentElement windowElement;

    public BsModal(FluentElement windowElement) {
        this.windowElement = windowElement;
    }

    public static BsModal find(FluentElement parent) {
        return new BsModal(parent.waitFor(By.className(CLASS_NAME)));
    }

    public String getTitle() {
        return windowElement.find().div(withClass("modal-header")).first().text();
    }

    @Override
    public Form form() {
        return new BsFormPanel(windowElement.find().div(withClass("modal-body")).first());
    }

    private FluentElement buttonsContainer() {
        return windowElement.find().div(withClass("modal-footer")).first();
    }

    public void click(String buttonName, final String expectedTitle) {
        buttonsContainer().find().button(XPathBuilder.withText(buttonName)).clickWhenReady();
        windowElement.waitUntil(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver input) {

                if (!windowElement.isDisplayed()) {  // is the box closed?
                    return false;
                }

                if (getTitle().contains(expectedTitle)) {
                    return true;
                }

                // check for an error ?

                return false;
            }
        });

    }

    @Override
    public void accept() {
        throw new UnsupportedOperationException();
    }

    public boolean isClosed() {
        return !windowElement.isDisplayed();
    }

}
