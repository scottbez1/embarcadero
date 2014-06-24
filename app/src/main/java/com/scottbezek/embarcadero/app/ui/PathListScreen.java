package com.scottbezek.embarcadero.app.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.scottbezek.embarcadero.app.model.PathManager;
import com.scottbezek.embarcadero.app.model.data.PathListItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PathListScreen extends FrameLayout {

    private static final String TAG = PathListScreen.class.getName();

    private final PathListAdapter mAdapter;
    private final Observable<List<PathListItem>> mPathListObservable;
    private Subscription mPathListSubscription;

    public PathListScreen(Context context, PathManager pathManager) {
        super(context);

        mPathListObservable = pathManager.getPathList(Schedulers.io());
        mAdapter = new PathListAdapter(context);

        ListView lv = new ListView(context);
        lv.setAdapter(mAdapter);
        addView(lv, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mPathListSubscription = mPathListObservable
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<PathListItem>>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onNext(List<PathListItem> pathListItems) {
                        mAdapter.setData(pathListItems);
                    }
                });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPathListSubscription.unsubscribe();
        mAdapter.setData(Collections.<PathListItem>emptyList());
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
