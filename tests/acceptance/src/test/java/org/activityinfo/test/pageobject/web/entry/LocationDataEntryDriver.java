package org.activityinfo.test.pageobject.web.entry;

import org.activityinfo.test.driver.DataEntryDriver;
import org.joda.time.LocalDate;

public class LocationDataEntryDriver implements DataEntryDriver {
    
    private final LocationDialog dialog;

    public LocationDataEntryDriver(LocationDialog dialog) {
        this.dialog = dialog;
    }

    public LocationDialog getDialog() {
        return dialog;
    }

    @Override
    public boolean nextField() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void submit() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLabel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void fill(String text) {
        throw new UnsupportedOperationException();
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
    public boolean isNextEnabled() {
        throw new UnsupportedOperationException();
    }
}
