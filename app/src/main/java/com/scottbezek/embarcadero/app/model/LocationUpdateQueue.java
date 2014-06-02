package com.scottbezek.embarcadero.app.model;

import com.scottbezek.embarcadero.app.model.location.LocationUpdateProvider;
import com.scottbezek.embarcadero.app.model.location.LocationUpdateProvider.LocationUpdateListener;

import android.location.Location;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Registers for location updates and acts as a {@link java.util.concurrent.BlockingQueue}, allowing them to be processed in a blocking fashion from another thread via {@link #take()}.
 */
public class LocationUpdateQueue {

    private final BlockingQueue<Location> mQueue = new LinkedBlockingDeque<>();
    private final LocationUpdateProvider mLocationUpdateProvider;

    private final LocationUpdateListener mLocationUpdateListener = new LocationUpdateListener() {
        @Override
        public void onLocationChanged(Location location) {
            mQueue.offer(location);
        }
    };

    public LocationUpdateQueue(LocationUpdateProvider locationUpdateProvider) {
        mLocationUpdateProvider = locationUpdateProvider;
    }

    public void enableProducer() {
        mLocationUpdateProvider.startLocationUpdates(mLocationUpdateListener);
    }

    public Location take() throws InterruptedException {
        return mQueue.take();
    }

    public void disableProducer() {
        mLocationUpdateProvider.stopLocationUpdates(mLocationUpdateListener);
    }
}
