package com.scottbezek.embarcadero.app.util;

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

    public static abstract class PotentialChangeListener implements SyncStatusListener {
        private boolean mHadIncoming = false;
        private boolean mHadOutgoing = false;

        @Override
        public void onDatastoreStatusChange(DbxDatastore datastore) {
            DbxDatastoreStatus status = datastore.getSyncStatus();
            if ((mHadIncoming && !status.hasIncoming)
                    || (!mHadOutgoing && status.hasOutgoing)) {
                onPotentialDataChange(datastore);
            }
            mHadIncoming = status.hasIncoming;
            mHadOutgoing = status.hasOutgoing;
        }

        public abstract void onPotentialDataChange(DbxDatastore datastore);
    }
}
