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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.shared.IslandActions
import com.android.systemui.axdynamicbar.shared.*

@Composable
internal fun NowPlayingExpanded(event: IslandEvent.NowPlaying, interactor: IslandActions) {
    val context = LocalContext.current
    val tone = IslandTone.ACCENT

    ExpandedCardLayout(
        accentColor = tone.tint,
        icon = {
            event.albumArt?.let { art ->
                Image(
                    bitmap = art.toSquareScaledBitmap(SizeIconBadge),
                    contentDescription = null,
                    modifier = Modifier.size(SizeIconBadge).clip(ShapeXs),
                    contentScale = ContentScale.Crop,
                )
            } ?: Icon(
                Icons.Filled.MusicNote,
                null,
                tint = tone.tint,
                modifier = Modifier.size(16.dp),
            )
        },
        title = {
            Text(
                event.songTitle,
                color = OnCardText,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (event.artist.isNotEmpty()) {
                Text(
                    event.artist,
                    color = OnCardSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        // The "Now playing" trailing label is gone: the tile is already titled with the track, so
        // the label said nothing the row did not. The actions take its place.
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceSm),
            ) {
                event.actions.forEach { notifAction ->
                    NotificationActionButton(
                        action = notifAction.action,
                        label = notifAction.label.toString(),
                        tone = tone,
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
