package com.anish.owee.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.data.model.Expense
import com.anish.owee.data.model.ExpenseParticipant
import com.anish.owee.viewmodel.MemberBalanceViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberBalanceBottomSheet(
    memberName: String,
    memberId: String,
    currentUserId: String,
    expenses: List<Expense>,
    participantsByExpense: Map<String, List<ExpenseParticipant>>,
    onDismiss: () -> Unit,
    onSettleClick: (
        memberId: String,
        amount: Double
    ) -> Unit
){
    val viewModel: MemberBalanceViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(memberId) {
        viewModel.loadTransactions(
            currentUserId = currentUserId,
            memberId = memberId,
            expenses = expenses,
            participantsByExpense = participantsByExpense
        )
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
            // Member Header
            Text(
                text = memberName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            )
            
            val isOwed = uiState.totalAmount > 0
            val statusText = if (isOwed) "owes you" else "you owe"
            val statusColor = if (isOwed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            
            Text(
                text = statusText.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = statusColor.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Net Balance Card
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "₹${"%.2f".format(abs(uiState.totalAmount))}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        ),
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Transactions List
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(uiState.transactions) { transaction ->
                    val transIsOwed = transaction.amount > 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Transaction Icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (transIsOwed) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (transIsOwed) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (transIsOwed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = transaction.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${if (transIsOwed) "+" else "-"}₹${"%.2f".format(abs(transaction.amount))}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (transIsOwed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isOwed) {
                // Settle Button
                Button(
                    onClick = {
                        onSettleClick(
                            memberId,
                            abs(uiState.totalAmount)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = statusColor
                    )
                ) {
                    Text(
                        text = "Settle with $memberName",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
