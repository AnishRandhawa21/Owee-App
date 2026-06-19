package com.anish.owee.ui.screen.friend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anish.owee.ui.theme.Error
import com.anish.owee.ui.theme.ErrorContainer
import com.anish.owee.ui.theme.Outline
import com.anish.owee.ui.theme.OutlineVariant
import com.anish.owee.ui.theme.Success
import com.anish.owee.ui.theme.SuccessContainer
import com.anish.owee.ui.theme.TextPrimary
import com.anish.owee.ui.theme.TextSecondary
import com.anish.owee.ui.theme.Warning
import com.anish.owee.ui.theme.OweeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FriendRequestActivityCard(
    title: String,
    note: String?,
    amount: Double,
    status: String,
    modifier: Modifier = Modifier
) {
    val config = statusConfig(status)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ── Status icon pill ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = config.iconBackground,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = status,
                    tint = config.iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            // ── Title + note ─────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!note.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                // Status badge inline under the title
                StatusBadge(label = status, config = config)
            }

            Spacer(Modifier.width(12.dp))

            // ── Amount ───────────────────────────────────────────────────
            Text(
                text = formatRupees(amount),
                style = MaterialTheme.typography.titleMedium,
                color = config.amountColor
            )
        }
    }
}

// ── Status badge ──────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(label: String, config: StatusConfig) {
    Box(
        modifier = Modifier
            .background(
                color = config.badgeBackground,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = config.badgeTint
        )
    }
}

// ── Status config ─────────────────────────────────────────────────────────────

private data class StatusConfig(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color,
    val badgeTint: Color,
    val badgeBackground: Color,
    val amountColor: Color
)

private fun statusConfig(status: String): StatusConfig {
    return when (status.lowercase().trim()) {

        "paid", "settled" -> StatusConfig(
            icon = Icons.Rounded.CheckCircle,
            iconTint = Success,
            iconBackground = SuccessContainer,
            badgeTint = Success,
            badgeBackground = SuccessContainer,
            amountColor = Success
        )

        "pending" -> StatusConfig(
            icon = Icons.Rounded.Schedule,
            iconTint = Warning,
            iconBackground = Warning.copy(alpha = 0.12f),
            badgeTint = Warning,
            badgeBackground = Warning.copy(alpha = 0.12f),
            amountColor = TextPrimary
        )

        "you owe", "owe" -> StatusConfig(
            icon = Icons.Rounded.CallMade,
            iconTint = Error,
            iconBackground = ErrorContainer,
            badgeTint = Error,
            badgeBackground = ErrorContainer,
            amountColor = Error
        )

        "owes you", "owed" -> StatusConfig(
            icon = Icons.Rounded.CallReceived,
            iconTint = Success,
            iconBackground = SuccessContainer,
            badgeTint = Success,
            badgeBackground = SuccessContainer,
            amountColor = Success
        )

        // Fallback for any unknown status string
        else -> StatusConfig(
            icon = Icons.Rounded.Schedule,
            iconTint = TextSecondary,
            iconBackground = OutlineVariant,
            badgeTint = TextSecondary,
            badgeBackground = OutlineVariant,
            amountColor = TextPrimary
        )
    }
}

// ── Amount formatter ──────────────────────────────────────────────────────────

private fun formatRupees(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    return "₹${formatter.format(amount)}"
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun PaidPreview() {
    OweeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FriendRequestActivityCard(
                title = "Dinner at Barbeque Nation",
                note = "Split equally between 4",
                amount = 850.0,
                status = "paid"
            )
            FriendRequestActivityCard(
                title = "Uber to airport",
                note = null,
                amount = 320.0,
                status = "pending"
            )
            FriendRequestActivityCard(
                title = "Hotel Taj booking",
                note = "Rahul paid",
                amount = 4200.0,
                status = "you owe"
            )
            FriendRequestActivityCard(
                title = "Movie tickets",
                note = "PVR IMAX",
                amount = 600.0,
                status = "owes you"
            )
        }
    }
}