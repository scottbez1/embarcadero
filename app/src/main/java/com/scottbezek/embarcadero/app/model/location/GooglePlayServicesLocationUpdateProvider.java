package com.scottbezek.embarcadero.app.model.location;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

public class GooglePlayServicesLocationUpdateProvider implements
        LocationUpdateProvider {

    private final LocationClient mLocationClient;
    private final LocationRequest mLocationRequest;
    private final Looper mLooper;

    private final Object mListenerLock = new Object();
    private final Set<LocationUpdateListener> mListenersToNotify =
            new HashSet<>();
    private boolean mConnectedToLocationClient = false;

    private final LocationListener mUnderlyingListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            final List<LocationUpdateListener> toNotify;
            synchronized (mListenerLock) {
                toNotify = new ArrayList<>(mListenersToNotify);
            }
            for (LocationUpdateListener listener : toNotify) {
                listener.onLocationChanged(location);
            }
        }
    };

    private final ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle connectionHint) {
            synchronized (mListenerLock) {
                if (mConnectedToLocationClient) {
                    throw new IllegalStateException("Already connected to client");
                }
                mConnectedToLocationClient = true;

                /*
                 * It's possible that the connection took a while and some
                 * listeners registered in the meantime. If that's the case, the
                 * underlying listener won't have been registered yet, so we
                 * register it here.
                 */
                if (mListenersToNotify.size() > 0) {
                    mLocationClient.requestLocationUpdates(mLocationRequest, mUnderlyingListener);
                }
            }
        }

        @Override
        public void onDisconnected() {
            synchronized (mListenerLock) {
                if (!mConnectedToLocationClient) {
                    throw new IllegalStateException("Not connected to a client");
                }
                mConnectedToLocationClient = false;
            }
        }
    };

    private final OnConnectionFailedListener mConnectionFailedListener =
            new OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            // TODO(sbezek): handle this
            throw new RuntimeException();
        }
    };

    /**
     * Create a {@link LocationUpdateProvider} backed by Google Play Services fused location.
     * @param context Context for connecting to the {@link LocationClient}
     * @param locationRequest {@link LocationRequest} to use when registering for location updates
     * @param looper A {@link android.os.Looper} to use for callback invocations.
     */
    public GooglePlayServicesLocationUpdateProvider(Context context, LocationRequest locationRequest, Looper looper) {
        mLocationClient = new LocationClient(context, mConnectionCallbacks,
                mConnectionFailedListener);
        mLocationClient.connect();
        // TODO(sbezek): disconnect ever? probably?

        mLocationRequest = locationRequest;
        mLooper = looper;
    }


    @Override
    public void stopLocationUpdates(LocationUpdateListener listener) {
        synchronized (mListenerLock) {
            /*
             * Need to stop the underlying location updates if we're removing
             * the last listener.
             */
            if (mConnectedToLocationClient
                    && mListenersToNotify.size() == 1
                    && mListenersToNotify.contains(listener)) {
                mLocationClient.removeLocationUpdates(mUnderlyingListener);
            }

            if (!mListenersToNotify.remove(listener)) {
                throw new IllegalStateException("Can't remove listener: listener not registered");
            }
        }
    }

    @Override
    public void startLocationUpdates(LocationUpdateListener listener) {
        synchronized (mListenerLock) {
            if (!mListenersToNotify.add(listener)) {
                throw new IllegalStateException("Can't add listener: listener already registered");
            }

            /*
             * Need to start the underlying location updates if this is the
             * first listener registered.
             */
            if (mConnectedToLocationClient
                   && mListenersToNotify.size() == 1) {
                mLocationClient.requestLocationUpdates(mLocationRequest, mUnderlyingListener,
                        Looper.getMainLooper());
            }
        }
    }

    @Override
    @CheckForNull
    public Location getLastLocation() {
        synchronized (mListenerLock) {
            if (mConnectedToLocationClient) {
                return mLocationClient.getLastLocation();
            }
            return null;
        }
    }
}
