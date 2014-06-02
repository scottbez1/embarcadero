package com.scottbezek.embarcadero.app.model;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PathRecorderService extends Service {

//    private static final String EXTRA_PATH_RECORD_ID = "EXTRA_PATH_RECORD_ID";
//    private static final String EXTRA_USER_STATE = "EXTRA_USER_STATE";
//
//    private CountDownLatch mStopLatch;
//
//    public static Intent getStartIntent(Context context, String pathRecordId, UserState userState) {
//        final Intent intent = new Intent(context, PathRecorderService.class);
//        intent.putExtra(EXTRA_PATH_RECORD_ID, pathRecordId);
//        intent.putExtra(EXTRA_USER_STATE, userState.toBundle());
//        return intent;
//    }
//
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding not supported");
    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (!intent.hasExtra(EXTRA_PATH_RECORD_ID)
//                || !intent.hasExtra(EXTRA_USER_STATE)) {
//            stopSelf();
//            return START_NOT_STICKY;
//        }
//        if (mStopLatch != null) {
//            throw new IllegalStateException("Already running?");
//        }
//        mStopLatch = new CountDownLatch(1);
//        UserStateManager userStateManager = ((EmbarcaderoApplication)getApplication()).getUserStateManager();
//        UserState userState = UserState.fromBundle(userStateManager, intent.getBundleExtra(EXTRA_USER_STATE));
//        final PathManager pathManager = userState.getPathManager();
//
//        new Thread() {
//            @Override
//            public void run() {
//                try {
//
//                }
//            }
//        }.start();
//
//        return START_REDELIVER_INTENT;
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        if (!mRunning.compareAndSet(true, false)) {
//            throw new IllegalStateException("Destroyed service that isn't running");
//        }
//    }
}
