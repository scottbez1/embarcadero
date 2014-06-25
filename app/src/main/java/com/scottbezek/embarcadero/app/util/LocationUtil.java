package com.scottbezek.embarcadero.app.util;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class LocationUtil {
    private LocationUtil() {}

    /**
     * Returns a new {@link LatLngBounds} with the specified minimum degrees
     * latitude/longitude side length, centered around the original bounds
     * center.
     */
    public static LatLngBounds clampMinimumSize(LatLngBounds original, double minSideDegrees) {
        double adjustment = minSideDegrees / 2;
        LatLng center = original.getCenter();
        return original
                .including(new LatLng(center.latitude, center.longitude + adjustment))
                .including(new LatLng(center.latitude, center.longitude - adjustment))
                .including(new LatLng(center.latitude + adjustment, center.longitude))
                .including(new LatLng(center.latitude - adjustment, center.longitude));
    }
}
