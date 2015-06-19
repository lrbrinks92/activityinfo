package org.activityinfo.test.pageobject.gxt;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.api.FluentElements;
import org.activityinfo.test.pageobject.web.components.Form;
import org.joda.time.LocalDate;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

import java.util.List;

import static java.lang.String.format;
import static org.activityinfo.test.pageobject.api.XPathBuilder.withClass;
import static org.activityinfo.test.pageobject.api.XPathBuilder.withText;

public class GxtFormPanel extends Form {

    public static final String ARROW_CLASS = "x-form-trigger-arrow";
    public static final String COMBO_LIST_CLASS = "x-combo-list-inner";
    public static final String READ_ONLY_CLASS = "x-form-readonly";
    public static final String ITEM_LABEL_CLASS = "x-form-item-label";
    public static final String COMBO_ITEM_CLASS = "x-combo-list-item";

    private FluentElement form;
    private GxtField current;

    public GxtFormPanel(FluentElement form) {
        this.form = form;
    }

    
    @Override
    public GxtField findFieldByLabel(String labelText) {
        try {
            return new GxtField(form.find().label(withText(labelText + ":")).ancestor().div(withClass("x-form-item")).first());
        } catch(NoSuchElementException e) {
            throw new AssertionError(format("The form panel has no field with label %s", labelText));
        }
    }

    @Override
    public FormItem current() {
        return current;
    }
    
    @Override
    public boolean moveToNext() {
        Optional<FluentElement> nextElement;
        if(current == null) {
            nextElement = form.find().div(withClass("x-form-item")).firstIfPresent();
        } else {
            nextElement = current.element.find().followingSibling().div(withClass("x-form-item")).firstIfPresent();
        }
        
        if(nextElement.isPresent()) {
            current = new GxtField(nextElement.get());
            System.out.println("current = " + current.getLabel());
            return true;
        } else {
            return false;
        }
    }

    public class GxtField implements FormItem {

        private final FluentElement element;

        public GxtField(FluentElement element) {
            this.element = element;
        }

        @Override
        public String getLabel() {
            String label = this.element.find().label(withClass(ITEM_LABEL_CLASS)).first().text();
            if(label.endsWith(":")) {
                return label.substring(0, label.length()-1);
            } else {
                return label;
            }
        }

        @Override
        public boolean isDropDown() {
            return element.find().img(withClass(ARROW_CLASS)).exists();
        }

        @Override
        public boolean isEnabled() {
            boolean enabled = !this.element.exists(By.className(READ_ONLY_CLASS));
            System.out.println(getLabel() + ".enabled = " + enabled);
            return enabled;
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

        @Override
        public void select(String itemLabel) {

            element.findElement(By.className(ARROW_CLASS)).click();

            FluentElement list = this.element.waitFor(By.className(COMBO_LIST_CLASS));


            FluentElements items = list.findElements(By.className(COMBO_ITEM_CLASS));
            List<String> itemLabels = Lists.newArrayList();
            for (FluentElement element : items) {
                String text = element.text();
                itemLabels.add(text);
                if(text.equals(itemLabel)) {
                    element.click();
                    return;
                }
            }

            // Report nice error message
            throw new AssertionError(format("Could not select '%s' from combo box '%s'. Options:\n%s",
                    itemLabel,
                    getLabel(),
                    Joiner.on("\n").join(itemLabels)));
        }
        
        public boolean isValid() {
            return !element.findElement(By.tagName("input")).attribute("class").contains("x-form-invalid");
        }

        public void assertValid() {
            if(!isValid()) {
                throw new AssertionError(format("Expected field '%s' to be valid", getLabel()));
            }
        }

        public FluentElement getElement() {
            return element;
        }
    }
}
