package com.anish.owee.ui.screen.friend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.owee.ui.theme.*

import com.anish.owee.ui.theme.*
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SettlementBottomSheet(
    balance: Double,
    requestedByMe: Double,
    requestedByFriend: Double,
    friendName: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDismiss: () -> Unit,
    onSettleNow: (Double) -> Unit
) {
    val isOwedByFriend = balance > 0
    val isSettled = balance == 0.0
    val absBalance = kotlin.math.abs(balance)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (rest of summary)
            Text(
                text = "Settlement Summary",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(32.dp))

            // Comparison Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettlementItem(
                    label = "You requested",
                    amount = requestedByMe.toInt(),
                    icon = Icons.Rounded.NorthEast,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .size(1.dp, 40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                SettlementItem(
                    label = "$friendName requested",
                    amount = requestedByFriend.toInt(),
                    icon = Icons.Rounded.SouthWest,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(40.dp))

            // Final Balance Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = (if (isSettled) MaterialTheme.colorScheme.surfaceVariant else if (isOwedByFriend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error).copy(alpha = 0.1f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSettled) "All Caught Up" else "Net Balance",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = (if (isSettled) MaterialTheme.colorScheme.onSurfaceVariant else if (isOwedByFriend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error).copy(alpha = 0.7f)
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = "₹${"%.2f".format(absBalance)}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        ),
                        color = if (isSettled) MaterialTheme.colorScheme.onSurface else if (isOwedByFriend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSettled) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSettled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when {
                                balance > 0 -> "$friendName owes you ₹${"%.2f".format(absBalance)}"
                                balance < 0 -> "You owe $friendName ₹${"%.2f".format(absBalance)}"
                                else -> "No pending requests"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            if (balance < -0.01) {
                Button(
                    onClick = {
                        onSettleNow(absBalance)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Settle Now",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettlementItem(
    label: String,
    amount: Int,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Text(
            text = "₹$amount",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
