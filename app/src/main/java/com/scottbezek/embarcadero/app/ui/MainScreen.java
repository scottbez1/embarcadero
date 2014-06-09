package com.scottbezek.embarcadero.app.ui;

import com.google.android.gms.location.LocationRequest;

import com.scottbezek.embarcadero.app.R;
import com.scottbezek.embarcadero.app.model.PathManager;
import com.scottbezek.embarcadero.app.model.UserStateManager.UserState;
import com.scottbezek.embarcadero.app.model.location.GooglePlayServicesLocationUpdateProvider;
import com.scottbezek.embarcadero.app.util.ResettableClickListener;

import android.content.Context;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 */
public class MainScreen extends LinearLayout {

    private final Observable<Boolean> mRecordingState;
    private final Action1<Boolean> mRecordingStateChange;

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

        mRecordingStateChange = new Action1<Boolean>() {
            @Override
            public void call(Boolean recording) {
                stopButton.setVisibility(recording ? View.VISIBLE : View.GONE);
                startButton.setVisibility(recording ? View.GONE : View.VISIBLE);
                startClickListener.reset();
                stopClickListener.reset();
            }
        };

        addView(new PathListScreen(context, pathManager), new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
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
