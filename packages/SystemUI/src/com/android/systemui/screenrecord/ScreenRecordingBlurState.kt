/*
 * Copyright (C) 2026 The Evolution X Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.screenrecord

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-local state used to suppress expensive SystemUI blur while the
 * built-in screen recorder is active.
 */
object ScreenRecordingBlurState {
    private val _recordingActive = MutableStateFlow(false)

    @Volatile private var keepBlurForNextRecording = false
    @Volatile private var keepBlurForCurrentRecording = false
    @Volatile private var recordingSessionActive = false

    val recordingActive = _recordingActive.asStateFlow()

    @JvmStatic
    fun isRecordingActive(): Boolean = _recordingActive.value

    @JvmStatic
    fun setKeepBlurForNextRecording(keepBlur: Boolean) {
        keepBlurForNextRecording = keepBlur
    }

    @JvmStatic
    fun clearPendingBlurPreference() {
        keepBlurForNextRecording = false
    }

    @JvmStatic
    fun setRecordingActive(active: Boolean, disableBlurByDefault: Boolean) {
        if (active) {
            if (!recordingSessionActive) {
                keepBlurForCurrentRecording = keepBlurForNextRecording
                clearPendingBlurPreference()
                recordingSessionActive = true
            }

            _recordingActive.value =
                disableBlurByDefault && !keepBlurForCurrentRecording
        } else {
            _recordingActive.value = false
            recordingSessionActive = false
            keepBlurForCurrentRecording = false
        }
    }
}
