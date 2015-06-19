package org.activityinfo.test.pageobject.gxt;


import org.activityinfo.test.pageobject.api.FluentElement;
import org.openqa.selenium.By;

import static org.activityinfo.test.pageobject.api.XPathBuilder.withText;

public class GalleryView {

    private final FluentElement gallery;

    public GalleryView(FluentElement gallery) {
        this.gallery = gallery;
    }

    public static GalleryView find(FluentElement parent) {
        return new GalleryView(parent.waitFor(By.className("gallery")));
    }
    
    public void select(String heading) {
        gallery.find().h4(withText(heading)).waitForFirst().clickWhenReady();
    }
    
}
