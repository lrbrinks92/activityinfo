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

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.api.FluentElements;
import org.activityinfo.test.pageobject.web.components.Form;
import org.joda.time.LocalDate;
import org.openqa.selenium.By;

import java.util.List;

import static org.activityinfo.test.pageobject.api.XPathBuilder.withClass;
import static org.activityinfo.test.pageobject.api.XPathBuilder.withText;

/**
 * @author yuriyz on 05/12/2015.
 */
public class BsFormPanel extends Form {

    private final FluentElement form;
    private BsField current;

    public BsFormPanel(FluentElement form) {
        this.form = form;
    }

    @Override
    public FormItem findFieldByLabel(String labelText) {
        Optional<FluentElement> element = form.find().label(withText(labelText)).ancestor().div(withClass("form-group")).firstIfPresent();
        if (element.isPresent()) {
            return new BsField(element.get());
        }

        element = form.find().label(withText(labelText)).ancestor().span(withClass("radio")).firstIfPresent();
        if (element.isPresent()) {
            return new BsField(element.get());
        }

        throw new AssertionError(String.format("The form panel has no field with label %s", labelText));
    }

    @Override
    public boolean moveToNext() {
        return false;
    }

    @Override
    public FormItem current() {
        return current;
    }

    public class BsField implements Form.FormItem {

        private final FluentElement element;

        public BsField(FluentElement element) {
            this.element = element;
        }

        @Override
        public String getLabel() {
            return element.findElement(By.tagName("label")).text();
        }

        @Override
        public boolean isDropDown() {
            return element.exists(By.tagName("a"));
        }

        @Override
        public void fill(String value) {
            FluentElement input = element.findElement(By.tagName("input"));
            input.element().clear();
            input.sendKeys(value);
        }

        @Override
        public void fill(LocalDate date) {
            fill(date.toString("M/d/YY"));
        }

        private FluentElements items() {
            final FluentElements items;
            if (isDropDown()) {
                element.findElement(By.tagName("a")).click();

                FluentElement list = this.element.waitFor(By.tagName("ul"));
                items = list.findElements(By.tagName("li"));
            } else {
                items = element.findElements(By.tagName("label"));
            }
            return items;
        }

        public List<String> itemLabels() {
            List<String> itemLabels = Lists.newArrayList();

            for (FluentElement element : items()) {
                String text = element.text();
                itemLabels.add(text);
            }
            return itemLabels;
        }


        @Override
        public void select(String itemLabel) {
            final FluentElements items = items();

            List<String> itemLabels = Lists.newArrayList();
            for (FluentElement element : items) {
                String text = element.text();
                itemLabels.add(text);
                if (text.equalsIgnoreCase(itemLabel)) {
                    element.click();
                    return;
                }
            }

            // Report nice error message
            throw new AssertionError(String.format("Could not select '%s' from combo box '%s'. Options:\n%s",
                    itemLabel,
                    getLabel(),
                    Joiner.on("\n").join(itemLabels)));
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public FluentElement getElement() {
            return element;
        }


    }
}
