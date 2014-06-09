package com.scottbezek.embarcadero.app.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastore.SyncStatusListener;
import com.dropbox.sync.android.DbxDatastoreStatus;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.NotFound;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;
import com.dropbox.sync.android.DbxTable.QueryResult;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.DatastoreWithLock.OnSyncListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

public class DatastoreUtils {

    private DatastoreUtils() {}

    /**
     * Calls {@link com.dropbox.sync.android.DbxDatastore#sync()} and returns true if successful, or false if the datastore was deleted remotely. Throws a {@link java.lang.RuntimeException} if something fails while modifying local state.
     */
    public static boolean syncQuietly(@Nonnull DatastoreWithLock datastoreWithLock) {
        try {
            datastoreWithLock.doSync();
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

        private Map<OnSyncListener, Handler> mListeners = new HashMap<>();

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

        public Map<String, Set<DbxRecord>> doSync() throws DbxException {
            if (!Thread.holdsLock(mLock)) {
                throw new IllegalStateException("Must hold lock during sync");
            }
            Map<String, Set<DbxRecord>> result = mDatastore.sync();

            synchronized (mListeners) {
                for (Entry<OnSyncListener, Handler> entry : mListeners.entrySet()) {
                    final OnSyncListener listener = entry.getKey();
                    entry.getValue().postAtTime(new Runnable() {
                        @Override
                        public void run() {
                            listener.onSynced();
                        }
                    }, listener, 0);
                }
            }
            return result;
        }

        public void close() {
            mDatastore.close();
        }

        public void addSyncListener(OnSyncListener listener) {
            synchronized (mListeners) {
                if (mListeners.containsKey(listener)) {
                    throw new IllegalStateException("Already registered");
                }
                mListeners.put(listener, new Handler(Looper.myLooper()));
            }
        }

        public void removeSyncListener(OnSyncListener listener) {
            synchronized (mListeners) {
                Handler removed = mListeners.remove(listener);
                if (removed == null) {
                    throw new IllegalStateException("Not registered");
                }
                if (removed.getLooper() != Looper.myLooper()) {
                    throw new IllegalStateException("Must unregister on original Looper");
                }
                removed.removeCallbacksAndMessages(listener);
            }
        }

        public interface OnSyncListener {
            /**
             * A reasonable proxy for data within the datastore having changed. Syncs are invoked
             * after local changes are made, or when remote changes become available.
             */
            void onSynced();
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
                    syncQuietly(AutoSyncingDatastoreWithLock.this);
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

    public abstract static class DatastoreQuery<T> {

        private final String mTableId;
        private final DbxFields mQuery;

        public DatastoreQuery(String tableId, DbxFields query) {
            mTableId = tableId;
            mQuery = query;
        }

        /**
         * Given a query result, returns an <b>IMMUTABLE</b> snapshot of that data. The input {@link com.dropbox.sync.android.DbxTable.QueryResult}, while mutable, will not change throughout the duration of this call.
         */
        public abstract T createImmutableSnapshot(QueryResult result);

        public final T executeOnDatastore(DatastoreWithLock datastoreWithLock) throws DbxException {
            synchronized (datastoreWithLock.getLock()) {
                DbxTable table = datastoreWithLock.getDatastore().getTable(mTableId);
                return createImmutableSnapshot(table.query(mQuery));
            }
        }
    }

    public interface DataStream<T> {

        public interface LoaderCallback<T> {
            void onNewData(T data);
        }

        void start(LoaderCallback<T> callback, Looper callbackLooper);

        void stop();
    }

    public static class QueryLoader<T> implements DataStream<T> {

        private static final String TAG = QueryLoader.class.getName();

        private final RefCountedObject<? extends DatastoreWithLock> mDatastoreRef;
        private final Executor mQueryExecutor = Executors.newSingleThreadExecutor();
        private final DatastoreQuery<T> mQuery;

        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private DatastoreWithLock mLiveDatastore = null;
        @GuardedBy("mLock")
        private LoaderCallback<T> mCallback;
        @GuardedBy("mLock")
        private Handler mCallbackHandler;

        private final OnSyncListener mChangeListener = new OnSyncListener() {
            @Override
            public void onSynced() {
                Log.d(TAG, "Got potential data change, going to trigger reload!");
                triggerReload();
            }
        };

        public QueryLoader(RefCountedObject<? extends DatastoreWithLock> datastoreRef, DatastoreQuery<T> query) {
            mDatastoreRef = datastoreRef;
            mQuery = query;
        }

        public void start(LoaderCallback<T> callback, Looper callbackLooper) {
            synchronized (mLock) {
                if (mLiveDatastore != null) {
                    throw new IllegalStateException("Already started!");
                }
                mCallback = callback;
                mCallbackHandler = new Handler(callbackLooper);
                mLiveDatastore = mDatastoreRef.acquire();
                mLiveDatastore.addSyncListener(mChangeListener);
                triggerReload();
            }
        }

        private void triggerReload() {
            mQueryExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    final DatastoreWithLock datastore = mDatastoreRef.acquire();
                    try {
                        notifyCallback(mQuery.executeOnDatastore(datastore));
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
                mLiveDatastore.removeSyncListener(mChangeListener);
                mDatastoreRef.release(mLiveDatastore);
                mLiveDatastore = null;
            }
        }
    }
}
