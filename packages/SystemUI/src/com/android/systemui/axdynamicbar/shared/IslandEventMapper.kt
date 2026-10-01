/*
 * Copyright 2025-2026 AxionOS
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

package com.android.systemui.axdynamicbar.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.model.RecordingState
import com.android.systemui.res.R

/**
 * How much room an event's chip takes.
 *
 * Size signals urgency, shape signals kind — this is what separates events now that they all
 * share one wallpaper-derived accent. See [ChipTier.height] and [ChipTier.cornerRadius].
 */
internal enum class ChipTier {
    /** Passive status, worth a glance and nothing more: a small circle. */
    PASSIVE,
    /** Active information the user is tracking: the standard pill. */
    ACTIVE,
    /** Something is happening right now: a larger squircle. */
    LIVE,
}

internal data class EventStyle(
    val tone: IslandTone,
    val tier: ChipTier,
    val icon: ImageVector?,
    val labelRes: Int,
)

/**
 * The event's accent as an actual colour: the saturated Material role for [tone]. Composable
 * because the role is read from the scheme, which is what makes every chip wallpaper-derived.
 */
internal val EventStyle.accent: Color
    @Composable get() = tone.tint

internal fun eventStyleFor(event: IslandEvent): EventStyle = when (event) {
    is IslandEvent.AudioRecording -> EventStyle(
        tier = ChipTier.LIVE,
        tone = when (event.state) {
            RecordingState.RECORDING -> IslandTone.ACCENT
            RecordingState.PAUSED -> IslandTone.ACCENT
            RecordingState.SAVED -> IslandTone.POSITIVE
        },
        icon = when (event.state) {
            RecordingState.RECORDING -> Icons.Filled.Mic
            RecordingState.PAUSED -> Icons.Filled.Pause
            RecordingState.SAVED -> Icons.Filled.CheckCircle
        },
        labelRes = when (event.state) {
            RecordingState.RECORDING -> R.string.ax_dynamic_bar_recording
            RecordingState.PAUSED -> R.string.ax_dynamic_bar_paused
            RecordingState.SAVED -> R.string.ax_dynamic_bar_saved
        },
    )
    is IslandEvent.Media -> EventStyle(
        tier = ChipTier.ACTIVE,
        tone = IslandTone.ACCENT,
        icon = Icons.Filled.MusicNote,
        labelRes = R.string.ax_dynamic_bar_music,
    )
    is IslandEvent.PromotedOngoing -> EventStyle(
        tier = ChipTier.ACTIVE,
        tone = IslandTone.ACCENT,
        icon = null,
        labelRes = R.string.ax_dynamic_bar_on,
    )
    is IslandEvent.NowPlaying -> EventStyle(
        tier = ChipTier.ACTIVE,
        tone = IslandTone.ACCENT,
        icon = Icons.Filled.MusicNote,
        labelRes = R.string.ax_dynamic_bar_now_playing,
    )
    is IslandEvent.Sports -> EventStyle(
        tier = when (event.status) {
            IslandEvent.GameStatus.LIVE, IslandEvent.GameStatus.HALFTIME -> ChipTier.LIVE
            else -> ChipTier.ACTIVE
        },
        tone = when (event.status) {
            IslandEvent.GameStatus.LIVE, IslandEvent.GameStatus.HALFTIME -> IslandTone.POSITIVE
            IslandEvent.GameStatus.PRE_GAME -> IslandTone.ACCENT
            IslandEvent.GameStatus.FINAL -> IslandTone.POSITIVE
        },
        icon = null,
        labelRes = R.string.ax_dynamic_bar_on,
    )
    is IslandEvent.Bluetooth -> EventStyle(
        tier = ChipTier.PASSIVE,
        tone = IslandTone.ACCENT,
        icon = Icons.Filled.Bluetooth,
        labelRes = R.string.ax_dynamic_bar_connected,
    )
    is IslandEvent.Hotspot -> EventStyle(
        tier = ChipTier.ACTIVE,
        tone = IslandTone.ACCENT,
        icon = Icons.Filled.Wifi,
        labelRes = R.string.ax_dynamic_bar_hotspot,
    )
    is IslandEvent.Charging -> EventStyle(
        tier = ChipTier.ACTIVE,
        tone = IslandTone.POSITIVE,
        icon = Icons.Filled.BatteryChargingFull,
        labelRes = if (event.isWireless) R.string.ax_dynamic_bar_wireless_charging
        else R.string.ax_dynamic_bar_charging,
    )
    is IslandEvent.Alarm -> EventStyle(
        tier = ChipTier.ACTIVE,
        tone = IslandTone.ATTENTION,
        icon = Icons.Filled.Alarm,
        labelRes = R.string.ax_dynamic_bar_alarm,
    )
    is IslandEvent.Timer -> EventStyle(
        tier = ChipTier.ACTIVE,
        tone = IslandTone.ACCENT,
        icon = Icons.Filled.Timer,
        labelRes = R.string.ax_dynamic_bar_timer,
    )
    is IslandEvent.Stopwatch -> EventStyle(
        tier = ChipTier.ACTIVE,
        tone = IslandTone.ACCENT,
        icon = Icons.Filled.AvTimer,
        labelRes = R.string.ax_dynamic_bar_stopwatch,
    )
    is IslandEvent.RingerMode -> EventStyle(
        tier = ChipTier.PASSIVE,
        tone = when (event.mode) {
            AudioManager.RINGER_MODE_SILENT -> IslandTone.ATTENTION
            AudioManager.RINGER_MODE_VIBRATE -> IslandTone.ATTENTION
            else -> IslandTone.ACCENT
        },
        icon = when (event.mode) {
            AudioManager.RINGER_MODE_SILENT -> Icons.Filled.VolumeOff
            AudioManager.RINGER_MODE_VIBRATE -> Icons.Filled.Vibration
            else -> Icons.Filled.VolumeUp
        },
        labelRes = when (event.mode) {
            AudioManager.RINGER_MODE_SILENT -> R.string.ax_dynamic_bar_silent
            AudioManager.RINGER_MODE_VIBRATE -> R.string.ax_dynamic_bar_vibrate
            else -> R.string.ax_dynamic_bar_ring
        },
    )
    is IslandEvent.Vpn -> EventStyle(
        tier = ChipTier.PASSIVE,
        tone = if (event.isValidated) IslandTone.POSITIVE else IslandTone.ATTENTION,
        icon = Icons.Filled.VpnKey,
        labelRes = if (event.isBranded) R.string.ax_dynamic_bar_vpn_active
        else R.string.ax_dynamic_bar_vpn_connected,
    )
    is IslandEvent.Clipboard -> EventStyle(
        tier = ChipTier.PASSIVE,
        tone = IslandTone.ACCENT,
        icon = Icons.Filled.ContentCopy,
        labelRes = R.string.ax_dynamic_bar_copied,
    )
    is IslandEvent.Torch -> EventStyle(
        tier = ChipTier.PASSIVE,
        tone = IslandTone.ACCENT,
        icon = Icons.Filled.FlashlightOn,
        labelRes = R.string.ax_dynamic_bar_flashlight,
    )
    is IslandEvent.Notification -> EventStyle(
        tier = ChipTier.ACTIVE,
        tone = IslandTone.ACCENT,
        icon = Icons.Filled.Notifications,
        labelRes = R.string.ax_dynamic_bar_on,
    )
    is IslandEvent.AppSwitch -> EventStyle(
        tier = ChipTier.PASSIVE,
        tone = IslandTone.ACCENT,
        icon = null,
        labelRes = R.string.ax_dynamic_bar_on,
    )
    is IslandEvent.BiometricUnlock -> EventStyle(
        tier = ChipTier.PASSIVE,
        tone = IslandTone.POSITIVE,
        icon = Icons.Filled.Fingerprint,
        labelRes = R.string.ax_dynamic_bar_unlocked,
    )
    is IslandEvent.KeyguardIndication -> EventStyle(
        tier = ChipTier.PASSIVE,
        tone = when (event.indicationType) {
            IslandEvent.KeyguardIndication.IndicationType.BIOMETRIC -> IslandTone.POSITIVE
            IslandEvent.KeyguardIndication.IndicationType.ALIGNMENT -> IslandTone.ATTENTION
            else -> IslandTone.ACCENT
        },
        icon = null,
        labelRes = R.string.ax_dynamic_bar_on,
    )
    is IslandEvent.AospChip -> EventStyle(
        tier = ChipTier.PASSIVE,
        tone = IslandTone.ACCENT,
        icon = Icons.Filled.Notifications,
        labelRes = R.string.ax_dynamic_bar_on,
    )
}
