package org.activityinfo.test.pageobject.web.entry;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.web.components.Form;
import org.joda.time.LocalDate;
import org.openqa.selenium.Keys;

import static org.activityinfo.test.pageobject.api.XPathBuilder.withClass;


public class GxtIndicatorForm extends Form {

    private FluentElement container;
    private IndicatorField current;

    public GxtIndicatorForm(FluentElement container) {
        this.container = container;
    }
    
    @Override
    public boolean moveToNext() {
        
        if(current == null) {
            Optional<FluentElement> first = container.find().input().ancestor().tr().firstIfPresent();
            if (first.isPresent()) {
                current = new IndicatorField(first.get());
                current.focus();
                return true;
            } else {
                return false;
            }
        } else {
            // otherwise use tab key to advance

            current.tab();

            Optional<FluentElement> focused = container.find()
                    .input(withClass("x-form-focus")).ancestor().tr().firstIfPresent();
            if (focused.isPresent()) {
                current = new IndicatorField(focused.get());
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public FormItem current() {
        return current;
    }

    @Override
    public FormItem findFieldByLabel(String label) {
        throw new UnsupportedOperationException();
    }
    
    
    private class IndicatorField implements FormItem {

        private final FluentElement row;

        public IndicatorField(FluentElement row) {
            this.row = row;
            assert row.getTagName().equals("tr");
        }

        public void focus() {
            row.find().input().clickWhenReady();
        }

        @Override
        public String getLabel() {
            FluentElement labelCell = row.find().td().first();
            String label = labelCell.text();
            assert !Strings.isNullOrEmpty(label);
            return label;
        }

        @Override
        public boolean isDropDown() {
            return false;
        }

        @Override
        public void fill(String value) {
            FluentElement input = row.find().input().first();
            input.element().clear();
            input.sendKeys(value);
        }

        public void tab() {
            row.find().input().first().sendKeys(Keys.TAB);
        }
        
        public String getName() {
            return row.find().input().first().attribute("name");
        }

        @Override
        public void fill(LocalDate date) {
            throw new UnsupportedOperationException("indicator fields cannot accept dates");
        }

        @Override
        public void select(String itemLabel) {
            throw new UnsupportedOperationException("indicator fields are not drop down lists");
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public FluentElement getElement() {
            return row;
        }
    }

}
