package com.scottbezek.embarcadero.app.model;

import android.location.Location;

import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;
import com.dropbox.sync.android.DbxTable.QueryResult;
import com.scottbezek.embarcadero.app.model.data.PathCoord;
import com.scottbezek.embarcadero.app.model.data.PathListItem;
import com.scottbezek.embarcadero.app.model.location.LocationUpdateProvider;
import com.scottbezek.embarcadero.app.model.location.LocationUpdateQueue;
import com.scottbezek.embarcadero.app.util.DatastoreUtils;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.AutoSyncingDatastoreWithLock;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.DataStream;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.DatastoreQuery;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.DatastoreRowQuery;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.DatastoreTableQuery;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.DatastoreWithLock;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.DatastoreWithLock.OnSyncListener;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.QueryLoader;
import com.scottbezek.embarcadero.app.util.RefCountedObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.Subscriptions;

/**
 */
public class PathManager {

    private final RefCountedObject<AutoSyncingDatastoreWithLock> mDatastoreRef;

    private Thread mPathRecordThread = null;
    private final AtomicBoolean mShouldStopPathRecording = new AtomicBoolean();
    private final BehaviorSubject<RecordingState> mRecordingStateSubject = BehaviorSubject.create(new RecordingState(false, null));

    public PathManager(@Nonnull RefCountedObject<AutoSyncingDatastoreWithLock> datastoreRef) {
        mDatastoreRef = datastoreRef;
    }

    @Immutable
    public class RecordingState {

        private final boolean mRecording;

        @CheckForNull
        private final String mPathRecordId;

        public RecordingState(boolean recording, String pathRecordId) {
            mRecording = recording;
            mPathRecordId = pathRecordId;
        }

        public boolean isRecording() {
            return mRecording;
        }

        @CheckForNull
        public String getPathRecordId() {
            return mPathRecordId;
        }
    }

    public void startRecording(final LocationUpdateProvider locationProvider) {
        if (mPathRecordThread != null) {
            throw new IllegalStateException("Already recording!");
        }
        mRecordingStateSubject.onNext(new RecordingState(true, null));
        mShouldStopPathRecording.set(false);
        mPathRecordThread = new ThreadWithDatastore(mDatastoreRef) {
            @Override
            protected void runWithDatastore(DatastoreWithLock datastoreWithLock) {
                final DbxDatastore datastore = datastoreWithLock.getDatastore();
                final Object datastoreLock = datastoreWithLock.getLock();
                final DbxTable pathsTable = datastore.getTable("paths");
                final LocationUpdateQueue locationUpdateQueue = new LocationUpdateQueue(locationProvider);

                final Location lastLocation = locationProvider.getLastLocation();

                final PathRecordWriter pathWriter;
                synchronized (datastoreLock) {
                    DbxRecord pathRecord = pathsTable.insert();
                    pathWriter = new PathRecordWriter(pathRecord);
                    pathWriter.setStartTime(System.currentTimeMillis());

                    // If we know our current location, add it immediately so the path starts with at least one coord
                    if (lastLocation != null) {
                        // TODO(sbezek): ignore if last location is too old?
                        pathWriter.addLocation(lastLocation);
                    }

                    if (!DatastoreUtils.syncQuietly(datastoreWithLock)) {
                        return;
                    }
                    mRecordingStateSubject.onNext(new RecordingState(true, pathRecord.getId()));
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
                            if (!DatastoreUtils.syncQuietly(datastoreWithLock)) {
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
                    DatastoreUtils.syncQuietly(datastoreWithLock);
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
        mRecordingStateSubject.onNext(new RecordingState(false, null));
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

    private static final DatastoreQuery<List<PathListItem>> PATH_LIST_QUERY =
            new DatastoreTableQuery<List<PathListItem>>("paths", new DbxFields()) {
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

    private static DatastoreQuery<List<PathCoord>> getPathCoordsQuery(String pathRecordId) {
        return new DatastoreRowQuery<List<PathCoord>>("paths", pathRecordId) {
            @Override
            public List<PathCoord> createImmutableSnapshot(@CheckForNull DbxRecord result) {
                if (result == null) {
                    // TODO(sbezek): make a useful RuntimeException subclass: RecordNotFoundException?
                    throw new RuntimeException("Record not found");
                } else {
                    return PathCoord.listFrom(result);
                }
            }
        };
    }

    public DataStream<List<PathListItem>> getPathListLoader() {
        return new QueryLoader<>(mDatastoreRef, PATH_LIST_QUERY);
    }

    public Observable<List<PathListItem>> getPathList(Scheduler queryExecutionScheduler) {
        return QueryObservable.createObservable(mDatastoreRef, PATH_LIST_QUERY, queryExecutionScheduler);
    }

    public Observable<List<PathCoord>> getPathCoords(String pathRecordId, Scheduler queryExecutionScheduler) {
        return QueryObservable.createObservable(mDatastoreRef, getPathCoordsQuery(pathRecordId), queryExecutionScheduler);
    }

    public Observable<RecordingState> getRecordingState() {
        return mRecordingStateSubject.asObservable();
    }

    private static class QueryObservable<T> implements Observable.OnSubscribe<T> {

        private final RefCountedObject<? extends DatastoreWithLock> mDatastoreRef;
        private final DatastoreQuery<T> mQuery;
        private final Scheduler mScheduler;

        public QueryObservable(RefCountedObject<? extends DatastoreWithLock> datastoreRef, DatastoreQuery<T> query, Scheduler scheduler) {
            mDatastoreRef = datastoreRef;
            mQuery = query;
            mScheduler = scheduler;
        }

        public static <T> Observable<T> createObservable(RefCountedObject<? extends DatastoreWithLock> datastoreRef, DatastoreQuery<T> query, Scheduler scheduler) {
            return Observable.create(new QueryObservable(datastoreRef, query, scheduler));
        }

        @Override
        public void call(final Subscriber<? super T> subscriber) {
            final Worker worker = mScheduler.createWorker();

            final DatastoreWithLock datastore = mDatastoreRef.acquire();
            // Register a change listener for recurring loads
            final OnSyncListener requeryTrigger = new OnSyncListener() {
                @Override
                public void onSynced() {
                    worker.schedule(new Action0() {
                        @Override
                        public void call() {
                            try {
                                subscriber.onNext(mQuery.executeOnDatastore(datastore));
                            } catch (DbxException e) {
                                subscriber.onError(e);
                                // TODO(sbezek): unregister change listener and stop emitting data?
                            } catch (Throwable e) {
                                subscriber.onError(e);
                            }
                        }
                    });
                }
            };
            datastore.addSyncListener(requeryTrigger);

            // Trigger an initial load...
            requeryTrigger.onSynced();

            // When unsubscribed, unregister the change listener and release the datastore
            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    datastore.removeSyncListener(requeryTrigger);
                    mDatastoreRef.release(datastore);
                    worker.unsubscribe();
                }
            }));

        }
    }
}
