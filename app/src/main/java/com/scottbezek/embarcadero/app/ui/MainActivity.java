package com.scottbezek.embarcadero.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.dropbox.sync.android.DbxAccountManager;
import com.scottbezek.embarcadero.app.EmbarcaderoApplication;
import com.scottbezek.embarcadero.app.R;
import com.scottbezek.embarcadero.app.model.UserStateManager;
import com.scottbezek.embarcadero.app.model.UserStateManager.UserState;
import com.scottbezek.embarcadero.app.ui.LoginScreen.Callbacks;


public class MainActivity extends Activity {

    private static final int REQUEST_LINK_TO_DBX = 0;

    private FrameLayout mContentFrame;
    private DbxAccountManager mAccountManager;
    private UserStateManager mUserStateManager;

    private final Callbacks mLoginCallbacks = new Callbacks() {
        @Override
        public void onSignInClicked() {
            mAccountManager.startLink(MainActivity.this, REQUEST_LINK_TO_DBX);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContentFrame = (FrameLayout)findViewById(R.id.content_frame);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAccountManager = ((EmbarcaderoApplication)getApplication()).getAccountManager();
        mUserStateManager = ((EmbarcaderoApplication)getApplication()).getUserStateManager();
        UserState userState = mUserStateManager.getMainUserState();
        if (userState == null) {
            replaceView(new LoginScreen(this, mLoginCallbacks));
        } else {
            replaceView(new MainScreen(this, userState));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    private void replaceView(View newView) {
        mContentFrame.removeAllViews();
        mContentFrame.addView(newView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }
}
