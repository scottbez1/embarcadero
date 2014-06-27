package com.scottbezek.embarcadero.app.ui.drawer.pathlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.scottbezek.embarcadero.app.R;

public class PathListItemRow extends LinearLayout {

    private final TextView mPathDate;
    private final TextView mTextView;

    public PathListItemRow(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.item_row_path, this, true);
        mPathDate = (TextView)findViewById(R.id.path_date);
        mTextView = (TextView)findViewById(R.id.path_text);
    }

    public void setData(String date, String bogus) {
        mPathDate.setText(date);
        mTextView.setText(bogus);
    }
}
