package org.activityinfo.ui.client.util;

import org.activityinfo.model.type.geo.AiLatLng;
import org.activityinfo.legacy.shared.reports.util.mapping.Extents;
import org.discotools.gwt.leaflet.client.types.LatLng;
import org.discotools.gwt.leaflet.client.types.LatLngBounds;

public final class LeafletUtil {

    private static final double EPSILON = 0.000001;
    
    private LeafletUtil() {}
    
    public static LatLngBounds newLatLngBounds(Extents bounds) {
        LatLng southWest = new LatLng(bounds.getMinLat(), bounds.getMinLon());
        LatLng northEast = new LatLng(bounds.getMaxLat(), bounds.getMaxLon());
        return new LatLngBounds(southWest, northEast);
    }

    public static LatLng to(AiLatLng latLng) {
        return new LatLng(latLng.getLat(), latLng.getLng());
    }

    public static String color(String color) {
        if (color == null) {
            return "#FF0000";
        } else if (color.startsWith("#")) {
            return color;
        } else {
            return "#" + color;
        }
    }

    public static boolean equal(LatLngBounds a, LatLngBounds b) {
        if (a != null && b != null) {
            LatLng northWest = a.getNorthWest();
            LatLng southEast = a.getSouthEast();
            if (northWest != null && southEast != null) {
                return equal(northWest, b.getNorthWest()) && equal(southEast, b.getSouthEast());
            }
        }
        return false;
    }

    public static boolean equal(LatLng a, LatLng b) {
        return a != null && b != null && 
                Math.abs(a.lat() - b.lat()) < EPSILON && 
                Math.abs(a.lng() - b.lng()) < EPSILON;
    }
}
