package com.android.systemui.axdynamicbar.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.systemui.axdynamicbar.shared.IslandActions
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.shared.*
import com.android.systemui.res.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

private val AlbumArtSize = 80.dp
private val PlayPauseSize = 56.dp
private val ControlButtonSize = 44.dp
private val ControlIconSize = 22.dp

@Composable
internal fun MediaCard(event: IslandEvent.Media, interactor: IslandActions) {
    val colors = rememberMediaColors(event)
    val accent = colors.accent

    Box(
        modifier =
            Modifier.fillMaxWidth().clip(ShapeCard).border(1.dp, CardBorderBrush, ShapeCard)
    ) {
        AxBlurBackdrop(RadiusCard, CardBg, Modifier.matchParentSize())
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable {
                        interactor.openMediaApp()
                        interactor.collapseIsland()
                    }
                    .padding(SpaceXxl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpaceXxl),
        ) {
            event.albumArt?.let { art ->
                Image(
                    bitmap = art.toSquareScaledBitmap(AlbumArtSize),
                    contentDescription = null,
                    modifier = Modifier.size(AlbumArtSize).clip(ShapeLg),
                    contentScale = ContentScale.Crop,
                )
            }
                ?: Box(
                    modifier =
                        Modifier.size(AlbumArtSize)
                            .clip(ShapeLg)
                            .background(accent.copy(alpha = AlphaFaint)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        null,
                        tint = accent,
                        modifier = Modifier.size(36.dp),
                    )
                }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpaceXs),
            ) {
                Text(
                    event.track.ifEmpty { stringResource(R.string.ax_dynamic_bar_now_playing) },
                    color = OnCardText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
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
            }

            MediaTransportButtons(event, interactor, accent)
        }
    }
}

/**
 * Play/pause and skip-next, sitting inline with the track text instead of in a row of their own.
 * The media card carries no other transport: no shuffle, no previous, no like and no seek bar.
 */
@Composable
private fun MediaTransportButtons(
    event: IslandEvent.Media,
    interactor: IslandActions,
    accent: Color,
) {
    // Both transport buttons share one treatment — an accent wash with the accent itself as the
    // glyph — so the pair reads as a family rather than a solid disc next to a tinted one.
    // Play/pause leads, so it takes the stronger wash; the glyph stays at full accent either way,
    // which is what keeps it legible on a wash of its own colour.
    val primaryBg = accent.copy(alpha = AlphaAccent)
    val tonalBg = accent.copy(alpha = AlphaSubtle)
    var playPauseToggleCount by remember { mutableIntStateOf(0) }
    var nextToggleCount by remember { mutableIntStateOf(0) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceSm),
    ) {
        Surface(
            onClick = {
                interactor.togglePlayPause()
                playPauseToggleCount++
            },
            shape = CircleShape,
            color = primaryBg,
            modifier = Modifier.size(PlayPauseSize).squishAnimation(playPauseToggleCount),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    if (event.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (event.isPlaying)
                        stringResource(R.string.ax_dynamic_bar_pause)
                    else
                        stringResource(R.string.ax_dynamic_bar_play),
                    tint = accent,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        Surface(
            onClick = {
                interactor.skipNext()
                nextToggleCount++
            },
            shape = CircleShape,
            color = tonalBg,
            modifier = Modifier.size(ControlButtonSize).squishAnimation(nextToggleCount),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Filled.SkipNext,
                    null,
                    tint = accent,
                    modifier = Modifier.size(ControlIconSize),
                )
            }
        }
    }
}

@Composable
private fun Modifier.squishAnimation(toggleCount: Int): Modifier {
    val scaleX = remember { Animatable(1f, visibilityThreshold = 0.01f) }
    val scaleY = remember { Animatable(1f, visibilityThreshold = 0.01f) }
    val currentToggleCount by rememberUpdatedState(toggleCount)
    LaunchedEffect(Unit) {
        snapshotFlow { currentToggleCount }
            .drop(1)
            .collectLatest {
                scaleX.snapTo(1f)
                scaleY.snapTo(1f)
                coroutineScope {
                    launch {
                        scaleX.animateTo(
                            targetValue = 1f,
                            animationSpec = keyframes {
                                durationMillis = 400
                                1.066f at 120 using FastOutSlowInEasing
                                0.967f at 260
                                1f at 400
                            },
                        )
                    }
                    launch {
                        scaleY.animateTo(
                            targetValue = 1f,
                            animationSpec = keyframes {
                                durationMillis = 400
                                0.945f at 120 using FastOutSlowInEasing
                                1.033f at 260
                                1f at 400
                            },
                        )
                    }
                }
            }
    }
    return this.graphicsLayer {
        this.scaleX = scaleX.value
        this.scaleY = scaleY.value
    }
}
