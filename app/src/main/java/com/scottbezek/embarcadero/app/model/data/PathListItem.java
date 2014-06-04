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

    private final long mStartTimeMillis;

    @CheckForNull
    private final Long mStopTimeMillis;

    private final int mPathSegmentCount;

    public PathListItem(@Nonnull String recordId, @CheckForNull String name, long startTimeMillis, @CheckForNull Long stopTimeMillis, int pathSegmentCount) {
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

    @CheckForNull
    public String getName() {
        return mName;
    }

    public long getStartTimeMillis() {
        return mStartTimeMillis;
    }

    @CheckForNull
    public Long getStopTimeMillis() {
        return mStopTimeMillis;
    }

    public int getPathSegmentCount() {
        return mPathSegmentCount;
    }
}
