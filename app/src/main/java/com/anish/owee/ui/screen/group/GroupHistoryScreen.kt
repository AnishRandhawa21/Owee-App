package com.anish.owee.ui.screen.group

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.data.model.Expense
import com.anish.owee.ui.components.ExpenseDetailBottomSheet
import com.anish.owee.viewmodel.GroupDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupHistoryScreen(
    groupId: String,
    onBack: () -> Unit,
    viewModel: GroupDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var expenseMenuAnchor by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(groupId) {
        viewModel.loadGroupData(groupId)
    }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            containerColor = Color.White,
            title = { Text("Delete Expense?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Are you sure you want to delete this expense? All balances in this group will be updated.") },
            confirmButton = {
                Button(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it.id, groupId) }
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Status Bar Loading ---
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).zIndex(1f)
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 8.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
                    Text(
                        text = "Expense History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = uiState.group?.name ?: "Group",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.expenses.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = uiState.expenses,
                        key = { it.id }
                    ) { expense ->
                        val payerName = uiState.members
                            .firstOrNull { it.id == expense.payerId }
                            ?.displayName ?: "Unknown"

                        Column {
                            Box {
                                ExpenseItemFlat(
                                    expense = expense,
                                    payerName = payerName,
                                    onClick = { selectedExpense = expense },
                                    onLongClick = {
                                        val currentUserId = viewModel.getCurrentUserId()
                                        if (expense.payerId == currentUserId) {
                                            expenseMenuAnchor = expense
                                        }
                                    }
                                )
                                
                                DropdownMenu(
                                    expanded = expenseMenuAnchor?.id == expense.id,
                                    onDismissRequest = { expenseMenuAnchor = null },
                                    containerColor = Color.White
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Expense", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            expenseMenuAnchor = null
                                            expenseToDelete = expense
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    selectedExpense?.let { expense ->
        val payerName = uiState.members.firstOrNull { it.id == expense.payerId }?.displayName ?: "Unknown"
        val currentUserId = viewModel.getCurrentUserId() ?: ""
        ExpenseDetailBottomSheet(
            expense = expense,
            payerName = payerName,
            currentUserId = currentUserId,
            balances = uiState.balances,
            onDismiss = { selectedExpense = null }
        )
    }
}
