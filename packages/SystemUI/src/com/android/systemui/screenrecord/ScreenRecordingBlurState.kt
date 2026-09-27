/*
 * Copyright (C) 2026 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.screenrecord

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-local blur suppression; independent of recorder codec and frame-rate options. */
object ScreenRecordingBlurState {
    private val suppression = MutableStateFlow(false)
    val recordingActive = suppression.asStateFlow()

    @JvmStatic
    fun isRecordingActive(): Boolean = suppression.value

    fun setSuppressed(suppressed: Boolean) {
        suppression.value = suppressed
    }
}
