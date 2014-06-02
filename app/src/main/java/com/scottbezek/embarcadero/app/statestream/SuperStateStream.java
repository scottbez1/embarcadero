package com.scottbezek.embarcadero.app.statestream;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * A stream of states. Caches the most recent state, and provides that when new {@link
 * SuperStateStream.Listener}s subscribe for changes.
 */
public abstract class SuperStateStream<StateType> implements ListenerChangeListener {

    private final Object mStateLock = new Object();

    @GuardedBy("mStateLock")
    private final Map<Listener<StateType>, Handler>
            mListeners = new HashMap<Listener<StateType>, Handler>();

    private class ListenerStateChangeRunnable implements Runnable {

        private final Listener<StateType> mListener;

        private final Handler mHandler;

        private final StateType mState;

        public ListenerStateChangeRunnable(Listener<StateType> listener,
                Handler handler, StateType state) {
            mListener = listener;
            mHandler = handler;
            mState = state;
        }

        @Override
        public void run() {
            // Ensure the listener hasn't been unregistered or changed Loopers in the meantime
            synchronized (mStateLock) {
                if (!mHandler.equals(mListeners.get(mListener))) {
                    return;
                }
            }
            mListener.onStateChanged(mState);
        }
    }

    /**
     * Can be called on any thread to update the current state value.
     */
    public void update(@Nonnull final StateType newState) {
        synchronized (mStateLock) {
            // Copy the entries to avoid ConcurrentModificationExceptions while notifying
            Set<Pair<Listener<StateType>, Handler>> entrySetCopy =
                    new HashSet<Pair<Listener<StateType>, Handler>>();
            for (Entry<Listener<StateType>, Handler> entry : mListeners.entrySet()) {
                entrySetCopy.add(new Pair<Listener<StateType>, Handler>(entry.getKey(),
                        entry.getValue()));
            }

            for (Pair<Listener<StateType>, Handler> entry : entrySetCopy) {
                entry.second
                        .post(new ListenerStateChangeRunnable(entry.first, entry.second, newState));
            }
        }
    }

    /**
     * Subscribe for change callbacks (which will be delivered using the same {@link
     * android.os.Looper} as this method is called on). Before this method returns, the provided
     * listener's callback will be invoked with the current state.
     */
    public void subscribeInvoke(Listener<StateType> listener) {
        Looper currentLooper = Looper.myLooper();
        if (currentLooper == null) {
            throw new IllegalStateException("Must be called on a Looper thread");
        }
        synchronized (mStateLock) {
            if (mListeners.containsKey(listener)) {
                throw new IllegalStateException("Listener already registered");
            }
            mListeners.put(listener, new Handler(currentLooper));
//            listener.onStateChanged(mProvider.getCurrentState());
            if (mListeners.size() == 1) {
                onFirstListenerRegistered();
            }
        }
    }

    /**
     * Unsubscribe a listener. Must be called on the same Looper thread under which it was
     * subscribed.
     */
    public void unsubscribe(Listener<StateType> listener) {
        Looper currentLooper = Looper.myLooper();
        if (currentLooper == null) {
            throw new IllegalStateException("Must be called on a Looper thread");
        }
        synchronized (mStateLock) {
            Handler oldRegisteredHandler = mListeners.get(listener);
            if (oldRegisteredHandler == null) {
                throw new IllegalStateException("Listener not registered");
            } else if (!oldRegisteredHandler.getLooper().equals(currentLooper)) {
                throw new IllegalStateException(
                        "Listener is currently registered under a different Looper");
            }
            if (mListeners.remove(listener) == null) {
                throw new IllegalStateException();
            }
            if (mListeners.size() == 0) {
                onLastListenerUnregistered();
            }
        }
    }
//
//    public interface Provider<StateType> {
//        StateType getCurrentState();
//    }

    public interface Listener<StateType> {

        /**
         * Called when the state changes. Invoked on the {@link android.os.Looper} for which this
         * {@link SuperStateStream.Listener} was registered.
         *
         * @param state The new state value.
         */
        void onStateChanged(@Nonnull StateType state);
    }
}