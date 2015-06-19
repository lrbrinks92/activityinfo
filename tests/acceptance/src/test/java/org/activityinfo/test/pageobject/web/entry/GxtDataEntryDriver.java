package org.activityinfo.test.pageobject.web.entry;


import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.test.driver.DataEntryDriver;
import org.activityinfo.test.pageobject.gxt.GxtModal;
import org.joda.time.LocalDate;

public class GxtDataEntryDriver implements DataEntryDriver {
    
    private DataEntryDriver current;
    
    public GxtDataEntryDriver(GxtModal gxtModal) {
        if(gxtModal.getTitle().equals(I18N.CONSTANTS.chooseLocation())) {
            current = new LocationDataEntryDriver(new LocationDialog(gxtModal));
        } else {
            current = new GxtFormDataEntryDriver(gxtModal);
        }
    }
    
    public LocationDialog getLocationDialog() {
        if(!(current instanceof LocationDataEntryDriver)) {
            throw new AssertionError("The Location dialog is not displayed");
        }
        return ((LocationDataEntryDriver) current).getDialog();
    }

    public DataEntryDriver getCurrent() {
        return current;
    }

    @Override
    public boolean nextField() {
        return current.nextField();
    }

    @Override
    public void submit() throws InterruptedException {
        current.submit();
    }

    @Override
    public String getLabel() {
        return current.getLabel();
    }

    @Override
    public void fill(String text) {
        current.fill(text);
    }

    @Override
    public void fill(LocalDate date) {
        current.fill(date);
    }

    @Override
    public void select(String itemLabel) {
        current.fill(itemLabel);
    }

    @Override
    public boolean isNextEnabled() {
        return current.isNextEnabled();
    }
}
