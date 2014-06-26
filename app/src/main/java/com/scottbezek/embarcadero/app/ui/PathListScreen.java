package com.scottbezek.embarcadero.app.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.scottbezek.embarcadero.app.model.data.PathListItem;
import com.scottbezek.embarcadero.app.util.SubscribeWhileAttached;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

public class PathListScreen extends FrameLayout {

    private static final String TAG = PathListScreen.class.getName();

    private final PathListAdapter mAdapter;

    public PathListScreen(Context context, Observable<List<PathListItem>> pathList) {
        super(context);

        mAdapter = new PathListAdapter(context);

        ListView lv = new ListView(context);
        lv.setAdapter(mAdapter);
        addView(lv, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        addOnAttachStateChangeListener(new SubscribeWhileAttached<>(
                pathList
                        .distinctUntilChanged()
                        .observeOn(AndroidSchedulers.mainThread()),
                new Observer<List<PathListItem>>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onNext(List<PathListItem> pathListItems) {
                        mAdapter.setData(pathListItems);
                    }
                }
        ));
    }

    @Override
    protected void onAttachedToWindow() {
        mAdapter.setData(Collections.<PathListItem>emptyList());
        super.onAttachedToWindow();
    }

    public interface PathSelectedListener {
        void onPathSelected(String pathId);
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
