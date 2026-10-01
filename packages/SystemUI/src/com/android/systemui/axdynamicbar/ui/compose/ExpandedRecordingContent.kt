package com.android.systemui.axdynamicbar.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.systemui.axdynamicbar.shared.IslandActions
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.model.RecordingState
import com.android.systemui.res.R
import androidx.compose.ui.platform.LocalContext
import com.android.systemui.axdynamicbar.shared.*
import kotlinx.coroutines.delay

@Composable
internal fun AudioRecordingExpanded(
    event: IslandEvent.AudioRecording,
    interactor: IslandActions,
) {
    val context = LocalContext.current
    val style = eventStyleFor(event)
    val isSaved = event.state == RecordingState.SAVED
    val stateWord =
        stringResource(
            when (event.state) {
                RecordingState.RECORDING -> R.string.ax_dynamic_bar_recording
                RecordingState.PAUSED -> R.string.ax_dynamic_bar_paused
                RecordingState.SAVED -> R.string.ax_dynamic_bar_saved
            }
        )

    var elapsedMs by remember(event.startTimeMs, event.pausedDurationMs) {
        mutableLongStateOf(
            (System.currentTimeMillis() - event.startTimeMs - event.pausedDurationMs).coerceAtLeast(0L)
        )
    }
    LaunchedEffect(event.startTimeMs, event.state, event.pausedDurationMs) {
        if (event.state == RecordingState.RECORDING) {
            while (true) {
                delay(1000)
                elapsedMs =
                    (System.currentTimeMillis() - event.startTimeMs - event.pausedDurationMs)
                        .coerceAtLeast(0L)
            }
        }
    }

    ExpandedCardLayout(
        accentColor = style.accent,
        icon = {
            when (event.state) {
                RecordingState.RECORDING -> PulsingDot(color = style.accent, size = 12.dp, durationMs = 550, minAlpha = AlphaTrack)
                else -> style.icon?.let { Icon(it, null, tint = style.accent, modifier = Modifier.size(16.dp)) }
            }
        },
        title = {
            // The elapsed time is the headline; the state and the app collapse into one caption
            // line, replacing the separate recording chip that used to sit in the trailing column.
            Text(
                if (isSaved) stringResource(style.labelRes) else formatElapsedTime(elapsedMs),
                color = style.accent,
                style = if (isSaved) MaterialTheme.typography.titleSmall else TsValue,
                maxLines = 1,
            )
            Text(
                if (event.appName.isEmpty()) stateWord else "$stateWord · ${event.appName}",
                color = SubtleGray,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceSm),
            ) {
                event.actions.forEach { notifAction ->
                    NotificationActionButton(
                        action = notifAction.action,
                        label = notifAction.label.toString(),
                        tone = style.tone,
                    ) {
                        try {
                            notifAction.action.actionIntent?.sendWithBal(context)
                        } catch (_: Exception) {}
                        interactor.collapseIsland()
                    }
                }
            }
        },
    )
}
