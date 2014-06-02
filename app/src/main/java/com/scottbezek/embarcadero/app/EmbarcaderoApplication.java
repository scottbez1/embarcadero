package com.scottbezek.embarcadero.app;

import com.dropbox.sync.android.DbxAccountManager;
import com.scottbezek.embarcadero.app.model.UserStateManager;

import android.app.Application;

public class EmbarcaderoApplication extends Application {

    private DbxAccountManager mAccountManager;
    private UserStateManager mUserStateManager;

    @Override
    public void onCreate() {
        super.onCreate();

        mAccountManager = DbxAccountManager
                .getInstance(this, DropboxApiKey.APP_KEY, DropboxApiKey.APP_SECRET);
        mUserStateManager = new UserStateManager(mAccountManager);
    }

    public DbxAccountManager getAccountManager() {
        return mAccountManager;
    }

    public UserStateManager getUserStateManager() {
        return mUserStateManager;
    }
}
