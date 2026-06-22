package com.anish.owee.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaymentConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    amount: Double? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Confirm Payment",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (amount != null) 
                        "Did you successfully complete the payment of ₹${"%.2f".format(amount)}?" 
                    else 
                        "Was the payment completed successfully?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "Yes, Marked as Settled",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Text(
                    "No, Not Yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = true
        )
    )
}
