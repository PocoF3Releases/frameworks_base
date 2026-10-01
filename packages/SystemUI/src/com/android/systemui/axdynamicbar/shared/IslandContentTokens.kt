@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.systemui.axdynamicbar.shared

import android.app.ActivityOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.android.systemui.res.R
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlin.math.min
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.android.systemui.axdynamicbar.model.IslandEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector

internal val SpaceXxs = 2.dp
internal val SpaceXs = 4.dp
internal val SpaceSm = 6.dp
internal val SpaceMd = 8.dp
internal val SpaceLg = 12.dp
internal val SpaceXl = 14.dp
internal val SpaceXxl = 16.dp
internal val SpaceSection = 20.dp
internal val SpacePanel = 24.dp
internal val SpacePanelLarge = 28.dp

internal val RadiusXs = 8.dp
internal val RadiusSm = 12.dp
internal val RadiusLg = 24.dp
internal val RadiusXl = 32.dp
internal val RadiusCard = 28.dp

internal val ShapeXs = RoundedCornerShape(RadiusXs)
internal val ShapeSm = RoundedCornerShape(RadiusSm)
internal val ShapeLg = RoundedCornerShape(RadiusLg)
internal val ShapeXl = RoundedCornerShape(RadiusXl)
internal val ShapeCard = RoundedCornerShape(RadiusCard)

internal val SizeBadge = 14.dp
internal val SizeIconSm = 20.dp
internal val SizeIconMd = 28.dp
internal val SizeButton = 48.dp
internal val SizeButtonLg = 48.dp
internal val SizeButtonXl = 64.dp
internal val SizeAlbumSm = 52.dp
internal val SizeAlbumLg = 200.dp
internal val SizeSeekHeight = 16.dp
internal val SizeProgressHeight = 8.dp
internal val SizeStrokeWidth = 2.dp
internal val SizeStrokeThin = 1.5f
internal val SizeCompactIcon = 40.dp
internal val SizeActionHeight = 48.dp

/** The leading icon slot in an expanded tile: a small rounded badge, not a column of its own. */
internal val SizeIconBadge = 28.dp

/** A compact tile action — circular and icon-only, small enough to sit in the header row. */
internal val SizeActionIcon = 36.dp

internal const val AlphaSecondary = 0.7f
internal const val AlphaTertiary = 0.5f
internal const val AlphaHint = 0.4f
internal const val AlphaDisabled = 0.3f
internal const val AlphaSubtle = 0.15f
internal const val AlphaFaint = 0.1f
internal const val AlphaBorder = 0.08f
internal const val AlphaStatusChip = 0.14f
internal const val AlphaIconBg = 0.16f
internal const val AlphaTrack = 0.25f

/**
 * A wash strong enough to read as a filled control without going solid. One step up from
 * [AlphaSubtle] — the transport's primary button against its secondary, and any other place where
 * two accent washes have to sit side by side and one of them leads.
 */
internal const val AlphaAccent = 0.30f

internal const val AlphaGlass = 0.72f

/**
 * Micro badge — a count drawn inside a chip. The M3 label scale bottoms out at 11 sp, so the badge
 * keeps a size of its own and takes only the weight from the expressive label role.
 */
internal val TsBadge: TextStyle
    @Composable get() = MaterialTheme.typography.labelSmallEmphasized.copy(
        fontSize = 8.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )


// Semantic status roles for the keyguard battery chip. Monet-derived — no fixed hues.
internal val BatteryChargingColor: Color
    @Composable get() = MaterialTheme.colorScheme.primary
internal val BatteryPowerSaveColor: Color
    @Composable get() = MaterialTheme.colorScheme.tertiary
internal val BatteryNeutralColor: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

/**
 * Emphasis tone for an island event.
 *
 * [eventStyleFor] returns one of these instead of a fixed colour, so every hue the island draws
 * comes from the user's wallpaper-derived scheme. Four M3 accent families are available and each
 * maps onto a role:
 *
 * - [ACCENT]    event identity (most events)
 * - [POSITIVE]  success / active / done — charging, recording saved, unlocked, validated VPN
 * - [ATTENTION] needs notice — alarm ringing, vibrate, unvalidated VPN, paused capture
 * - [ALERT]     danger — low battery, hard errors
 *
 * Events sharing a tone are told apart by icon, shape and motion (see the chip tier system).
 */
