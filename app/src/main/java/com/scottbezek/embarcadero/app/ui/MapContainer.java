package com.scottbezek.embarcadero.app.ui;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.MapView;
import com.scottbezek.embarcadero.app.util.Asserts;

import javax.annotation.Nonnull;

/**
 * Holds a {@link MapView}. Makes it easy to apply a {@link CameraUpdate} to the
 * map, even immediately after configuring it. Normally a {@link MapView} throws
 * an Exception if a {@link CameraUpdate} is applied before the view has been
 * laid out for the first time, so this container will delay the
 * {@link CameraUpdate} until the next layout has completed.
 *
 * @see {@link #updateMapCamera(CameraUpdate, boolean)}
 */
public class MapContainer extends FrameLayout {

    private final MapView mMapView;

    public MapContainer(Context context, MapView mapView) {
        super(context);
        mMapView = mapView;
        addView(mMapView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    private boolean mHasLaidOut = false;
    private CameraUpdate mCameraUpdateForNextLayout = null;
    private Boolean mCameraUpdateForNextLayoutShouldAnimate = null;

    /**
     * Listener for the user touching the map.
     */
    public interface MapTouchListener {
        void onMapTouchEvent();
    }

    private MapTouchListener mMapTouchListener = null;

    @Override
    protected void onAttachedToWindow() {
        mHasLaidOut = false;
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHasLaidOut = false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mHasLaidOut = true;
        if (mCameraUpdateForNextLayout != null) {
            updateCamera(mCameraUpdateForNextLayout,
                    mCameraUpdateForNextLayoutShouldAnimate);
            mCameraUpdateForNextLayout = null;
            mCameraUpdateForNextLayoutShouldAnimate = null;
        }
    }

    private void updateCamera(@Nonnull CameraUpdate update, boolean animate) {
        if (animate) {
            mMapView.getMap().animateCamera(update);
        } else {
            mMapView.getMap().moveCamera(update);
        }
    }

    /**
     * Update the contained map's camera when possible. This may not happen
     * immediately if the map hasn't been laid out yet (in which case the camera
     * will be updated after the next layout).
     *
     * @param update
     *            the camera update to apply
     * @param animate
     *            whether to animate the camera update
     */
    public void updateMapCamera(@Nonnull CameraUpdate update, boolean animate) {
        Asserts.assertMainThreadOnly();
        if (mHasLaidOut) {
            updateCamera(update, animate);
        } else {
            mCameraUpdateForNextLayout = update;
            mCameraUpdateForNextLayoutShouldAnimate = animate;
        }
    }

    public void setMapTouchListener(MapTouchListener listener) {
        Asserts.assertMainThreadOnly();
        Asserts.assertNull(mMapTouchListener);
        mMapTouchListener = listener;
    }

    public void unsetMapTouchListener(MapTouchListener listener) {
        Asserts.assertMainThreadOnly();
        Asserts.assertNotNull(mMapTouchListener);
        Asserts.assertEqual(mMapTouchListener, listener);
        mMapTouchListener = null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && mMapTouchListener != null) {
            mMapTouchListener.onMapTouchEvent();
        }
        return super.onInterceptTouchEvent(ev);
    }
}
