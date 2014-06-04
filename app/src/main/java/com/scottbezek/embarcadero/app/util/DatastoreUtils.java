package com.scottbezek.embarcadero.app.util;

import android.util.Log;

import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastore.SyncStatusListener;
import com.dropbox.sync.android.DbxDatastoreStatus;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.NotFound;

public class DatastoreUtils {

    private DatastoreUtils() {}

    /**
     * Calls {@link com.dropbox.sync.android.DbxDatastore#sync()} and returns true if successful, or false if the datastore was deleted remotely. Throws a {@link java.lang.RuntimeException} if something fails while modifying local state.
     */
    public static boolean syncQuietly(DbxDatastore datastore) {
        try {
            datastore.sync();
            return true;
        } catch (NotFound e) {
            return false;
        } catch (DbxException e) {
            // TODO(sbezek): Should this just return false?
            throw new RuntimeException(e);
        }
    }

    /**
     * {@link com.dropbox.sync.android.DbxDatastore.SyncStatusListener} that filters for when new incoming changes become available.
     */
    public static abstract class PotentialIncomingDataListener implements SyncStatusListener {
        private boolean mHadIncoming = false;
//        private boolean mHadOutgoing = false;

        @Override
        public void onDatastoreStatusChange(DbxDatastore datastore) {
            DbxDatastoreStatus status = datastore.getSyncStatus();
            if ((mHadIncoming && !status.hasIncoming)) {
                onPotentialNewIncomingData(datastore);
            }
            mHadIncoming = status.hasIncoming;
//            mHadOutgoing = status.hasOutgoing;
        }

        public abstract void onPotentialNewIncomingData(DbxDatastore datastore);
    }

    public static class DatastoreWithLock {
        private final DbxDatastore mDatastore;
        private final Object mLock = new Object();

        public DatastoreWithLock(DbxDatastore datastore) {
            mDatastore = datastore;
        }

        public DbxDatastore getDatastore() {
            return mDatastore;
        }

        public Object getLock() {
            return mLock;
        }

        public void close() {
            mDatastore.close();
        }
    }

    public static class AutoSyncingDatastoreWithLock extends DatastoreWithLock {

        private final String TAG = AutoSyncingDatastoreWithLock.class.getName();

        private final SyncStatusListener mSyncListener = new PotentialIncomingDataListener() {
            @Override
            public void onPotentialNewIncomingData(DbxDatastore datastore) {
                synchronized (getLock()) {
                    DbxDatastore expectedDatastore = getDatastore();
                    if (datastore != expectedDatastore) {
                        throw new IllegalStateException();
                    }
                    Log.d(TAG, "Data potentially changed, going to sync...");
                    syncQuietly(expectedDatastore);
                }
            }
        };

        public AutoSyncingDatastoreWithLock(DbxDatastore datastore) {
            super(datastore);
            datastore.addSyncStatusListener(mSyncListener);
        }

        @Override
        public void close() {
            getDatastore().removeSyncStatusListener(mSyncListener);
            super.close();
        }
    }
}
