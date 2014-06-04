package com.scottbezek.embarcadero.app.util;

import com.scottbezek.embarcadero.app.model.UserStateManager;

import javax.annotation.Nonnull;

public class RefCountedObject<T> {

        private final Factory<T> mFactory;
        private final Closer<T> mCloser;

        private boolean mShutdown = false;
        private T mObject = null;
        private int mRefCount = 0;

        public RefCountedObject(Factory<T> factory, Closer<T> closer) {
            mFactory = factory;
            mCloser = closer;
        }

        @Nonnull
        public synchronized T acquire() {
            if (mShutdown) {
                throw new IllegalStateException("Already shut down");
            }
            if (mObject == null) {
                mObject = mFactory.create();
                if (mObject == null) {
                    throw new AssertionError("Factory returned null");
                }
            }
            mRefCount++;
            return mObject;
        }

        public synchronized void release(@Nonnull Object object) {
            if (mShutdown) {
                throw new IllegalStateException("Already shut down");
            }
            if (mRefCount <= 0) {
                throw new IllegalStateException("Can't release with a ref count of " + mRefCount);
            }

            // Must be the exact same instance that was acquired
            if (object != mObject) {
                throw new IllegalStateException("Not the right object");
            }

            mRefCount--;
            if (mRefCount == 0) {
                mCloser.close(mObject);
                mObject = null;
            }
        }

        public synchronized void shutdown() {
            mShutdown = true;
            if (mObject != null) {
                // TODO(sbezek): should this actually throw an exception? maybe it should be strict about releasing all refs before shutting down
                mCloser.close(mObject);
            }
        }

        public interface Factory<T> {
            @Nonnull
            T create();
        }

        public interface Closer<T> {

            /**
             * Called exactly once, when the item is no longer needed.
             */
            void close(@Nonnull T object);
        }
    }
