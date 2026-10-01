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

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.shared.IslandActions
import com.android.systemui.axdynamicbar.shared.*
import com.android.systemui.res.R

@Composable
internal fun NotificationExpanded(
    event: IslandEvent.Notification,
    interactor: IslandActions,
) {
    val context = LocalContext.current
    val accent = IslandTone.ACCENT.tint
    val hasProgress = event.progress >= 0 || event.isProgressIndeterminate
    val hasCustomContent =
        event.sbn.isOngoing &&
            (event.sbn.notification.bigContentView != null ||
                event.sbn.notification.contentView != null)

    Column(
        modifier = Modifier.fillMaxWidth().clickable {
            try { event.sbn.notification?.contentIntent?.sendWithBal(context) }
            catch (_: Exception) {}
            interactor.collapseIsland()
        },
        verticalArrangement = Arrangement.spacedBy(SpaceMd),
    ) {
        // Plain row rather than a tinted panel nested inside the card the tile already draws.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpaceMd),
        ) {
            NotifExpandedAvatar(event, SizeIconBadge)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpaceXxs),
            ) {
                if (event.appName.isNotEmpty()) {
                    Text(
                        event.appName,
                        color = SubtleGray,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    event.title ?: event.appName.ifEmpty { event.sbn.packageName.substringAfterLast('.') },
                    color = OnCardText,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // The trailing app icon that used to sit here duplicated the avatar on the left; the
            // actions take its place, so a one-line notification is a single row with its buttons.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceSm),
            ) {
                event.actions
                    .filter { a ->
                        val l = a.label?.toString()?.lowercase() ?: ""
                        l != "collapse" && l != "expand" && l != "minimize"
                    }
                    .take(2)
                    .forEach { notifAction ->
                        NotificationActionButton(
                            action = notifAction.action,
                            label = notifAction.label.toString(),
                            tone = IslandTone.ACCENT,
                        ) {
                            interactor.onNotificationInteraction(event.id)
                            try { notifAction.action.actionIntent?.sendWithBal(context) }
                            catch (_: Exception) {}
                            interactor.dismissEvent(event)
                            interactor.collapseIsland()
                        }
                    }

                ActionIconButton(
                    icon = Icons.Filled.Close,
                    tint = accent,
                    bg = accent.copy(alpha = AlphaIconBg),
                    contentDescription = stringResource(R.string.ax_dynamic_bar_dismiss),
                    onClick = {
                        interactor.onNotificationInteraction(event.id)
                        interactor.dismissEvent(event)
                    },
                )
            }
        }

        if (hasProgress || hasCustomContent) {
            SbnRemoteViewContent(
                event.sbn,
                fallback = {
                    if (hasProgress) {
                        NotificationProgressFallback(event)
                    } else {
                        event.text?.let {
                            Text(it, color = SubtleGray, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
            )
        } else {
            event.text?.let {
                Text(it, color = SubtleGray, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NotifExpandedAvatar(event: IslandEvent.Notification, size: Dp) {
    val icon = event.appIcon
    if (icon != null) {
        Image(
            bitmap = icon.toScaledBitmap(size),
            contentDescription = null,
            modifier = Modifier.size(size).clip(ShapeIconMedium),
        )
    } else {
        Box(
            modifier = Modifier.size(size).clip(ShapeIconMedium).background(IslandTone.ACCENT.tint.copy(alpha = AlphaSubtle)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Notifications, null, tint = IslandTone.ACCENT.tint, modifier = Modifier.size(SizeIconSm))
        }
    }
}

private fun resolveRemoteViews(ctx: Context, sbn: StatusBarNotification): RemoteViews? {

    sbn.notification.bigContentView?.let {
        return it
    }
    sbn.notification.contentView?.let {
        return it
    }

    return try {
        val builder = Notification.Builder.recoverBuilder(ctx, sbn.notification)
        builder.createBigContentView() ?: builder.createContentView()
    } catch (_: Exception) {
        null
    }
}

@Composable
internal fun SbnRemoteViewContent(sbn: StatusBarNotification, fallback: @Composable () -> Unit) {
    var remoteViewFailed by remember(sbn.key) { mutableStateOf(false) }

    if (!remoteViewFailed) {
        AndroidView(
            factory = { factoryCtx ->
                FrameLayout(factoryCtx).apply {
                    try {
                        val rv = resolveRemoteViews(factoryCtx, sbn)
                        val inflated = rv?.apply(factoryCtx, this)
                        if (inflated != null) {
                            addView(
                                inflated,
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                ),
                            )
                        } else {
                            remoteViewFailed = true
                        }
                    } catch (_: Exception) {
                        remoteViewFailed = true
                    }
                }
            },
            update = { container ->
                try {
                    container.removeAllViews()
                    val rv = resolveRemoteViews(container.context, sbn)
                    val inflated = rv?.apply(container.context, container)
                    if (inflated != null) {
                        container.addView(
                            inflated,
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                            ),
                        )
                    }
                } catch (_: Exception) {}
            },
            modifier = Modifier.fillMaxWidth().clip(ShapeIconMedium),
        )
    } else {
        fallback()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotificationProgressFallback(event: IslandEvent.Notification) {
    val fraction =
        when {
            event.isProgressIndeterminate -> null
            event.progressMax > 0 -> event.progress.toFloat() / event.progressMax
            else -> null
        }
    Column(verticalArrangement = Arrangement.spacedBy(SpaceXs)) {
        event.text?.let {
            Text(
                it,
                color = SubtleGray,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(stringResource(R.string.ax_dynamic_bar_progress), color = SubtleGray, style = MaterialTheme.typography.labelSmall)
            if (fraction != null) {
                Text("${(fraction * 100).toInt()}%", color = IslandTone.ACCENT.tint, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (fraction != null) {
            LinearWavyProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = IslandTone.ACCENT.tint,
                trackColor = IslandTone.ACCENT.tint.copy(alpha = AlphaSubtle),
            )
        } else {
            LinearWavyProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = IslandTone.ACCENT.tint,
                trackColor = IslandTone.ACCENT.tint.copy(alpha = AlphaSubtle),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NotificationGroupCard(
    notifications: List<IslandEvent.Notification>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    interactor: IslandActions,
) {
    val first = notifications.first()
    val accent = IslandTone.ACCENT.tint

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeLg)
                .background(accent.copy(alpha = AlphaFaint))
                .clickable { onToggleExpand() }
                .padding(SpaceLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpaceLg),
        ) {
            NotifExpandedAvatar(first, SizeCompactIcon)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpaceXxs),
            ) {
                Text(
                    first.appName.ifEmpty { first.sbn.packageName.substringAfterLast('.') },
                    color = OnCardText,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!isExpanded) {
                    val latest = notifications.first()
                    Text(
                        latest.text ?: latest.title ?: "",
                        color = SubtleGray,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(ShapeChip)
                    .background(accent.copy(alpha = AlphaSubtle))
                    .padding(horizontal = SpaceMd, vertical = SpaceXs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${notifications.size}",
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Icon(
                if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = stringResource(
                    if (isExpanded) R.string.ax_dynamic_bar_show_less
                    else R.string.ax_dynamic_bar_show_more,
                ),
                tint = SubtleGray,
                modifier = Modifier.size(SizeIconSm),
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SpaceMd),
                verticalArrangement = Arrangement.spacedBy(SpaceXs),
            ) {
                notifications.forEach { event ->
                    GroupedNotificationRow(
                        event = event,
                        interactor = interactor,
                        onDismiss = { interactor.dismissEvent(event) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GroupedNotificationRow(
    event: IslandEvent.Notification,
    interactor: IslandActions,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val accent = IslandTone.ACCENT.tint
    var childExpanded by remember { mutableStateOf(false) }

    MagneticSwipeToDismiss(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeSm)
                .background(DarkCard)
                .padding(SpaceLg),
            verticalArrangement = Arrangement.spacedBy(SpaceMd),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { childExpanded = !childExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceLg),
            ) {
                NotifExpandedAvatar(event, SpacePanel)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SpaceXxs),
                ) {
                    val displayName = event.title
                        ?: event.appName.ifEmpty {
                            event.sbn.packageName.substringAfterLast('.')
                        }
                    Text(
                        displayName,
                        color = OnCardText,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    event.text?.let {
                        Text(
                            it,
                            color = SubtleGray,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (childExpanded) 4 else 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Icon(
                    if (childExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = SubtleGray,
                    modifier = Modifier.size(SizeIconSm),
                )
            }

            AnimatedVisibility(
                visible = childExpanded,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpaceMd),
                ) {
                    ActionChip(
                        label = stringResource(R.string.ax_dynamic_bar_open),
                        color = accent,
                        bg = accent.copy(alpha = AlphaIconBg),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            try {
                                event.sbn.notification?.contentIntent?.sendWithBal(context)
                            } catch (_: Exception) {}
                            interactor.collapseIsland()
                        },
                    )

                    event.actions
                        .filter { a ->
                            val l = a.label?.toString()?.lowercase() ?: ""
                            l != "collapse" && l != "expand" && l != "minimize"
                        }
                        .take(2)
                        .forEach { notifAction ->
                            ActionChip(
                                label = notifAction.label.toString(),
                                color = OnActionText,
                                bg = ActionBg,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    try {
                                        notifAction.action.actionIntent?.sendWithBal(context)
                                    } catch (_: Exception) {}
                                    interactor.collapseIsland()
                                },
                            )
                        }
                }
            }

        }
    }
}
