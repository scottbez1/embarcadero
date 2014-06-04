package com.scottbezek.embarcadero.app.ui;

import com.scottbezek.embarcadero.app.model.PathManager;
import com.scottbezek.embarcadero.app.model.data.PathListItem;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.QueryLoader;
import com.scottbezek.embarcadero.app.util.DatastoreUtils.QueryLoader.LoaderCallback;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathListScreen extends FrameLayout {

    private static final String TAG = PathListScreen.class.getName();

    private final PathListAdapter mAdapter;
    private final QueryLoader<List<PathListItem>> mQueryLoader;

    public PathListScreen(Context context, PathManager pathManager) {
        super(context);

        mQueryLoader = pathManager.getPathListLoader();
        mAdapter = new PathListAdapter(context);

        ListView lv = new ListView(context);
        lv.setAdapter(mAdapter);
        addView(lv, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mQueryLoader.start(new LoaderCallback<List<PathListItem>>() {
            @Override
            public void onNewData(List<PathListItem> data) {
                Log.d(TAG, "Got new data, setting it: " + data.size());
                mAdapter.setData(data);
            }
        }, new Handler());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mQueryLoader.stop();
        mAdapter.setData(Collections.<PathListItem>emptyList());
    }

    private static class PathListAdapter extends BaseAdapter {

        private final Context mContext;
        private final List<PathListItem> mData = new ArrayList<>();

        public PathListAdapter(Context context) {
            mContext = context;
        }

        public void setData(List<PathListItem> data) {
            mData.clear();
            mData.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final PathListItemRow row;
            if (convertView != null) {
                row = (PathListItemRow)convertView;
            } else {
                row = new PathListItemRow(mContext);
            }
            row.setData(mData.get(position));
            return row;
        }
    }
}
