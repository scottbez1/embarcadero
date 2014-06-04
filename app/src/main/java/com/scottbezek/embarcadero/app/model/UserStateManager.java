package com.scottbezek.embarcadero.app.model;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxAccountManager.AccountListener;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastore.SyncStatusListener;
import com.dropbox.sync.android.DbxDatastoreStatus;
import com.dropbox.sync.android.DbxException;
import com.scottbezek.embarcadero.app.util.RefCountedObject;
import com.scottbezek.embarcadero.app.util.RefCountedObject.Closer;
import com.scottbezek.embarcadero.app.util.RefCountedObject.Factory;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.AutoSyncingDatastoreWithLock;

import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 */
public class UserStateManager {

    private final DbxAccountManager mAccountManager;

    @GuardedBy("mUserStateMap")
    private final Map<String, UserState> mUserStateMap = new HashMap<>();

    public UserStateManager(DbxAccountManager accountManager) {
        mAccountManager = accountManager;
        mAccountManager.addListener(new AccountListener() {
            @Override
            public void onLinkedAccountChange(DbxAccountManager accountManager,
                    DbxAccount account) {
                refreshUserState();
            }
        });
        refreshUserState();
    }

    private void refreshUserState() {
        synchronized (mUserStateMap) {
            final Set<String> existingUids = mUserStateMap.keySet();
            final Set<String> currentUids = new HashSet<>();

            // TODO(sbezek): deal with multiple linked accounts once available
            DbxAccount account = mAccountManager.getLinkedAccount();
            if (account != null) {
                currentUids.add(account.getUserId());
                if (!existingUids.contains(account.getUserId())) {
                    mUserStateMap.put(account.getUserId(), new UserState(account));
                }
            }

            for (String existingUid : existingUids) {
                if (!currentUids.contains(existingUid)) {
                    UserState oldUserState = mUserStateMap.remove(existingUid);
                    oldUserState.destroy();
                }
            }
        }
    }

    /**
     * Temporary single-user state retrieval.
     *
     * @return Single user's {@link com.scottbezek.embarcadero.app.model.UserStateManager.UserState} if a user is signed in, <code>null</code> if no users are signed in. Throws an exception if more than one user is signed in.
     */
    public UserState getMainUserState() {
        synchronized (mUserStateMap) {
            final int size = mUserStateMap.size();
            if (size == 0) {
                return null;
            } else if (size == 1) {
                return mUserStateMap.values().iterator().next();
            } else {
                throw new IllegalStateException("More than 1 user signed in: " + mUserStateMap.keySet());
            }
        }
    }

    public static class UserState {

        private static final String BUNDLE_KEY_USER_ID = "BUNDLE_KEY_USER_ID";

        private final String mUserId;
        private final PathManager mPathManager;
        private final RefCountedObject<AutoSyncingDatastoreWithLock> mDatastoreRef;

        // XXX
        private final SyncStatusListener mSyncStatusListener = new SyncStatusListener() {
            @Override
            public void onDatastoreStatusChange(DbxDatastore datastore) {
                DbxDatastoreStatus s = datastore.getSyncStatus();
                Log.d("XXX NORELEASE", "Datastore status change: outgoing:" + s.hasOutgoing + ", incoming:" + s.hasIncoming + ", uploading:" + s.isUploading + ", downloading:" + s.isDownloading);
            }
        };

        UserState(final DbxAccount account) {
            mDatastoreRef = new RefCountedObject<>(new Factory<AutoSyncingDatastoreWithLock>() {
                @Nonnull
                @Override
                public AutoSyncingDatastoreWithLock create() {
                    try {
                        DbxDatastore datastore = DbxDatastore.openDefault(account);
                        return new AutoSyncingDatastoreWithLock(datastore);
                    } catch (DbxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, new Closer<AutoSyncingDatastoreWithLock>() {
                @Override
                public void close(@Nonnull AutoSyncingDatastoreWithLock object) {
                    object.close();
                }
            });
            mPathManager = new PathManager(mDatastoreRef);
            mUserId = account.getUserId();
        }

        public PathManager getPathManager() {
            return mPathManager;
        }

        public Bundle toBundle() {
            final Bundle bundle = new Bundle();
            bundle.putString(BUNDLE_KEY_USER_ID, mUserId);
            return bundle;
        }

        public static UserState fromBundle(UserStateManager userStateManager, Bundle bundle) {
            String desiredUserId = bundle.getString(BUNDLE_KEY_USER_ID);
            if (desiredUserId == null) {
                throw new IllegalStateException("Missing user id in bundle");
            }
            UserState currentMainUserState = userStateManager.getMainUserState();
            if (currentMainUserState.mUserId.equals(desiredUserId)) {
                return currentMainUserState;
            } else {
                return null;
            }
        }

        public void destroy() {
            mDatastoreRef.shutdown();
        }
    }

}
