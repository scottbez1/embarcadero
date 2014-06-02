package com.scottbezek.embarcadero.app.model;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

/**
 */
public class CentralDatastoreManager {

    private static final Object sInstanceLock = new Object();
    private static CentralDatastoreManager sInstance = null;

    private final Map<String, RefCountedDatastore> mUserDatastores = new HashMap<>();

    public static CentralDatastoreManager getInstance() {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                sInstance = new CentralDatastoreManager();
            }
            return sInstance;
        }
    }

    public DbxDatastore acquireForUser(DbxAccount account) {
        synchronized (mUserDatastores) {
            final String userId = account.getUserId();
            RefCountedDatastore rcd = mUserDatastores.get(userId);
            if (rcd == null) {
                try {
                    rcd = new RefCountedDatastore(DbxDatastore.openDefault(account));
                } catch (DbxException e) {
                    throw new RuntimeException(e);
                }
                mUserDatastores.put(userId, rcd);
            }
            return rcd.acquire();
        }
    }

    public void release(DbxAccount account, DbxDatastore datastore) {
        synchronized (mUserDatastores) {
            final String userId = account.getUserId();
            RefCountedDatastore rcd = mUserDatastores.get(userId);
            if (rcd == null) {
                throw new IllegalStateException("No datastore found for user");
            }
            int refs = rcd.release(datastore);
            if (refs == 0) {
                datastore.close();
                mUserDatastores.remove(userId);
            }
        }
    }

    @NotThreadSafe
    private static class RefCountedDatastore {

        private final DbxDatastore mDatastore;
        private int mRefCount = 0;

        public RefCountedDatastore(DbxDatastore datastore) {
            mDatastore = datastore;
        }

        /**
         * Acquire a reference to the datastore.
         */
        public DbxDatastore acquire() {
            mRefCount++;
            return mDatastore;
        }

        /**
         * Release a reference to the datastore.
         * @return The new reference count.
         */
        public int release(DbxDatastore datastore) {
            if (datastore != mDatastore) {
                throw new IllegalArgumentException("Trying to release the wrong datastore");
            }
            mRefCount--;
            if (mRefCount < 0) {
                throw new IllegalStateException("No refs to release!");
            }
            return mRefCount;
        }
    }
}
