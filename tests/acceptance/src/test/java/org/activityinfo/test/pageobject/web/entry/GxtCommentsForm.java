package org.activityinfo.test.pageobject.web.entry;

import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.web.components.Form;
import org.joda.time.LocalDate;


public class GxtCommentsForm extends Form {

    public static final String LABEL = "Comments";
    
    private final FluentElement textArea;
    private FormItem current = null;

    public GxtCommentsForm(FluentElement textArea) {
        this.textArea = textArea;
    }


    @Override
    public boolean moveToNext() {
        if(current == null) {
            current = new CommentField();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public FormItem current() {
        return current;
    }

    @Override
    public FormItem findFieldByLabel(String label) {
        if(label.equals(LABEL)) {
            return new CommentField();
        } else {
            throw new AssertionError("Comments form has only field with label " + LABEL);
        }
    }

    private class CommentField implements FormItem {

        @Override
        public String getLabel() {
            return LABEL;
        }

        @Override
        public boolean isDropDown() {
            return false;
        }

        @Override
        public void fill(String value) {
            textArea.sendKeys(value);
        }

        @Override
        public void fill(LocalDate date) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void select(String itemLabel) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public FluentElement getElement() {
            throw new UnsupportedOperationException();
        }
    }
}
