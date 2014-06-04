package com.scottbezek.embarcadero.app.util;

import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract.Data;
import android.util.Log;

import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastore.SyncStatusListener;
import com.dropbox.sync.android.DbxDatastoreStatus;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.NotFound;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxTable;
import com.dropbox.sync.android.DbxTable.QueryResult;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.GuardedBy;

public class DatastoreUtils {

    private DatastoreUtils() {}

    /**
     * Calls {@link com.dropbox.sync.android.DbxDatastore#sync()} and returns true if successful, or false if the datastore was deleted remotely. Throws a {@link java.lang.RuntimeException} if something fails while modifying local state.
     */
    public static boolean syncQuietly(@Nonnull DbxDatastore datastore) {
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

        @Override
        public void onDatastoreStatusChange(DbxDatastore datastore) {
            DbxDatastoreStatus status = datastore.getSyncStatus();
            if ((!mHadIncoming && status.hasIncoming)) {
                onPotentialNewIncomingData(datastore);
            }
            mHadIncoming = status.hasIncoming;
        }

        public abstract void onPotentialNewIncomingData(DbxDatastore datastore);
    }

    /**
     */
    public static abstract class PotentialDataChangeListener implements SyncStatusListener {

        private static final String TAG = PotentialDataChangeListener.class.getName();

        private boolean mHadIncoming = false;
        private boolean mHadOutgoing = false;

        @Override
        public void onDatastoreStatusChange(DbxDatastore datastore) {
            DbxDatastoreStatus status = datastore.getSyncStatus();
            if ((mHadIncoming && !status.hasIncoming) ||
                    (!mHadOutgoing && status.hasOutgoing)) {
                onPotentialDataChange(datastore);
            }
            mHadIncoming = status.hasIncoming;
            mHadOutgoing = status.hasOutgoing;
            Log.d(TAG, "Datastore status changed: " + status);
        }

        public abstract void onPotentialDataChange(DbxDatastore datastore);
    }

    public static class DatastoreWithLock {
        private final DbxDatastore mDatastore;
        private final Object mLock = new Object();

        public DatastoreWithLock(@Nonnull DbxDatastore datastore) {
            mDatastore = datastore;
        }

        @Nonnull
        public DbxDatastore getDatastore() {
            return mDatastore;
        }

        @Nonnull
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
                // TODO(sbezek): XXX don't do this on the main thread!
                final DbxDatastore expectedDatastore = getDatastore();
                if (datastore != expectedDatastore) {
                    throw new IllegalStateException();
                }
                synchronized (getLock()) {
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

    public abstract static class LiveQuery<T> {

        private final String mTableId;
        private final DbxFields mQuery;

        public LiveQuery(String tableId, DbxFields query) {
            mTableId = tableId;
            mQuery = query;
        }

        /**
         * Given a query result, returns an <b>IMMUTABLE</b> snapshot of that data. The input {@link com.dropbox.sync.android.DbxTable.QueryResult}, while mutable, will not change throughout the duration of this call.
         */
        public abstract T createImmutableSnapshot(QueryResult result);

        final T execute(DatastoreWithLock datastoreWithLock) throws DbxException {
            synchronized (datastoreWithLock.getLock()) {
                DbxTable table = datastoreWithLock.getDatastore().getTable(mTableId);
                return createImmutableSnapshot(table.query(mQuery));
            }
        }
    }

    public static class QueryLoader<T> {

        private static final String TAG = QueryLoader.class.getName();

        private final RefCountedObject<? extends DatastoreWithLock> mDatastoreRef;
        private final Executor mQueryExecutor = Executors.newSingleThreadExecutor();
        private final LiveQuery<T> mQuery;

        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private DatastoreWithLock mLiveDatastore = null;
        @GuardedBy("mLock")
        private LoaderCallback<T> mCallback;
        @GuardedBy("mLock")
        private Handler mCallbackHandler;

        private final SyncStatusListener mChangeListener = new PotentialDataChangeListener() {
            @Override
            public void onPotentialDataChange(DbxDatastore datastore) {
                Log.d(TAG, "Got potential data change, going to trigger reload!");
                triggerReload();
            }
        };

        public interface LoaderCallback<T> {
            void onNewData(T data);
        }

        public QueryLoader(RefCountedObject<? extends DatastoreWithLock> datastoreRef, LiveQuery<T> query) {
            mDatastoreRef = datastoreRef;
            mQuery = query;
        }

        public void start(LoaderCallback<T> callback, Handler callbackHandler) {
            synchronized (mLock) {
                if (mLiveDatastore != null) {
                    throw new IllegalStateException("Already started!");
                }
                mCallback = callback;
                mCallbackHandler = callbackHandler;
                mLiveDatastore = mDatastoreRef.acquire();
                mLiveDatastore.getDatastore().addSyncStatusListener(mChangeListener);
                triggerReload();
            }
        }

        private void triggerReload() {
            mQueryExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    final DatastoreWithLock datastore = mDatastoreRef.acquire();
                    try {
                        notifyCallback(mQuery.execute(datastore));
                    } catch (DbxException e) {
                        // XXX TODO
                        throw new RuntimeException(e);
                    } finally {
                        mDatastoreRef.release(datastore);
                    }
                }
            });
        }

        private void notifyCallback(final T data) {
            mCallbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mLock) {
                        // Check that we aren't currently stopped (i.e. no live datastore)
                        if (mLiveDatastore != null) {
                            mCallback.onNewData(data);
                        }
                    }
                }
            });
        }

        public void stop() {
            synchronized (mLock) {
                if (mLiveDatastore == null) {
                    throw new IllegalStateException("Not started");
                }
                mLiveDatastore.getDatastore().removeSyncStatusListener(mChangeListener);
                mDatastoreRef.release(mLiveDatastore);
                mLiveDatastore = null;
            }
        }
    }
}
