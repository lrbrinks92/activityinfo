package org.activityinfo.test.pageobject.web.entry;


import org.activityinfo.i18n.shared.I18N;
import org.activityinfo.test.pageobject.api.FluentElement;
import org.activityinfo.test.pageobject.gxt.GxtFormPanel;
import org.activityinfo.test.pageobject.gxt.GxtModal;

import static java.lang.String.format;
import static org.activityinfo.test.pageobject.api.XPathBuilder.*;

public class LocationDialog {

    private GxtModal dialog;
    private final GxtFormPanel formPanel;

    public LocationDialog(GxtModal dialog) {
        this.dialog = dialog;

        formPanel = new GxtFormPanel(dialog.getWindowElement()
                .find().div(withClass("x-component"), withClass("x-form-label-left")).first());

    }

    public FluentElement locationInput() {
        return dialog.getWindowElement().find().input(
                withClass("x-form-field"),
                withClass("x-form-text"),
                withoutClass("x-triggerfield-noedit"))
                .first();
    }

    public GxtFormPanel getFormPanel() {
        return formPanel;
    }

    public static LocationDialog get(FluentElement root) {
        GxtModal modal = new GxtModal(root);
        if(!modal.getTitle().equals(I18N.CONSTANTS.chooseLocation())) {
            throw new AssertionError(format("Expected dialog with title '%s', found '%s'",
                    I18N.CONSTANTS.chooseLocation(), modal.getTitle()));
        }

        return new LocationDialog(modal);
    }

    public GxtFormPanel.GxtField latitudeField() {
        return formPanel.findFieldByLabel(I18N.CONSTANTS.latitude());
    }

    public GxtFormPanel.GxtField longitudeField() {
        return formPanel.findFieldByLabel(I18N.CONSTANTS.longitude());
    }


    public void addNew() {
        dialog.getWindowElement().find().button(withText(I18N.CONSTANTS.newLocation()))
                .waitForFirst().click();
    }

    public void dragMarker(int pixelsToLeft, int pixelsToRight) {
        FluentElement marker = dialog.getWindowElement().find()
                .div(withClass("leaflet-marker-pane"))
                .div(withClass("leaflet-marker-icon"))
                .waitForFirst();
        
        marker.dragAndDropBy(pixelsToLeft, pixelsToRight);

    }
}
