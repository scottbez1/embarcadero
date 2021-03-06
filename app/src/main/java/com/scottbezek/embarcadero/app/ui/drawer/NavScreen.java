package com.scottbezek.embarcadero.app.ui.drawer;

import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.dropbox.sync.android.DbxAccountInfo;
import com.scottbezek.embarcadero.app.model.data.PathListItem;
import com.scottbezek.embarcadero.app.ui.drawer.pathlist.PathListScreen;
import com.scottbezek.embarcadero.app.ui.drawer.pathlist.PathListScreen.PathSelectedListener;

import java.util.List;

import rx.Observable;

public class NavScreen extends LinearLayout {

    public NavScreen(Context context, Observable<DbxAccountInfo> accountInfo,
                     Observable<List<PathListItem>> pathList,
                     PathSelectedListener pathSelectionListener) {
        super(context);

        setBackgroundColor(Color.parseColor("#f4f4f4"));
        setOrientation(VERTICAL);

        addView(new AccountInfoPanel(context, accountInfo),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(new PathListScreen(context, pathList, pathSelectionListener),
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    }
}
