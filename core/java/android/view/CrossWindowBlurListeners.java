/**
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.view;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.util.Preconditions;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Class that holds all registered {@link CrossWindowBlurEnabledListener}s. It listens
 * for updates from the WindowManagerService and updates all registered listeners.
 * @hide
 */
public final class CrossWindowBlurListeners {
    private static final String TAG = "CrossWindowBlurListeners";

    // property for background blur support in surface flinger
    private static final String BLUR_PROPERTY = "ro.surface_flinger.supports_background_blur";
    public static final boolean CROSS_WINDOW_BLUR_SUPPORTED =
            SystemProperties.getBoolean(BLUR_PROPERTY, false);

    private static volatile CrossWindowBlurListeners sInstance;
    private static final Object sLock = new Object();

    private final BlurEnabledListenerInternal mListenerInternal = new BlurEnabledListenerInternal();
    private final ArrayMap<Consumer<Boolean>, Executor> mListeners = new ArrayMap();
    private boolean mInternalListenerAttached = false;
    private boolean mCrossWindowBlurEnabled;
    private boolean mBlurSuppressedForProcess;

    private CrossWindowBlurListeners() {}

    /**
     * Returns a CrossWindowBlurListeners instance
     */
    public static CrossWindowBlurListeners getInstance() {
        CrossWindowBlurListeners instance = sInstance;
        if (instance == null) {

            synchronized (sLock) {
                instance = sInstance;
                if (instance == null) {
                    instance = new CrossWindowBlurListeners();
                    sInstance = instance;
                }
            }
        }
        return instance;
    }

    public boolean isCrossWindowBlurEnabled() {
        synchronized (sLock) {
            attachInternalListenerIfNeededLocked();
            return isBlurEnabledLocked();
        }
    }

    /**
     * Temporarily suppresses cross-window blur in this process only. The system's
     * enabled state and user settings are retained for restoration.
     */
    public void setBlurSuppressedForProcess(boolean suppressed) {
        synchronized (sLock) {
            final boolean wasEnabled = isBlurEnabledLocked();
            mBlurSuppressedForProcess = suppressed;
            final boolean enabled = isBlurEnabledLocked();
            if (wasEnabled != enabled) {
                notifyListenersLocked(enabled);
            }
        }
    }

    private boolean isBlurEnabledLocked() {
        return mCrossWindowBlurEnabled && !mBlurSuppressedForProcess;
    }

    private void notifyListenersLocked(boolean enabled) {
        // A direct listener can unregister or change policy during delivery.
        final ArrayMap<Consumer<Boolean>, Executor> listeners = new ArrayMap<>(mListeners);
        for (int i = 0; i < listeners.size(); i++) {
            if (enabled != isBlurEnabledLocked()) {
                break;
            }
            notifyListener(listeners.keyAt(i), listeners.valueAt(i), enabled);
        }
    }

    public void addListener(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Boolean> listener) {
        Preconditions.checkNotNull(listener, "listener cannot be null");
        Preconditions.checkNotNull(executor, "executor cannot be null");

        synchronized (sLock) {
            attachInternalListenerIfNeededLocked();

            mListeners.put(listener, executor);
            notifyListener(listener, executor, isBlurEnabledLocked());
        }
    }


    public void removeListener(Consumer<Boolean> listener) {
        Preconditions.checkNotNull(listener, "listener cannot be null");

        synchronized (sLock) {
            mListeners.remove(listener);

            if (mInternalListenerAttached && mListeners.size() == 0) {
                try {
                    WindowManagerGlobal.getWindowManagerService()
                            .unregisterCrossWindowBlurEnabledListener(mListenerInternal);
                    mInternalListenerAttached = false;
                } catch (RemoteException e) {
                    Log.d(TAG, "Could not unregister ICrossWindowBlurEnabledListener");
                }
            }
        }
    }

    private void attachInternalListenerIfNeededLocked() {
        if (!mInternalListenerAttached) {
            try {
                mCrossWindowBlurEnabled = WindowManagerGlobal.getWindowManagerService()
                        .registerCrossWindowBlurEnabledListener(mListenerInternal);
                mInternalListenerAttached = true;
            } catch (RemoteException e) {
                Log.d(TAG, "Could not register ICrossWindowBlurEnabledListener");
            }
        }
    }

    private void notifyListener(Consumer<Boolean> listener, Executor executor, boolean enabled) {
        executor.execute(() -> listener.accept(enabled));
    }

    private final class BlurEnabledListenerInternal extends ICrossWindowBlurEnabledListener.Stub {
        @Override
        public void onCrossWindowBlurEnabledChanged(boolean enabled) {
            synchronized (sLock) {
                final boolean wasEnabled = isBlurEnabledLocked();
                mCrossWindowBlurEnabled = enabled;
                final boolean effectiveEnabled = isBlurEnabledLocked();
                if (wasEnabled == effectiveEnabled) {
                    return;
                }

                final long token = Binder.clearCallingIdentity();
                try {
                    notifyListenersLocked(effectiveEnabled);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }
    }
}