internal enum class IslandTone { ACCENT, POSITIVE, ATTENTION, ALERT }

/** Saturated role: for icon/text tints and for translucent washes (`tint.copy(alpha = …)`). */
internal val IslandTone.tint: Color
    @Composable
    get() = when (this) {
        IslandTone.ACCENT -> MaterialTheme.colorScheme.primary
        IslandTone.POSITIVE -> MaterialTheme.colorScheme.secondary
        IslandTone.ATTENTION -> MaterialTheme.colorScheme.tertiary
        IslandTone.ALERT -> MaterialTheme.colorScheme.error
    }

/** Filled container role: for chips that paint the accent as their background. */
internal val IslandTone.container: Color
    @Composable
    get() = when (this) {
        IslandTone.ACCENT -> MaterialTheme.colorScheme.primaryContainer
        IslandTone.POSITIVE -> MaterialTheme.colorScheme.secondaryContainer
        IslandTone.ATTENTION -> MaterialTheme.colorScheme.tertiaryContainer
        IslandTone.ALERT -> MaterialTheme.colorScheme.errorContainer
    }

/** Content role guaranteed legible on [container]. */
internal val IslandTone.onContainer: Color
    @Composable
    get() = when (this) {
        IslandTone.ACCENT -> MaterialTheme.colorScheme.onPrimaryContainer
        IslandTone.POSITIVE -> MaterialTheme.colorScheme.onSecondaryContainer
        IslandTone.ATTENTION -> MaterialTheme.colorScheme.onTertiaryContainer
        IslandTone.ALERT -> MaterialTheme.colorScheme.onErrorContainer
    }

/** Content role legible on the saturated [tint] itself, for glyphs drawn inside a filled shape. */
internal val IslandTone.onTint: Color
    @Composable
    get() = when (this) {
        IslandTone.ACCENT -> MaterialTheme.colorScheme.onPrimary
        IslandTone.POSITIVE -> MaterialTheme.colorScheme.onSecondary
        IslandTone.ATTENTION -> MaterialTheme.colorScheme.onTertiary
        IslandTone.ALERT -> MaterialTheme.colorScheme.onError
    }

internal val ExpandedMaxWidth = 420.dp

internal val SubtleGray: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
internal val CardBg: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceBright
internal val DarkCard: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
internal val OnCardText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
internal val OnCardSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
internal val ActionBg: Color
    @Composable get() = MaterialTheme.colorScheme.primary
internal val OnActionText: Color
    @Composable get() = MaterialTheme.colorScheme.onPrimary
internal val DestructiveBg: Color
    @Composable get() = MaterialTheme.colorScheme.errorContainer
internal val OnDestructiveText: Color
    @Composable get() = MaterialTheme.colorScheme.onErrorContainer

internal val CardBorderBrush: Brush
    @Composable
    get() =
        Brush.verticalGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f),
                )
        )

/**
 * The island's type roles. Sizes come from the M3 Expressive scale that `PlatformTheme` installs,
 * so weight and tracking follow the system instead of being hand-picked per call site. The
 * `*Emphasized` roles are the expressive emphasis step: same size as their base role, heavier
 * grade — which is why swapping them in moves no layout.
 */
internal val PillPrimary: TextStyle
    @Composable get() = MaterialTheme.typography.labelSmallEmphasized.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

internal val PillAccent: TextStyle
    @Composable get() = MaterialTheme.typography.labelLargeEmphasized

/** Title over a caption, as in the keyguard media panel. */
internal val PillTitle: TextStyle
    @Composable get() = MaterialTheme.typography.titleSmallEmphasized

