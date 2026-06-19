package com.anish.owee.ui.screen.friend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anish.owee.ui.theme.ErrorContainer
import com.anish.owee.ui.theme.OweeTheme
import com.anish.owee.ui.theme.Surface
import com.anish.owee.ui.theme.SuccessContainer
import com.anish.owee.ui.theme.TextSecondary

/**
 * Incoming friend request row with Accept/Reject. Same data and the same
 * two callbacks as before (acceptFriendRequest / rejectFriendRequest by
 * friendship id) — only the presentation changed.
 */
@Composable
fun FriendRequestCard(
    senderDisplayName: String,
    senderUsername: String,
    senderPhotoUrl: String?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            FriendAvatar(
                photoUrl = senderPhotoUrl,
                displayName = senderDisplayName,
                size = 48.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = senderDisplayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@$senderUsername",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            RequestActionButton(
                icon = Icons.Rounded.Close,
                containerColor = ErrorContainer,
                contentColor = MaterialTheme.colorScheme.error,
                contentDescription = "Reject request",
                onClick = onReject
            )

            RequestActionButton(
                icon = Icons.Rounded.Check,
                containerColor = SuccessContainer,
                contentColor = com.anish.owee.ui.theme.Success,
                contentDescription = "Accept request",
                onClick = onAccept
            )
        }
    }
}

@Composable
private fun RequestActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(36.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(36.dp)
                .background(containerColor, shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendRequestCardPreview() {
    OweeTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FriendRequestCard(
                senderDisplayName = "Priya Sharma",
                senderUsername = "priyas",
                senderPhotoUrl = null,
                onAccept = {},
                onReject = {}
            )
        }
    }
}