package com.anish.owee.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.data.model.Expense
import com.anish.owee.viewmodel.ExpenseDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailBottomSheet(
    expense: Expense,
    payerName: String,
    onDismiss: () -> Unit,
    viewModel: ExpenseDetailViewModel = viewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(expense.id) {
        viewModel.loadExpenseParticipants(
            expense.id
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = expense.title,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "₹${expense.amount}",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Paid By $payerName",
                modifier = Modifier.padding(top = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = "Participants",
                style = MaterialTheme.typography.titleMedium
            )

            if (uiState.isLoading) {

                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )

            } else {

                LazyColumn {

                    items(uiState.participants) { participant ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Text(
                                participant.user?.displayName
                                    ?: "Unknown"
                            )

                            Text(
                                "₹${participant.shareAmount}"
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = expense.createdAt
            )
        }
    }
}