/** Second line under [PillTitle]; 10 sp is below the M3 label floor by design. */
internal val TsCaption: TextStyle
    @Composable get() = MaterialTheme.typography.labelSmallEmphasized.copy(
        fontSize = 10.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

/**
 * Mono variants. The family is functional, not decorative: counters tick in place, and proportional
 * digits would make the readout jitter as the numbers change. Size and weight still come from the
 * scale, so a mono counter is emphasized exactly like a proportional one.
 */
internal val PillMono: TextStyle
    @Composable get() = MaterialTheme.typography.labelSmallEmphasized.copy(
        fontFamily = FontFamily.Monospace,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

/** The ticking number in the expanded panel, a step above [PillMono] so it reads as the row's value. */
internal val TsCounter: TextStyle
    @Composable get() = MaterialTheme.typography.labelLargeEmphasized.copy(
        fontFamily = FontFamily.Monospace,
    )

/**
 * The headline value of a compact expanded tile — the timer countdown, the stopwatch, the elapsed
 * recording time. Mono for the same reason as [TsCounter]: the digits tick in place, so proportional
 * figures would make the readout jitter. One step down from the `headlineMedium` these used to take,
 * which is what lets the value share a row with its tile's icon and actions.
 */
internal val TsValue: TextStyle
    @Composable get() = MaterialTheme.typography.headlineSmall.copy(
        fontFamily = FontFamily.Monospace,
    )

internal val ShapeIconLarge = RoundedCornerShape(20.dp)
internal val ShapeIconMedium = RoundedCornerShape(16.dp)
internal val ShapeAlbum = RoundedCornerShape(16.dp)
internal val ShapeChip = RoundedCornerShape(percent = 50)

// Chip geometry per tier. Height carries urgency, corner radius carries kind — together they are
// what tells events apart now that every chip shares one wallpaper-derived accent.
internal val ChipTierPassiveSize = 28.dp
internal val ChipTierActiveHeight = 24.dp
internal val ChipTierLiveHeight = 34.dp

/**
 * Deliberately less than half of [ChipTierLiveHeight]: a live chip has to read as a squircle, not
 * as the pill that [ChipTier.ACTIVE] already owns.
 */
internal val ChipTierLiveCorner = 12.dp

internal val ChipTier.chipHeight: Dp
    get() = when (this) {
        ChipTier.PASSIVE -> ChipTierPassiveSize
        ChipTier.ACTIVE -> ChipTierActiveHeight
        ChipTier.LIVE -> ChipTierLiveHeight
    }

/** Fixed radii rather than percentages, so the corner can animate smoothly between tiers. */
internal val ChipTier.chipCornerRadius: Dp
    get() = when (this) {
        ChipTier.PASSIVE -> ChipTierPassiveSize / 2
        ChipTier.ACTIVE -> ChipTierActiveHeight / 2
        ChipTier.LIVE -> ChipTierLiveCorner
    }
internal val ShapeCompact = RoundedCornerShape(12.dp)

/** The tone an event's chip is drawn in. */
internal fun toneFor(event: IslandEvent): IslandTone = eventStyleFor(event).tone

/** How much room an event's chip takes. */
internal fun tierFor(event: IslandEvent): ChipTier = eventStyleFor(event).tier

/**
 * Saturated accent role for [event]: icon and text tints, and translucent washes via
 * `accentColorFor(event).copy(alpha = …)`. Replaces the old fixed-hue accent.
 */
@Composable internal fun accentColorFor(event: IslandEvent): Color = toneFor(event).tint

/** Filled-container role for [event], for chips that paint the accent as their background. */
@Composable internal fun chipAccentColorFor(event: IslandEvent): Color = toneFor(event).container

/** Content role guaranteed legible on [chipAccentColorFor]. */
@Composable internal fun chipContentColorFor(event: IslandEvent): Color = toneFor(event).onContainer

/**
 * Monet-backed colour set for one keyguard / expanded surface. A view of the event's [IslandTone]
 * resolved against the user's scheme — no fixed hues survive here.
 */
internal data class IslandColorScheme(
    /** The saturated fill. */
    val accent: Color,
    /** Content legible *on* [accent] — note `onTint`, not `onContainer`: [accent] is [IslandTone.tint]. */
    val onAccent: Color,
    /** A faint wash of [accent], for tinting a surface rather than filling it. */
    val tonal: Color,
)

@Composable
internal fun rememberIslandColors(event: IslandEvent): IslandColorScheme {
    val tone = toneFor(event)
    return IslandColorScheme(
        accent = tone.tint,
        // Pairs with `accent` above. `onContainer` is the content role for `container`, the pale
        // fill — using it here put a near-white glyph on a saturated fill in dark theme.
        onAccent = tone.onTint,
        tonal = tone.tint.copy(alpha = AlphaFaint),
    )
}

/** Media surfaces use the event's own tone; album-art colour extraction is gone. */
@Composable
internal fun rememberMediaColors(event: IslandEvent.Media): IslandColorScheme =
    rememberIslandColors(event)

@Composable
internal fun StatusChip(text: String, color: Color = SubtleGray) {
    Box(
        modifier =
            Modifier.background(color.copy(alpha = AlphaSubtle), ShapeChip)
                .padding(horizontal = SpaceMd, vertical = SpaceXxs)
    ) {
        Text(text.uppercase(), color = color, style = MaterialTheme.typography.labelSmallEmphasized)
    }
}

@Composable
internal fun CircleButton(
    color: Color = ActionBg,
    size: Dp = 48.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = color,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) { content() }
    }
}

