package com.anish.owee.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    participantsByExpense:
    Map<String, List<ExpenseParticipant>>,
    onDismiss: () -> Unit
){

    val viewModel: MemberBalanceViewModel =
        viewModel()

    val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(memberId) {

        viewModel.loadTransactions(
            currentUserId = currentUserId,
            memberId = memberId,
            expenses = expenses,
            participantsByExpense =
                participantsByExpense
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = memberName,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    if (uiState.totalAmount > 0)
                        "You are owed ₹${uiState.totalAmount}"
                    else
                        "You owe ₹${abs(uiState.totalAmount)}"
            )

            HorizontalDivider(
                modifier = Modifier.padding(
                    vertical = 16.dp
                )
            )

            LazyColumn {

                items(uiState.transactions) { transaction ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = transaction.title
                        )

                        Text(
                            text =
                                if (transaction.amount > 0)
                                    "+₹${transaction.amount}"
                                else
                                    "-₹${kotlin.math.abs(transaction.amount)}"
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(
                    vertical = 16.dp
                )
            )

            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Settle Up")
            }
        }
    }
}