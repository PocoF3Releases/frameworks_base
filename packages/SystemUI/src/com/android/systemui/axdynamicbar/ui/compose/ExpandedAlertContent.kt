package com.android.systemui.axdynamicbar.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.shared.IslandActions
import com.android.systemui.axdynamicbar.shared.*
import com.android.systemui.res.R
import android.text.format.DateFormat
import java.util.Date

@Composable
internal fun AlarmExpanded(event: IslandEvent.Alarm, interactor: IslandActions) {
    ExpandedCardLayout(
        accentColor = IslandTone.ATTENTION.tint,
        icon = {
            if (event.isRinging) PulsingDot(color = IslandTone.ATTENTION.tint, size = 16.dp)
            else Icon(Icons.Filled.Alarm, null, tint = IslandTone.ATTENTION.tint, modifier = Modifier.size(16.dp))
        },
        title = {
            Text(event.label.ifEmpty { stringResource(R.string.ax_dynamic_bar_alarm) }, color = OnCardText, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // The ringing chip and the trigger time share the caption line rather than taking a
            // line and a trailing column of their own.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpaceMd),
            ) {
                if (event.isRinging) {
                    StatusChip(stringResource(R.string.ax_dynamic_bar_ringing), IslandTone.ATTENTION.tint)
                }
                if (event.triggerTimeMs > 0L) {
                    Text(
                        DateFormat.getTimeFormat(LocalContext.current)
                            .format(Date(event.triggerTimeMs)),
                        color = SubtleGray,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        trailing = {
            ActionIconButton(
                icon = Icons.Filled.Close,
                tint = IslandTone.ATTENTION.tint,
                bg = IslandTone.ATTENTION.tint.copy(alpha = AlphaIconBg),
                contentDescription = stringResource(R.string.ax_dynamic_bar_dismiss),
                onClick = { interactor.dismissEvent(event) },
            )
        },
    )
}
