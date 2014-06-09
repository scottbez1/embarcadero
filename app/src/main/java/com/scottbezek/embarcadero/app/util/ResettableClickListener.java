package com.scottbezek.embarcadero.app.util;

import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;

public abstract class ResettableClickListener implements OnClickListener {

    private boolean mTriggered = false;

    @Override
    public final void onClick(View v) {
        if (mTriggered) {
            return;
        }
        mTriggered = true;
        onClick();
    }

    public void reset() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Must be called on main thread");
        }
        mTriggered = false;
    }

    protected abstract void onClick();
}
