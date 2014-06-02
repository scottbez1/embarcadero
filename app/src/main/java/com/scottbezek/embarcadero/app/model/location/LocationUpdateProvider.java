package com.scottbezek.embarcadero.app.model.location;

import android.location.Location;

/**
 * Provides location updates as they occur.
 */
public interface LocationUpdateProvider {

    /**
     * {@link LocationUpdateProvider}-agnostic location update listener
     * interface
     */
    public interface LocationUpdateListener {

        /**
         * Called when the location has changed.
         * @param location The new location.
         */
        void onLocationChanged(Location location);
    }

    /**
     * Starts location updates that will be delivered asynchronously to the
     * specified listener.
     *
     * @param listener
     *            The listener to register for updates. Must not already be
     *            registered.
     */
    void startLocationUpdates(LocationUpdateListener listener);

    /**
     * Stop location updates to a listener that was previously registered.
     *
     * @param listener
     *            The listener to unregister for updates.
     */
    void stopLocationUpdates(LocationUpdateListener listener);
}

