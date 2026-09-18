/*
 * Copyright (C) 2026 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.server.pm.parsing.library;

import com.android.internal.pm.parsing.pkg.ParsedPackage;

/** Supplies the split MiSys compatibility library to the preinstalled MIUI camera. */
final class XiaomiCameraSharedLibraryUpdater extends PackageSharedLibraryUpdater {
    static final String LIBRARY_NAME = "xiaomi-misys";
    private final boolean mLibraryAvailable;

    XiaomiCameraSharedLibraryUpdater(boolean libraryAvailable) {
        mLibraryAvailable = libraryAvailable;
    }

    @Override
    public void updatePackage(ParsedPackage parsedPackage, boolean isSystemApp,
            boolean isUpdatedSystemApp) {
        if (mLibraryAvailable && (isSystemApp || isUpdatedSystemApp)
                && "com.android.camera".equals(parsedPackage.getPackageName())) {
            // Native interfaces are already installed by the camera device tree.
            // The old APK expects these Java classes from MIUI's framework, not
            // from a uses-library entry. Do not inject into a user-installed clone.
            // This changes a classpath only; it grants no permission or HAL access.
            prefixRequiredLibrary(parsedPackage, LIBRARY_NAME);
        }
    }
}
