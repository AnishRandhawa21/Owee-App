package com.anish.owee.ui.screen.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.viewmodel.CreateExpenseViewModel

@Composable
fun CreateExpenseScreen(
    groupId: String,
    onBack: () -> Unit = {},
    viewModel: CreateExpenseViewModel = viewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(groupId) {
        viewModel.loadMembers(groupId)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBack()
        }
    }

    if (uiState.isLoading && uiState.members.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::updateTitle,
            label = {
                Text("Expense Title")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.amount,
            onValueChange = viewModel::updateAmount,
            label = {
                Text("Amount")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Text("Participants")

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {

            items(uiState.members) { member ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Column {

                        Text(member.displayName)

                        Text("@${member.username}")
                    }

                    Checkbox(
                        checked =
                            uiState.selectedParticipantIds.contains(
                                member.id
                            ),
                        onCheckedChange = {
                            viewModel.toggleParticipant(
                                member.id
                            )
                        }
                    )
                }
            }
        }

        Button(
            onClick = {
                viewModel.createExpense(groupId)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Expense")
        }
    }
}