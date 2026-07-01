package com.anish.owee.ui.screen.friend.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.owee.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendRequestActivityCard(
    title: String,
    note: String?,
    amount: Double,
    status: String,
    createdAt: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val config = remember(status, colorScheme) {
        when (status.lowercase().trim()) {
            "paid", "settled" -> StatusConfig(
                icon = Icons.Rounded.CheckCircle,
                iconTint = colorScheme.secondary,
                iconBackground = colorScheme.secondary,
                badgeTint = colorScheme.secondary,
                badgeBackground = colorScheme.secondary,
                amountColor = colorScheme.secondary
            )
            "pending" -> StatusConfig(
                icon = Icons.Rounded.Schedule,
                iconTint = Warning,
                iconBackground = Warning,
                badgeTint = Warning,
                badgeBackground = Warning,
                amountColor = colorScheme.onSurface
            )
            "you owe", "owe" -> StatusConfig(
                icon = Icons.Rounded.CallMade,
                iconTint = colorScheme.error,
                iconBackground = colorScheme.error,
                badgeTint = colorScheme.error,
                badgeBackground = colorScheme.error,
                amountColor = colorScheme.error
            )
            "owes you", "owed" -> StatusConfig(
                icon = Icons.Rounded.CallReceived,
                iconTint = colorScheme.secondary,
                iconBackground = colorScheme.secondary,
                badgeTint = colorScheme.secondary,
                badgeBackground = colorScheme.secondary,
                amountColor = colorScheme.secondary
            )
            else -> StatusConfig(
                icon = Icons.Rounded.Schedule,
                iconTint = colorScheme.onSurfaceVariant,
                iconBackground = colorScheme.outline,
                badgeTint = colorScheme.onSurfaceVariant,
                badgeBackground = colorScheme.outline,
                amountColor = colorScheme.onSurface
            )
        }
    }

    val formattedDate = remember(createdAt) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outputFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(createdAt)
            if (date != null) outputFormat.format(date) else ""
        } catch (e: Exception) {
            ""
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Status icon pill ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = config.iconBackground.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
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
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (formattedDate.isNotBlank()) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                    )
                }
                if (!note.isNullOrBlank()) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            StatusBadge(label = status, config = config)
        }

        Spacer(Modifier.width(12.dp))

        // ── Amount ───────────────────────────────────────────────────
        Text(
            text = formatRupees(amount),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = config.amountColor
        )
    }
}

@Composable
private fun StatusBadge(label: String, config: StatusConfig) {
    Surface(
        color = config.badgeBackground.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            ),
            color = config.badgeTint
        )
    }
}

private data class StatusConfig(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color,
    val badgeTint: Color,
    val badgeBackground: Color,
    val amountColor: Color
)

private fun formatRupees(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    return "₹${formatter.format(amount)}"
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun ActivityPreview() {
    OweeTheme {
        Column(
            modifier = Modifier.background(Background)
        ) {
            FriendRequestActivityCard(
                title = "Dinner at Barbeque Nation",
                note = "Split equally between 4",
                amount = 850.0,
                status = "paid",
                createdAt = "2024-03-20T19:30:00"
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp)
            FriendRequestActivityCard(
                title = "Uber to airport",
                note = null,
                amount = 320.0,
                status = "pending",
                createdAt = "2024-03-21T08:15:00"
            )
        }
    }
}
