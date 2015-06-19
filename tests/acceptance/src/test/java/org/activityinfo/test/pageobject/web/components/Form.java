package org.activityinfo.test.pageobject.web.components;


import org.activityinfo.test.pageobject.api.FluentElement;
import org.joda.time.LocalDate;

public abstract class Form {

    public void fillTextField(String label, String value) {
        findFieldByLabel(label).fill(value);
    }

    public void fillDateField(String label, LocalDate date) {
        findFieldByLabel(label).fill(date);
    }
    
    public void select(String label, String itemLabel) {
        findFieldByLabel(label).select(itemLabel);
    }
    
    public abstract FormItem findFieldByLabel(String label);

    public abstract boolean moveToNext();
    
    public abstract FormItem current();
    
    
    public interface FormItem {
        
        String getLabel();
        boolean isDropDown();
        
        void fill(String value);
        void fill(LocalDate date);
        void select(String itemLabel);
        
        boolean isEnabled();

        FluentElement getElement();
    }
}
