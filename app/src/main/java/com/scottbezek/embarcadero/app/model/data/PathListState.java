package com.scottbezek.embarcadero.app.model.data;

import java.util.List;

import javax.annotation.CheckForNull;

public class PathListState {

    @CheckForNull
    private final List<PathListItem> mPaths;

    public PathListState(@CheckForNull List<PathListItem> paths) {
        mPaths = paths;
    }
}
