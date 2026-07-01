package com.anish.owee.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anish.owee.data.model.Expense
import com.anish.owee.data.model.GroupMemberBalance
import com.anish.owee.viewmodel.ExpenseDetailViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailBottomSheet(
    expense: Expense,
    payerName: String,
    currentUserId: String,
    balances: List<GroupMemberBalance>,
    onDismiss: () -> Unit,
    viewModel: ExpenseDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val formattedDate = androidx.compose.runtime.remember(expense.createdAt) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(expense.createdAt)
            if (date != null) outputFormat.format(date) else ""
        } catch (e: Exception) {
            ""
        }
    }

    LaunchedEffect(expense.id) {
        viewModel.loadExpenseParticipants(expense.id)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header with Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = "Paid by $payerName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Overall Status for the current user
                val isPayer = expense.payerId == currentUserId
                if (isPayer) {
                    // Check if anyone still owes the current user in this group
                    val anyoneOwes = balances.any { it.amount > 0.01 }
                    StatusBadge(isPaid = !anyoneOwes)
                } else {
                    // Check if current user still owes the payer
                    val balanceWithPayer = balances.find { it.userId == expense.payerId }?.amount ?: 0.0
                    // If balanceWithPayer < -0.01, it means current user owes the payer
                    StatusBadge(isPaid = balanceWithPayer >= -0.01)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // ... rest of the content

            // Amount Card
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "₹${"%.2f".format(expense.amount)}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Split Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Participants List
            Box(modifier = Modifier.weight(1f, fill = false)) {
                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }

                LazyColumn {
                    items(uiState.participants) { participant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Participant Avatar
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (participant.user?.photoUrl != null) {
                                    AsyncImage(
                                        model = participant.user.photoUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp).align(Alignment.Center),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = participant.user?.displayName ?: "Unknown",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.weight(1f)
                            )

                            // Status for individual participant
                            val pUserId = participant.user?.id
                            if (pUserId == expense.payerId) {
                                StatusBadge(text = "PAYER", color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                            } else {
                                val isMeThePayer = expense.payerId == currentUserId
                                if (isMeThePayer && pUserId != null) {
                                    val pBalance = balances.find { it.userId == pUserId }?.amount ?: 0.0
                                    StatusBadge(isPaid = pBalance <= 0.01)
                                    Spacer(modifier = Modifier.width(12.dp))
                                } else if (pUserId == currentUserId) {
                                    val balanceWithPayer = balances.find { it.userId == expense.payerId }?.amount ?: 0.0
                                    StatusBadge(isPaid = balanceWithPayer >= -0.01)
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                            }

                            Text(
                                text = "₹${"%.2f".format(participant.shareAmount)}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Added on $formattedDate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun StatusBadge(isPaid: Boolean) {
    StatusBadge(
        text = if (isPaid) "PAID" else "UNPAID",
        color = if (isPaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
    )
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp
            ),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
