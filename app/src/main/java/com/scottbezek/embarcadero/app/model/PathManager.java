package com.scottbezek.embarcadero.app.model;

import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxList;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;
import com.dropbox.sync.android.DbxTable.QueryResult;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.LiveQuery;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.QueryLoader;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.QueryLoader.LoaderCallback;
import com.scottbezek.embarcadero.app.util.RefCountedObject;
import com.scottbezek.embarcadero.app.model.data.PathListItem;
import com.scottbezek.embarcadero.app.model.location.LocationUpdateProvider;
import com.scottbezek.embarcadero.app.model.location.LocationUpdateQueue;
import com.scottbezek.embarcadero.app.util.DatastoreUtils;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.AutoSyncingDatastoreWithLock;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.DatastoreWithLock;

import android.location.Location;
import android.os.Handler;
import android.provider.ContactsContract.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

/**
 */
public class PathManager {

    private final RefCountedObject<AutoSyncingDatastoreWithLock> mDatastoreRef;

    private Thread mPathRecordThread = null;
    private final AtomicBoolean mShouldStopPathRecording = new AtomicBoolean();

    public PathManager(@Nonnull RefCountedObject<AutoSyncingDatastoreWithLock> datastoreRef) {
        mDatastoreRef = datastoreRef;
    }

    public void startRecording(final LocationUpdateProvider locationProvider) {
        if (mPathRecordThread != null) {
            throw new IllegalStateException("Already recording!");
        }
        mShouldStopPathRecording.set(false);
        mPathRecordThread = new ThreadWithDatastore(mDatastoreRef) {
            @Override
            protected void runWithDatastore(DatastoreWithLock datastoreWithLock) {
                final DbxDatastore datastore = datastoreWithLock.getDatastore();
                final Object datastoreLock = datastoreWithLock.getLock();
                final DbxTable pathsTable = datastore.getTable("paths");
                final LocationUpdateQueue locationUpdateQueue = new LocationUpdateQueue(locationProvider);

                final PathRecordWriter pathWriter;
                synchronized (datastoreLock) {
                    pathWriter = new PathRecordWriter(pathsTable.insert());
                    pathWriter.setStartTime(System.currentTimeMillis());
                    if (!DatastoreUtils.syncQuietly(datastore)) {
                        return;
                    }
                }
                locationUpdateQueue.enableProducer();

                // TODO(sbezek): maybe want to split out a "resumeRecording" method for Service restarts following process death?
                // TODO(sbezek): Aquire wakelocks, start services, and all that jazz!

                // Pull Location updates from the queue and apply them to the PathRecordWriter
                while (true){
                    try {
                        Location updatedLocation = locationUpdateQueue.take();
                        synchronized (datastoreLock) {
                            pathWriter.addLocation(updatedLocation);
                            if (!DatastoreUtils.syncQuietly(datastore)) {
                                break;
                            }
                        }
                    } catch (InterruptedException e) {
                        if (mShouldStopPathRecording.get()) {
                            // We were requested to stop, so shut down cleanly
                            break;
                        } else {
                            // I don't know why we were interrupted, so freak out!
                            throw new RuntimeException(e);
                        }
                    }
                }

                locationUpdateQueue.disableProducer();
                synchronized (datastoreLock) {
                    pathWriter.setStopTime(System.currentTimeMillis());
                    DatastoreUtils.syncQuietly(datastore);
                }
            }
        };
        mPathRecordThread.start();
    }

    public void stopRecording() {
        if (mPathRecordThread == null) {
            throw new IllegalStateException("Not recording");
        }
        mShouldStopPathRecording.set(true);
        mPathRecordThread.interrupt();

        mPathRecordThread = null;
    }

    /**
     * Helper for running something in a background thread while holding a reference to the datastore.
     */
    private static abstract class ThreadWithDatastore extends Thread {

        private final RefCountedObject<? extends DatastoreWithLock> mDatastoreRef;

        public ThreadWithDatastore(RefCountedObject<? extends DatastoreWithLock> datastoreRef) {
            mDatastoreRef = datastoreRef;
        }

        @Override
        public final void run() {
            final DatastoreWithLock ds = mDatastoreRef.acquire();
            try {
                runWithDatastore(ds);
            } finally {
                mDatastoreRef.release(ds);
            }
        }

        protected abstract void runWithDatastore(DatastoreWithLock datastore);
    }

    private static final LiveQuery<List<PathListItem>> sPathListQuery = new LiveQuery<List<PathListItem>>("paths", new DbxFields()) {
        @Override
        public List<PathListItem> createImmutableSnapshot(QueryResult queryResult) {
            List<PathListItem> result = new ArrayList<>();
            for (DbxRecord record : queryResult) {
                final PathListItem item = new PathListItem(
                        record.getId(),
                        record.hasField("name") ? record.getString("name") : null,
                        record.getLong("start_time"),
                        record.hasField("stop_time") ? record.getLong("stop_time") : null,
                        record.hasField("coord_time") ? record.getList("coord_time").size() : 0);
                result.add(item);
            }
            return result;
        }
    };

    public QueryLoader<List<PathListItem>> getPathListLoader() {
        return new QueryLoader<>(mDatastoreRef, sPathListQuery);
    }
}
