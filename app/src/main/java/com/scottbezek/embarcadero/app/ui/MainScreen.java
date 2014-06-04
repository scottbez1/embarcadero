package com.scottbezek.embarcadero.app.ui;

import com.google.android.gms.location.LocationRequest;

import com.scottbezek.embarcadero.app.R;
import com.scottbezek.embarcadero.app.model.UserStateManager.UserState;
import com.scottbezek.embarcadero.app.model.location.GooglePlayServicesLocationUpdateProvider;

import android.content.Context;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 */
public class MainScreen extends LinearLayout {

    private final UserState mUserState;

    public MainScreen(final Context context, UserState userState) {
        super(context);
        mUserState = userState;
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

        startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mUserState.getPathManager().startRecording(new GooglePlayServicesLocationUpdateProvider(context, dummyRequest, Looper.getMainLooper()));
                startButton.setVisibility(GONE);
                stopButton.setVisibility(VISIBLE);
            }
        });

        stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mUserState.getPathManager().stopRecording();
                startButton.setVisibility(VISIBLE);
                stopButton.setVisibility(GONE);
            }
        });

        addView(new PathListScreen(context, userState.getPathManager()), new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
    }
}
