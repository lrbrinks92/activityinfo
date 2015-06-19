package org.activityinfo.test.pageobject.web.map;

import org.activityinfo.test.pageobject.api.FluentElement;
import org.openqa.selenium.By;

import java.awt.*;

public class LeafletMap {

    private final FluentElement pane;

    public LeafletMap(FluentElement pane) {
        this.pane = pane;        
    }
    
    public LeafletMap find(FluentElement container) {
        return new LeafletMap(container.findElement(By.className("leaflet-map-pane")));
    }
    
    public Image getImage() {
        throw new UnsupportedOperationException();
    }
}
