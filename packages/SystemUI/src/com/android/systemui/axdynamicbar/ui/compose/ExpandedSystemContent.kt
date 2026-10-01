package com.android.systemui.axdynamicbar.ui.compose

import android.media.AudioManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.systemui.axdynamicbar.shared.IslandActions
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.shared.*
import com.android.systemui.res.R

@Composable
internal fun ChargingExpanded(event: IslandEvent.Charging) {
    ExpandedCardLayout(
        accentColor = IslandTone.POSITIVE.tint,
        icon = {
            Icon(Icons.Filled.BatteryChargingFull, null, tint = IslandTone.POSITIVE.tint, modifier = Modifier.size(16.dp))
        },
        title = {
            Text(
                if (event.isWireless) stringResource(R.string.ax_dynamic_bar_wireless_charging)
                else stringResource(R.string.ax_dynamic_bar_charging),
                color = OnCardText,
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceMd),
            ) {
                StatusChip("${event.level}%", IslandTone.POSITIVE.tint)
                if (event.isPowerSave) {
                    StatusChip(stringResource(R.string.ax_dynamic_bar_battery_saver), IslandTone.ATTENTION.tint)
                }
                // The time-to-full estimate used to sit in a trailing column of its own; on the
                // caption line it costs no height.
                if (!event.timeRemaining.isNullOrEmpty()) {
                    Text(
                        "${event.timeRemaining} ${stringResource(R.string.ax_dynamic_bar_until_full)}",
                        color = SubtleGray,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        },
    )
}

@Composable
internal fun BluetoothExpanded(event: IslandEvent.Bluetooth, interactor: IslandActions) {
    ExpandedCardLayout(
        accentColor = IslandTone.ACCENT.tint,
        icon = {
            event.deviceIcon?.let {
                Image(
                    bitmap = it.toScaledBitmap(20.dp),
                    contentDescription = event.deviceTypeLabel.ifEmpty {
                        stringResource(R.string.ax_dynamic_bar_bluetooth_device)
                    },
                    modifier = Modifier.size(20.dp),
                )
            } ?: Icon(Icons.Filled.Bluetooth, null, tint = IslandTone.ACCENT.tint, modifier = Modifier.size(16.dp))
        },
        title = {
            Text(event.deviceName, color = OnCardText, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceMd),
            ) {
                StatusChip(
                    event.deviceTypeLabel.ifEmpty { stringResource(R.string.ax_dynamic_bar_connected) },
                    IslandTone.ACCENT.tint,
                )
                // The battery joins the caption line as text rather than a second chip.
                if (event.batteryLevel >= 0) {
                    Text(
                        "${event.batteryLevel}%",
                        color = if (event.batteryLevel > 20) IslandTone.POSITIVE.tint else IslandTone.ALERT.tint,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceSm),
            ) {
                PulsingDot(color = IslandTone.ACCENT.tint, size = SpaceMd)
                ActionIconButton(
                    icon = Icons.Filled.BluetoothDisabled,
                    tint = OnDestructiveText,
                    bg = DestructiveBg,
                    contentDescription = stringResource(R.string.ax_dynamic_bar_disconnect),
                    onClick = { interactor.disconnectBluetooth(event.address) },
                )
            }
        },
    )
}

@Composable
internal fun HotspotExpanded(event: IslandEvent.Hotspot) {
    ExpandedCardLayout(
        accentColor = IslandTone.ACCENT.tint,
        icon = { Icon(Icons.Filled.Wifi, null, tint = IslandTone.ACCENT.tint, modifier = Modifier.size(16.dp)) },
        title = {
            Text(stringResource(R.string.ax_dynamic_bar_hotspot_active), color = OnCardText, style = MaterialTheme.typography.titleMedium)
            StatusChip(
                when (event.numDevices) {
                    0 -> stringResource(R.string.ax_dynamic_bar_no_devices)
                    1 -> stringResource(R.string.ax_dynamic_bar_one_device_connected)
                    else -> stringResource(R.string.ax_dynamic_bar_devices_connected, event.numDevices)
                },
                IslandTone.ACCENT.tint,
            )
        },
        trailing = { PulsingDot(color = IslandTone.ACCENT.tint, size = SpaceMd) },
    )
}

@Composable
internal fun RingerModeExpanded(event: IslandEvent.RingerMode, interactor: IslandActions) {
    val style = eventStyleFor(event)
    ExpandedCardLayout(
        accentColor = style.accent,
        icon = { style.icon?.let { Icon(it, null, tint = style.accent, modifier = Modifier.size(16.dp)) } },
        title = {
            Text(stringResource(R.string.ax_dynamic_bar_sound_mode), color = OnCardText, style = MaterialTheme.typography.titleMedium)
            StatusChip(stringResource(style.labelRes), style.accent)
        },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpaceLg),
            ) {
                RingerCard(
                    isSelected = event.mode == AudioManager.RINGER_MODE_NORMAL,
                    icon = Icons.Filled.VolumeUp,
                    label = stringResource(R.string.ax_dynamic_bar_ring),
                    accent = IslandTone.ACCENT.tint,
                    onClick = { interactor.setRingerMode(AudioManager.RINGER_MODE_NORMAL) },
                    modifier = Modifier.weight(1f),
                )
                RingerCard(
                    isSelected = event.mode == AudioManager.RINGER_MODE_VIBRATE,
                    icon = Icons.Filled.Vibration,
                    label = stringResource(R.string.ax_dynamic_bar_vibrate),
                    accent = IslandTone.ATTENTION.tint,
                    onClick = { interactor.setRingerMode(AudioManager.RINGER_MODE_VIBRATE) },
                    modifier = Modifier.weight(1f),
                )
                RingerCard(
                    isSelected = event.mode == AudioManager.RINGER_MODE_SILENT,
                    icon = Icons.Filled.VolumeOff,
                    label = stringResource(R.string.ax_dynamic_bar_silent),
                    accent = IslandTone.ATTENTION.tint,
                    onClick = { interactor.setRingerMode(AudioManager.RINGER_MODE_SILENT) },
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RingerCard(
    isSelected: Boolean,
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by
        animateColorAsState(
            targetValue = if (isSelected) accent.copy(alpha = AlphaIconBg) else OnCardText.copy(alpha = AlphaFaint),
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "ringer_bg",
        )
    val tint by
        animateColorAsState(
            targetValue = if (isSelected) accent else SubtleGray,
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "ringer_tint",
        )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ShapeLg,
        color = bg,
    ) {
        Column(
            // These three cards sit inside a tile that is itself already inset, so the generous
            // vertical padding they used to carry just made the tile tall.
            modifier = Modifier.padding(vertical = SpaceMd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpaceXs),
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Text(label, color = tint, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun VpnExpanded(event: IslandEvent.Vpn) {
    val style = eventStyleFor(event)
    ExpandedCardLayout(
        accentColor = style.accent,
        icon = { style.icon?.let { Icon(it, null, tint = style.accent, modifier = Modifier.size(16.dp)) } },
        title = {
            Text(
                stringResource(style.labelRes),
                color = OnCardText,
                style = MaterialTheme.typography.titleMedium,
            )
            StatusChip(
                if (event.isValidated) stringResource(R.string.ax_dynamic_bar_secured)
                else stringResource(R.string.ax_dynamic_bar_connecting),
                if (event.isValidated) IslandTone.POSITIVE.tint else IslandTone.ATTENTION.tint,
            )
        },
        trailing = {
            PulsingDot(color = if (event.isValidated) IslandTone.POSITIVE.tint else IslandTone.ATTENTION.tint, size = SpaceMd)
        },
    )
}
