package org.activityinfo.test.pageobject.gxt;


import com.google.common.base.Optional;
import org.activityinfo.test.pageobject.api.FluentElement;
import org.openqa.selenium.By;

import static org.activityinfo.test.pageobject.api.XPathBuilder.withClass;

public class GxtMessageBox {

    private FluentElement element;

    public GxtMessageBox(FluentElement element) {
        this.element = element;
    }

    public static Optional<GxtMessageBox> get(FluentElement element) {
        Optional<FluentElement> messageBox = element.root().find()
                .div(withClass("ext-mb-icon"))
                .ancestor()
                .div(withClass("x-window")).firstIfPresent();
        if(messageBox.isPresent()) {
            return Optional.of(new GxtMessageBox(messageBox.get()));
        } else {
            return Optional.absent();
        }
    }
    
    public boolean isWarning() {
        return element.exists(By.className("ext-mb-warning"));
    }
    
    public String getMessage() {
        return element.findElement(By.className("ext-mb-content")).text();
    }
    
}
