package com.scottbezek.embarcadero.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.scottbezek.embarcadero.app.model.data.PathCoord;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class MapScreen extends FrameLayout {

    private static final String TAG = MapScreen.class.getName();
    private static final LatLng GEOGRAPHIC_CENTER_OF_CONTIGUOUS_US = new LatLng(39.8282, -98.5795);
    private static final PolylineOptions PATH_POLYLINE_OPTIONS =  new PolylineOptions()
            .width(5)
            .color(Color.BLACK);


    private final MapView mMapView;

    private boolean mAttachedToWindow = false;

    private Observable<List<PathCoord>> mPathCoordObservable = null;
    private Subscription mPathCoordSubscription = null;

    public MapScreen(Context context) {
        super(context);

        GoogleMapOptions mapOptions = new GoogleMapOptions()
                .mapType(GoogleMap.MAP_TYPE_NORMAL)
                .compassEnabled(false)
                .rotateGesturesEnabled(false)
                .scrollGesturesEnabled(true)
                .tiltGesturesEnabled(false)
                .zoomControlsEnabled(false)
                .zoomGesturesEnabled(true);
        mMapView = new MapView(context, mapOptions);
        addView(mMapView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    public void setData(Observable<List<PathCoord>> data) {
        if (mPathCoordSubscription != null) {
            mPathCoordSubscription.unsubscribe();
            mPathCoordSubscription = null;
        }
        mPathCoordObservable = data;
        if (mAttachedToWindow) {
            subscribe();
        }
    }

    private void subscribe() {
        if (mPathCoordSubscription != null) {
            throw new IllegalStateException("Already subscribed");
        }
        mPathCoordSubscription = mPathCoordObservable
//                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<PathCoord>>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Toast.makeText(getContext(), throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onNext(List<PathCoord> pathCoords) {
                        updateMap(pathCoords);
                    }
                });
    }

    private void updateMap(List<PathCoord> pathCoords) {
        GoogleMap map = mMapView.getMap();
        map.clear();

        LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
        List<LatLng> points = new ArrayList<LatLng>();
        for (PathCoord coord : pathCoords) {
            LatLng ll = new LatLng(coord.getLatitude(), coord.getLongitude());
            boundsBuilder.include(ll);
            points.add(ll);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;

        mMapView.onCreate(null);
        mMapView.onResume();

        if (mPathCoordObservable != null) {
            subscribe();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mMapView.onPause();
        mMapView.onDestroy();

        if (mPathCoordSubscription != null) {
            mPathCoordSubscription.unsubscribe();
            mPathCoordSubscription = null;
        }
        mAttachedToWindow = false;
    }
}
