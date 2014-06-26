package com.scottbezek.embarcadero.app.ui;

import android.content.Context;
import android.location.Location;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.location.LocationRequest;
import com.scottbezek.embarcadero.app.R;
import com.scottbezek.embarcadero.app.model.PathManager;
import com.scottbezek.embarcadero.app.model.PathManager.RecordingState;
import com.scottbezek.embarcadero.app.model.UserStateManager.UserState;
import com.scottbezek.embarcadero.app.model.data.PathCoord;

import java.util.Collections;
import java.util.List;

import pl.charmas.android.reactivelocation.observables.location.LastKnownLocationObservable;
import pl.charmas.android.reactivelocation.observables.location.LocationUpdatesObservable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainScreen extends DrawerLayout {

    private final Observable<RecordingState> mRecordingState;
    private final Action1<RecordingState> mRecordingStateChange;

    private Subscription mRecordingStateSubscription;

    public MainScreen(final Context context, UserState userState) {
        super(context);


        final long MIN_UPDATE_TIME_MS = 1000;
        final float MIN_UPDATE_DISTANCE_M = 0.1f;
        final LocationRequest dummyRequest = new LocationRequest()
                .setInterval(MIN_UPDATE_TIME_MS)
                .setSmallestDisplacement(MIN_UPDATE_DISTANCE_M)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        final Button startButton = (Button)findViewById(R.id.start_button);
        final Button stopButton = (Button)findViewById(R.id.stop_button);

        final PathManager pathManager = userState.getPathManager();
        mRecordingState = pathManager.getRecordingState();


//        final ResettableClickListener startClickListener;
//        final ResettableClickListener stopClickListener;
//        startClickListener = new ResettableClickListener() {
//            @Override
//            protected void onClick() {
//                pathManager.startRecording(new GooglePlayServicesLocationUpdateProvider(
//                        context, dummyRequest, Looper.getMainLooper()));
//            }
//        };
//        startButton.setOnClickListener(startClickListener);
//
//        stopClickListener = new ResettableClickListener() {
//            @Override
//            protected void onClick() {
//                pathManager.stopRecording();
//            }
//        };
//        stopButton.setOnClickListener(stopClickListener);

        final MapScreen mapScreen = new MapScreen(context);
        addView(mapScreen, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final int drawerWidth = getResources().getDimensionPixelSize(R.dimen.drawer_width);
        final LayoutParams drawerLayoutParams = new LayoutParams(drawerWidth, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT);
        final NavScreen navScreen = new NavScreen(context, userState.getAccountInfo(), pathManager.getPathList(Schedulers.io()));
        addView(navScreen, drawerLayoutParams);


        setDrawerListener(new SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                mapScreen.setOffset((int)(slideOffset * drawerWidth / 2));
            }
        });

        mRecordingStateChange = new Action1<RecordingState>() {
            @Override
            public void call(RecordingState state) {
//                stopButton.setVisibility(state.isRecording() ? View.VISIBLE : View.GONE);
//                startButton.setVisibility(state.isRecording() ? View.GONE : View.VISIBLE);
//                startClickListener.reset();
//                stopClickListener.reset();

                if (state.isRecording()) {
                    mapScreen.setData(pathManager.getPathCoords(state.getPathRecordId(), Schedulers.io()));
                } else {
                    Observable<List<PathCoord>> currentLocation = Observable.concat(
                            LastKnownLocationObservable.createObservable(getContext()),
                            LocationUpdatesObservable.createObservable(getContext(), dummyRequest))
                            .map(new Func1<Location, List<PathCoord>>() {
                                @Override
                                public List<PathCoord> call(Location location) {
                                    if (location == null) {
                                        return Collections.emptyList();
                                    } else {
                                        return Collections.singletonList(PathCoord.from(location));
                                    }
                                }
                            });
                    mapScreen.setData(currentLocation);
                }
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mRecordingStateSubscription = mRecordingState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mRecordingStateChange);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mRecordingStateSubscription.unsubscribe();
    }
}
