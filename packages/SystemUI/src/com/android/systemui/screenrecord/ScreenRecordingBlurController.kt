/*
 * Copyright (C) 2026 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.screenrecord

import android.content.Context
import com.android.systemui.CoreStartable
import com.android.systemui.Prefs
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.res.R
import com.android.systemui.settings.UserContextProvider
import javax.inject.Inject

/** Observe the common recording lifecycle without adding fields to recording parameters. */
@SysUISingleton
class ScreenRecordingBlurController @Inject constructor(
    @Application private val context: Context,
    private val controller: ScreenRecordUxController,
    private val userContextProvider: UserContextProvider,
) : CoreStartable, ScreenRecordUxController.StateChangeCallback {
    override fun start() {
        if (context.resources.getBoolean(R.bool.config_screenRecorderDisableBlur)) {
            controller.addCallback(this)
        }
    }

    override fun onRecordingStart() {
        val keepBlur = Prefs.getInt(userContextProvider.userContext, "screenrecord_keep_blur", 0) == 1
        ScreenRecordingBlurState.setSuppressed(!keepBlur)
    }

    override fun onRecordingEnd() {
        ScreenRecordingBlurState.setSuppressed(false)
    }
}
