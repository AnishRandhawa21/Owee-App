package com.anish.owee.ui.screen.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.viewmodel.GroupDetailViewModel
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.anish.owee.data.model.Expense
import com.anish.owee.ui.components.ExpenseDetailBottomSheet

@Composable
fun GroupDetailScreen(
    groupId: String,
    onAddExpenseClick: (String) -> Unit = {},
    viewModel: GroupDetailViewModel = viewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedExpense by remember {
        mutableStateOf<Expense?>(null)
    }

    LaunchedEffect(groupId) {
        viewModel.loadGroupData(groupId)
    }
    selectedExpense?.let { expense ->

        val payerName =
            uiState.members
                .firstOrNull {
                    it.id == expense.payerId
                }
                ?.displayName
                ?: "Unknown"

        ExpenseDetailBottomSheet(
            expense = expense,
            payerName = payerName,
            onDismiss = {
                selectedExpense = null
            }
        )
    }

    when {

        uiState.isLoading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

        else -> {

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {

                item {

                    Text(
                        text = "Members",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                items(uiState.members) { member ->

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = member.displayName
                        )

                        Text(
                            text = "@${member.username}"
                        )
                    }

                    HorizontalDivider()
                }

                item {

                    Text(
                        text = "Expenses",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                items(uiState.expenses) { expense ->
                    val payerName =
                        uiState.members
                            .firstOrNull {
                                it.id == expense.payerId
                            }
                            ?.displayName
                            ?: "Unknown"


                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable {
                                selectedExpense = expense
                            }
                    ) {

                        Text(
                            text = expense.title,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "₹${expense.amount}"
                        )

                        Text(
                            text = "Paid By $payerName"
                        )
                    }

                    HorizontalDivider()


                }

                item {

                    HorizontalDivider()

                    androidx.compose.material3.Button(
                        onClick = {
                            onAddExpenseClick(groupId)
                        },
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text("Add Expense")
                    }
                }
            }
        }
    }
}