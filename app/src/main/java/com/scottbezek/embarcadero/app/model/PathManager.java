package com.scottbezek.embarcadero.app.model;

import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastore.SyncStatusListener;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxList;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;
import com.scottbezek.embarcadero.app.model.UserStateManager.RefCountedObject;
import com.scottbezek.embarcadero.app.model.data.PathListItem;
import com.scottbezek.embarcadero.app.model.location.LocationUpdateProvider;
import com.scottbezek.embarcadero.app.model.location.LocationUpdateQueue;
import com.scottbezek.embarcadero.app.statestream.SuperStateStream;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.PotentialChangeListener;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

/**
 */
public class PathManager {

    private final Object mDatastoreLock = new Object();

    private final RefCountedObject<DbxDatastore> mDatastoreRef;

    private Thread mPathRecordThread = null;
    private final AtomicBoolean mShouldStopPathRecording = new AtomicBoolean();

    private final SuperStateStream<List<PathListItem>> mPathListStream;

    public PathManager(@Nonnull RefCountedObject<DbxDatastore> datastoreRef) {
        mDatastoreRef = datastoreRef;

        mPathListStream = new SuperStateStream<List<PathListItem>>() {

            private final SyncStatusListener mNotifyOnChangeListener = new PotentialChangeListener() {
                @Override
                public void onPotentialDataChange(DbxDatastore datastore) {
                    final List<PathListItem> pathList;
                    // XXX TODO(sbezek): don't do this on the main thread
                    synchronized (mDatastoreLock) {
                        pathList = getPathList(datastore);
                    }
                    update(pathList);
                }
            };

            private DbxDatastore mLiveDatastore;

            @Override
            public void onFirstListenerRegistered() {
                if (mLiveDatastore != null) {
                    throw new IllegalStateException();
                }
                mLiveDatastore = mDatastoreRef.acquire();
                mLiveDatastore.addSyncStatusListener(mNotifyOnChangeListener);
            }

            @Override
            public void onLastListenerUnregistered() {
                mLiveDatastore.removeSyncStatusListener(mNotifyOnChangeListener);
                mDatastoreRef.release(mLiveDatastore);
                mLiveDatastore = null;
            }
        };
    }

    public void startRecording(final LocationUpdateProvider locationProvider) {
        if (mPathRecordThread != null) {
            throw new IllegalStateException("Already recording!");
        }
        mShouldStopPathRecording.set(false);
        mPathRecordThread = new ThreadWithDatastore(mDatastoreRef) {
            @Override
            protected void runWithDatastore(DbxDatastore datastore) {
                final DbxTable pathsTable = datastore.getTable("paths");
                final LocationUpdateQueue locationUpdateQueue = new LocationUpdateQueue(locationProvider);

                final PathRecordWriter pathWriter;
                synchronized (mDatastoreLock) {
                    pathWriter = new PathRecordWriter(pathsTable.insert());
                    pathWriter.setStartTime(System.currentTimeMillis());
//                    if (!DatastoreUtils.syncQuietly(datastore)) {
//                        return;
//                    }
                }
                locationUpdateQueue.enableProducer();

                // TODO(sbezek): maybe want to split out a "resumeRecording" method for Service restarts following process death?
                // TODO(sbezek): Aquire wakelocks, start services, and all that jazz!

                // Pull Location updates from the queue and apply them to the PathRecordWriter
                while (true){
                    try {
                        Location updatedLocation = locationUpdateQueue.take();
                        synchronized (mDatastoreLock) {
                            pathWriter.addLocation(updatedLocation);
//                            if (!DatastoreUtils.syncQuietly(datastore)) {
//                                break;
//                            }
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
                synchronized (mDatastoreLock) {
                    pathWriter.setStopTime(System.currentTimeMillis());
//                    DatastoreUtils.syncQuietly(datastore);
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

        private final RefCountedObject<DbxDatastore> mDatastoreRef;

        public ThreadWithDatastore(RefCountedObject<DbxDatastore> datastoreRef) {
            mDatastoreRef = datastoreRef;
        }

        @Override
        public final void run() {
            final DbxDatastore ds = mDatastoreRef.acquire();
            try {
                runWithDatastore(ds);
            } finally {
                mDatastoreRef.release(ds);
            }
        }

        protected abstract void runWithDatastore(DbxDatastore datastore);
    }

    private static List<PathListItem> getPathList(DbxDatastore datastore) {
        try {
            DbxTable table = datastore.getTable("paths");
            List<DbxRecord> paths = table.query().asList();
            List<PathListItem> result = new ArrayList<>(paths.size());
            for (DbxRecord record : paths) {
                final DbxList coordinateTimeList = record.getList("coord_time");
                final PathListItem item = new PathListItem(
                        record.getId(),
                        record.getString("name"),
                        record.getLong("start_time"),
                        record.getLong("stop_time"),
                        coordinateTimeList == null ? 0 : coordinateTimeList.size());
                result.add(item);
            }
            return result;
        } catch (DbxException e) {
            // XXX TODO
            throw new RuntimeException(e);
        }
    }

    public SuperStateStream<List<PathListItem>> getPathListStream() {
        return mPathListStream;
    }
}
