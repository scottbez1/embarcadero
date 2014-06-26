package com.scottbezek.embarcadero.app.util;

import android.view.View;
import android.view.View.OnAttachStateChangeListener;

import rx.Observable;
import rx.Observer;
import rx.Subscription;

public class SubscribeWhileAttached<T> implements OnAttachStateChangeListener {

    private final Observable<T> mObservable;
    private final Observer<T> mObserver;

    private Subscription mSubscription = null;

    public SubscribeWhileAttached(Observable<T> observable, Observer<T> onSubscribe) {
        mObservable = observable;
        mObserver = onSubscribe;
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        Asserts.assertNull(mSubscription);
        mSubscription = mObservable.subscribe(mObserver);
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        mSubscription.unsubscribe();
        mSubscription = null;
    }
}
