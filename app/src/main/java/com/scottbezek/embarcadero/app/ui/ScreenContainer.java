package com.scottbezek.embarcadero.app.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class ScreenContainer extends FrameLayout {

    public ScreenContainer(Context context) {
        this(context, null);
    }

    public ScreenContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScreenContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void swapView(View newView) {
        removeAllViews();
        addView(newView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
