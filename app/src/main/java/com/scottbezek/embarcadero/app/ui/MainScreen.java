package com.scottbezek.embarcadero.app.ui;

import android.content.Context;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.location.LocationRequest;
import com.scottbezek.embarcadero.app.R;
import com.scottbezek.embarcadero.app.model.PathManager;
import com.scottbezek.embarcadero.app.model.PathManager.RecordingState;
import com.scottbezek.embarcadero.app.model.UserStateManager.UserState;
import com.scottbezek.embarcadero.app.model.location.GooglePlayServicesLocationUpdateProvider;
import com.scottbezek.embarcadero.app.util.ResettableClickListener;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainScreen extends LinearLayout {

    private final Observable<RecordingState> mRecordingState;
    private final Action1<RecordingState> mRecordingStateChange;

    private Subscription mRecordingStateSubscription;

    public MainScreen(final Context context, UserState userState) {
        super(context);

        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.screen_main, this, true);

        TextView welcomeText = (TextView)findViewById(R.id.welcome_message);
        welcomeText.setText("Welcome!");


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


        final ResettableClickListener startClickListener;
        final ResettableClickListener stopClickListener;
        startClickListener = new ResettableClickListener() {
            @Override
            protected void onClick() {
                pathManager.startRecording(new GooglePlayServicesLocationUpdateProvider(
                        context, dummyRequest, Looper.getMainLooper()));
            }
        };
        startButton.setOnClickListener(startClickListener);

        stopClickListener = new ResettableClickListener() {
            @Override
            protected void onClick() {
                pathManager.stopRecording();
            }
        };
        stopButton.setOnClickListener(stopClickListener);


        addView(new PathListScreen(context, pathManager), new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));

        final MapScreen mapScreen = new MapScreen(context);
        addView(mapScreen, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));

        mRecordingStateChange = new Action1<RecordingState>() {
            @Override
            public void call(RecordingState state) {
                stopButton.setVisibility(state.isRecording() ? View.VISIBLE : View.GONE);
                startButton.setVisibility(state.isRecording() ? View.GONE : View.VISIBLE);
                startClickListener.reset();
                stopClickListener.reset();

                if (state.isRecording()) {
                    mapScreen.setData(pathManager.getPathCoords(state.getPathRecordId(), Schedulers.io()));
                } else {

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
