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
import com.anish.owee.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementBottomSheet(
    balance: Double,
    requestedByMe: Double,
    requestedByFriend: Double,
    friendName: String,
    onDismiss: () -> Unit,
    onSettleNow: () -> Unit
) {
    val isOwedByFriend = balance > 0
    val isSettled = balance == 0.0
    val absBalance = kotlin.math.abs(balance).toInt()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Outline.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Settlement Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
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
                    color = Primary
                )

                Box(
                    modifier = Modifier
                        .size(1.dp, 40.dp)
                        .background(Outline.copy(alpha = 0.2f))
                )

                SettlementItem(
                    label = "$friendName requested",
                    amount = requestedByFriend.toInt(),
                    icon = Icons.Rounded.SouthWest,
                    color = Color(0xFFE57373) // Soft Red
                )
            }

            Spacer(Modifier.height(40.dp))

            // Final Balance Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = SurfaceVariant.copy(alpha = 0.5f),
                border = if (isSettled) null else MaterialTheme.shapes.extraLarge.let { 
                    null // Can add border if needed
                }
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSettled) "All Caught Up" else "Net Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = "₹$absBalance",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isSettled -> TextPrimary
                            isOwedByFriend -> Primary
                            else -> Color(0xFFE57373)
                        }
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
                            tint = if (isSettled) Primary else TextSecondary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when {
                                balance > 0 -> "$friendName owes you ₹$absBalance"
                                balance < 0 -> "You owe $friendName ₹$absBalance"
                                else -> "No pending requests"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isSettled) {
                        onDismiss()
                    } else {
                        onSettleNow()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSettled) SurfaceVariant else Primary,
                    contentColor = if (isSettled) TextPrimary else OnPrimary
                )
            ) {
                Text(
                    text = if (isSettled) "Close" else "Settle Now",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1
        )
    }
}
