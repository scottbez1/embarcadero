package com.scottbezek.embarcadero.app.ui;

import com.scottbezek.embarcadero.app.R;
import com.scottbezek.embarcadero.app.model.data.PathListItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

public class PathListItemRow extends FrameLayout {

    private final TextView mTextView;

    public PathListItemRow(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.item_row_path, this, true);
        mTextView = (TextView)findViewById(R.id.path_text);
    }

    public void setData(PathListItem pathListItem) {
        mTextView.setText(pathListItem.getRecordId() + " -- " + pathListItem.getPathSegmentCount());
    }
}