/**
 * The expanded tile shell: a compact header row — icon badge, title column, optional trailing
 * control — over an optional action row.
 *
 * The icon sits in a 28 dp [ShapeCompact] badge rather than a 44 dp circle of its own. That is the
 * same badge the chip rows use, and it is what lets the header be as short as the text it carries
 * instead of being pinned to the icon's height.
 */
@Composable
internal fun ExpandedCardLayout(
    accentColor: Color,
    icon: @Composable () -> Unit,
    iconSize: Dp = SizeIconBadge,
    iconBackground: Boolean = true,
    title: @Composable ColumnScope.() -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    actions: @Composable (ColumnScope.() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SpaceSm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpaceMd),
        ) {
            if (iconBackground) {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(ShapeCompact)
                        .background(accentColor.copy(alpha = AlphaIconBg)),
                    contentAlignment = Alignment.Center,
                ) { icon() }
            } else {
                Box(
                    modifier = Modifier.size(iconSize),
                    contentAlignment = Alignment.Center,
                ) { icon() }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpaceXxs),
            ) { title() }
            trailing?.invoke()
        }
        actions?.invoke(this)
    }
}

@Composable
internal fun ActionChip(
    label: String,
    icon: ImageVector? = null,
    color: Color = OnActionText,
    bg: Color = ActionBg,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val height = if (compact) SizeActionIcon else SizeActionHeight
    val horizontal = if (compact) SpaceLg else SpacePanel
    Surface(
        onClick = onClick,
        shape = ShapeChip,
        color = bg,
        modifier = modifier.border(1.2.dp, color.copy(alpha = 0.12f), ShapeChip),
    ) {
        Row(
            modifier = Modifier.height(height).padding(horizontal = horizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpaceMd, Alignment.CenterHorizontally),
        ) {
            if (icon != null) {
                Icon(icon, null, tint = color, modifier = Modifier.size(SizeIconSm))
            }
            Text(label, color = color, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/**
 * A compact tile action: circular, icon-only, 36 dp — small enough to sit in a tile's header row
 * beside the text rather than taking a 48 dp band of its own underneath it.
 *
 * Used where the action is recognised (see [NotificationActionType.icon]). An action the classifier
 * cannot place keeps its [ActionChip] instead, so an unknown command still carries a label rather
 * than being given a glyph that means something else.
 */
@Composable
internal fun ActionIconButton(
    icon: ImageVector,
    tint: Color,
    bg: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = SizeActionIcon,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, shape = CircleShape, color = bg, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(SizeIconSm))
        }
    }
}

/**
 * Icon for a classified notification action, or null when the classifier could not place it.
 *
 * A null here is meaningful: the caller falls back to the action's own label, so the island never
 * invents a glyph for a command it does not understand.
 */
internal val NotificationActionType.icon: ImageVector?
    get() =
        when (this) {
            NotificationActionType.PAUSE -> Icons.Filled.Pause
            NotificationActionType.RESUME -> Icons.Filled.PlayArrow
            NotificationActionType.STOP -> Icons.Filled.Stop
            NotificationActionType.RESET -> Icons.Filled.Refresh
            NotificationActionType.LAP -> Icons.Filled.Flag
            NotificationActionType.ADD_MINUTE -> Icons.Filled.MoreTime
            NotificationActionType.SNOOZE -> Icons.Filled.Snooze
            NotificationActionType.DELETE -> Icons.Filled.Delete
            NotificationActionType.DISMISS -> Icons.Filled.Close
            NotificationActionType.OTHER -> null
        }

/** Background and glyph colours for one classified action. */
internal data class ActionVisuals(val bg: Color, val content: Color)

/**
 * How an action is painted, so every tile draws the same command the same way. Destructive actions
 * take the error roles, a resume takes the positive container, and everything else is a wash of the
 * event's own tone.
 */
@Composable
internal fun NotificationActionType.visuals(tone: IslandTone): ActionVisuals =
    when (this) {
        NotificationActionType.STOP,
        NotificationActionType.DELETE -> ActionVisuals(DestructiveBg, OnDestructiveText)
        NotificationActionType.RESUME ->
            ActionVisuals(IslandTone.POSITIVE.container, IslandTone.POSITIVE.onContainer)
        else -> ActionVisuals(tone.tint.copy(alpha = AlphaIconBg), tone.tint)
    }

/**
 * One action forwarded from an event's notification.
 *
 * The classifier decides how it is drawn: a command it recognises becomes an [ActionIconButton],
 * and one it cannot place keeps its label as a compact [ActionChip]. Shared so timer, stopwatch,
 * recording, notification and ongoing tiles all render the same command identically.
 */
@Composable
internal fun NotificationActionButton(
    action: Notification.Action,
    label: String,
    tone: IslandTone,
    onSend: () -> Unit,
) {
    val context = LocalContext.current
    // classify() resolves icon resources through the package manager, so it is worth not repeating
    // on every recomposition of a running timer.
    val kind =
        remember(action) {
            action.classify(context, action.actionIntent?.creatorPackage ?: context.packageName)
        }
    val icon = kind.icon
    if (icon != null) {
        val visuals = kind.visuals(tone)
        ActionIconButton(
            icon = icon,
            tint = visuals.content,
            bg = visuals.bg,
            contentDescription = label,
            onClick = onSend,
        )
    } else {
        ActionChip(label = label, compact = true, onClick = onSend)
    }
}

@Composable
internal fun ExpressivePillButton(
    label: String,
    icon: ImageVector? = null,
    contentColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        color = backgroundColor,
        modifier = modifier.border(1.2.dp, contentColor.copy(alpha = 0.12f), RoundedCornerShape(percent = 50)),
    ) {
        Row(
            modifier = Modifier.height(SizeActionHeight).padding(horizontal = SpacePanel),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpaceMd),
        ) {
            if (icon != null) {
                Icon(
                    icon, null, tint = contentColor, modifier = Modifier.size(SizeIconSm),
                )
            }
            Text(label, color = contentColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}

internal data class MediaProgress(val progress: Float, val positionMs: Long)

@Composable
internal fun rememberMediaProgress(event: IslandEvent.Media): MediaProgress {
    return MediaProgress(event.progress, event.position)
}

internal fun formatElapsedTime(ms: Long): String {
    val secs = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(secs / 60, secs % 60)
}

internal fun formatCountdownSeconds(secs: Long): String {
    val s = secs.coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

internal fun formatCountdownLong(ms: Long): String {
    val secs = (ms / 1000).coerceAtLeast(0)
    val mins = secs / 60
    val hrs = mins / 60
    return if (hrs > 0) "%02d:%02d:%02d".format(hrs, mins % 60, secs % 60)
    else "%02d:%02d".format(mins, secs % 60)
}

internal fun formatStopwatch(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val hrs = totalSecs / 3600
    val mins = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    val tenths = ((ms % 1000) / 100).coerceIn(0, 9)
    return if (hrs > 0) "%d:%02d:%02d.%d".format(hrs, mins, secs, tenths)
    else "%d:%02d.%d".format(mins, secs, tenths)
}

internal fun formatTimeAgo(timestampMs: Long, res: android.content.res.Resources): String {
    val diff = System.currentTimeMillis() - timestampMs
    if (diff < 0) return ""
    val secs = diff / 1000
    val mins = secs / 60
    val hrs = mins / 60
    val days = hrs / 24
    return when {
        secs < 60 -> res.getString(R.string.ax_dynamic_bar_just_now)
        mins < 60 -> res.getString(R.string.ax_dynamic_bar_mins_ago, mins.toInt())
        hrs < 24 -> res.getString(R.string.ax_dynamic_bar_hours_ago, hrs.toInt())
        days < 7 -> res.getString(R.string.ax_dynamic_bar_days_ago, days.toInt())
        else -> res.getString(R.string.ax_dynamic_bar_weeks_ago, (days / 7).toInt())
    }
}

@Composable
internal fun Drawable.toScaledBitmap(sizeDp: Dp): ImageBitmap {
    val px = with(LocalDensity.current) { sizeDp.roundToPx() }
    return remember(this, px) { toBitmap(px, px).asImageBitmap() }
}

/** Center-crop to a square, then scale. Use for album art, including small icon slots. */
@Composable
internal fun Drawable.toSquareScaledBitmap(sizeDp: Dp): ImageBitmap {
    val px = with(LocalDensity.current) { sizeDp.roundToPx() }
    return remember(this, px) { cropToSquareThenScale(px).asImageBitmap() }
}

private fun Drawable.cropToSquareThenScale(px: Int): Bitmap {
    val w = intrinsicWidth
    val h = intrinsicHeight
    if (w <= 0 || h <= 0) return toBitmap(px, px)
    val raw = (this as? BitmapDrawable)?.bitmap?.takeUnless { it.isRecycled } ?: toBitmap(w, h)
    val src =
        if (raw.config == Bitmap.Config.HARDWARE) {
            raw.copy(Bitmap.Config.ARGB_8888, false) ?: return toBitmap(px, px)
        } else {
            raw
        }
    val side = min(src.width, src.height)
    if (side <= 0) return toBitmap(px, px)
    val square =
        if (src.width == src.height) src
        else Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
    return if (square.width == px && square.height == px) square
    else Bitmap.createScaledBitmap(square, px, px, true)
}

internal fun chipProgressFor(event: IslandEvent, includeMediaProgress: Boolean = false): Float? =
    when (event) {
        is IslandEvent.Media -> if (includeMediaProgress) event.progress.coerceIn(0f, 1f) else null
        is IslandEvent.PromotedOngoing ->
            if (event.progress >= 0f) event.progress.coerceIn(0f, 1f) else null
        is IslandEvent.Notification ->
            if (event.progress >= 0)
                (event.progress.toFloat() / event.progressMax).coerceIn(0f, 1f)
            else null
        else -> null
    }

internal fun iconKeyFor(event: IslandEvent): Any =
    when (event) {
        is IslandEvent.Media -> event.albumArt?.hashCode() ?: "media_default"
        is IslandEvent.Notification -> event.appIcon?.hashCode() ?: "notif_default"
        is IslandEvent.AppSwitch -> {
            val app = event.previousApp ?: event.recentApps.firstOrNull()
            app?.appIcon?.hashCode() ?: "app_default"
        }
        else -> event::class.simpleName ?: "default"
    }

internal fun textKeyFor(event: IslandEvent): Any =
    when (event) {
        is IslandEvent.Media -> "${event.track}|${event.artist}"
        is IslandEvent.Timer,
        is IslandEvent.Stopwatch,
        is IslandEvent.AudioRecording -> "tick_text"
        else -> event.id
    }

internal fun resolveLabelIcon(label: String): ImageVector {
    val lower = label.lowercase()
    return when {
        lower.contains("shuffle") -> Icons.Filled.Shuffle
        lower.contains("repeat") -> Icons.Filled.Repeat
        lower.contains("thumb") && lower.contains("up") -> Icons.Filled.ThumbUp
        lower.contains("thumb") && lower.contains("down") -> Icons.Filled.ThumbDown
        lower.contains("like") || lower.contains("love") || lower.contains("favorite") -> Icons.Filled.Favorite
        else -> Icons.Filled.Shuffle
    }
}

@Composable
internal fun CustomActionIcon(
    ca: IslandEvent.MediaCustomAction,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val appBitmap = ca.icon?.let { drawable ->
        remember(drawable) {
            try { drawable.toBitmap(48, 48).asImageBitmap() } catch (_: Exception) { null }
        }
    }
    if (appBitmap != null) {
        Icon(appBitmap, ca.label, tint = tint, modifier = modifier)
    } else {
        Icon(resolveLabelIcon(ca.label), ca.label, tint = tint, modifier = modifier)
    }
}

internal fun PendingIntent.sendWithBal(context: Context, fillIntent: Intent? = null) {
    val options = ActivityOptions.makeBasic()
    options.setPendingIntentBackgroundActivityStartMode(
        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
    )
    send(context, 0, fillIntent, null, null, null, options.toBundle())
}

