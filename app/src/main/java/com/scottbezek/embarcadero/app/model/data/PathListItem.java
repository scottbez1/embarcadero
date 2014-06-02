package com.scottbezek.embarcadero.app.model.data;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 */
@Immutable
public class PathListItem {

    @Nonnull
    private final String mRecordId;

    @CheckForNull
    private final String mName;

    @Nonnull
    private final long mStartTimeMillis, mStopTimeMillis;

    @Nonnull
    private final int mPathSegmentCount;

    public PathListItem(@Nonnull String recordId, @CheckForNull String name, @Nonnull long startTimeMillis, @Nonnull long stopTimeMillis, @Nonnull int pathSegmentCount) {
        mRecordId = recordId;
        mName = name;
        mStartTimeMillis = startTimeMillis;
        mStopTimeMillis = stopTimeMillis;
        mPathSegmentCount = pathSegmentCount;
    }

    @Nonnull
    public String getRecordId() {
        return mRecordId;
    }

    public String getName() {
        return mName;
    }

    @Nonnull
    public long getStartTimeMillis() {
        return mStartTimeMillis;
    }

    @Nonnull
    public long getStopTimeMillis() {
        return mStopTimeMillis;
    }

    @Nonnull
    public int getPathSegmentCount() {
        return mPathSegmentCount;
    }
}
