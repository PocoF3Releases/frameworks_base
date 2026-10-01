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

package com.android.systemui.axdynamicbar.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.shared.IslandActions
import com.android.systemui.axdynamicbar.shared.*
import com.android.systemui.res.R
import kotlinx.coroutines.delay

@Composable
internal fun TimerExpanded(event: IslandEvent.Timer, interactor: IslandActions) {
    val context = LocalContext.current
    val style = eventStyleFor(event)
    val totalMs = event.originalDurationMs.takeIf { it > 0L } ?: 1L
    var remainingMs by
        remember(event.endTimeMs) {
            mutableLongStateOf(
                if (event.endTimeMs > 0L)
                    (event.endTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
                else 0L
            )
        }
    if (!event.isPaused) {
        LaunchedEffect(event.endTimeMs) {
            if (event.endTimeMs > 0L) {
                while (remainingMs > 0L) {
                    delay(500)
                    remainingMs = (event.endTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
                }
            }
        }
    }
    val progress = if (event.endTimeMs > 0L) remainingMs.toFloat() / totalMs else 0f
    val counting = event.endTimeMs > 0L && !event.isPaused
    // Read here rather than inside the Canvas: the progress ring is drawn in a DrawScope lambda,
    // which is not composable, so the role has to be resolved before we get there.
    val accent = style.accent

    ExpandedCardLayout(
        accentColor = accent,
        // The ring is its own affordance, so it is drawn bare rather than washed onto a badge —
        // a circle inside a rounded square reads as neither.
        iconBackground = false,
        icon = {
            Box(modifier = Modifier.size(SizeIconBadge), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(SizeIconBadge)) {
                    drawArc(
                        color = accent.copy(alpha = AlphaSubtle),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = SizeStrokeWidth.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height),
                    )
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = SizeStrokeWidth.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height),
                    )
                }
                style.icon?.let { Icon(it, null, tint = style.accent, modifier = Modifier.size(14.dp)) }
            }
        },
        title = {
            // The countdown is the row's headline; the timer's name drops to the caption beneath it.
            // That inversion is what lets the value share a row with the icon and the actions.
            Text(
                when {
                    event.isPaused -> stringResource(R.string.ax_dynamic_bar_paused)
                    counting -> formatCountdownLong(remainingMs)
                    else -> stringResource(R.string.ax_dynamic_bar_running)
                },
                color = if (event.isPaused) SubtleGray else style.accent,
                style = if (counting) TsValue else MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                event.label.ifEmpty { stringResource(style.labelRes) },
                color = SubtleGray,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailing = { TimerActions(event, interactor, style, context) },
    )
}

@Composable
internal fun StopwatchExpanded(event: IslandEvent.Stopwatch, interactor: IslandActions) {
    val context = LocalContext.current
    val style = eventStyleFor(event)
    var elapsedMs by
        remember(event.startTimeMs) {
            mutableLongStateOf((System.currentTimeMillis() - event.startTimeMs).coerceAtLeast(0L))
        }
    if (event.isRunning) {
        LaunchedEffect(event.startTimeMs) {
            while (true) {
                delay(100)
                elapsedMs = (System.currentTimeMillis() - event.startTimeMs).coerceAtLeast(0L)
            }
        }
    }

    ExpandedCardLayout(
        accentColor = style.accent,
        icon = {
            if (event.isRunning) PulsingDot(color = style.accent, size = 14.dp)
            else style.icon?.let { Icon(it, null, tint = style.accent, modifier = Modifier.size(14.dp)) }
        },
        title = {
            Text(
                if (event.isRunning) formatStopwatch(elapsedMs)
                else stringResource(R.string.ax_dynamic_bar_paused),
                color = if (event.isRunning) style.accent else SubtleGray,
                style = if (event.isRunning) TsValue else MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                event.label.ifEmpty { stringResource(style.labelRes) },
                color = SubtleGray,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailing = { TimerActions(event, interactor, style, context) },
    )
}

/**
 * The tile's actions, inline in the header row instead of on a 48 dp band underneath it. Each
 * forwarded notification action is drawn by [NotificationActionButton], which falls back to a
 * labelled chip when the classifier cannot place it; with nothing to forward, the tile offers its
 * own dismiss.
 */
@Composable
private fun TimerActions(
    event: IslandEvent,
    interactor: IslandActions,
    style: EventStyle,
    context: android.content.Context,
) {
    val actions = when (event) {
        is IslandEvent.Timer -> event.actions
        is IslandEvent.Stopwatch -> event.actions
        else -> emptyList()
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceSm),
    ) {
        if (actions.isNotEmpty()) {
            actions.forEach { notifAction ->
                NotificationActionButton(
                    action = notifAction.action,
                    label = notifAction.label.toString(),
                    tone = style.tone,
                ) {
                    try {
                        notifAction.action.actionIntent?.sendWithBal(context)
                    } catch (_: Exception) {}
                }
            }
        } else {
            ActionIconButton(
                icon = Icons.Filled.Close,
                tint = style.accent,
                bg = style.accent.copy(alpha = AlphaIconBg),
                contentDescription = stringResource(R.string.ax_dynamic_bar_dismiss),
                onClick = { interactor.dismissEvent(event) },
            )
        }
    }
}
