package com.anish.owee.ui.screen.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anish.owee.data.model.Expense
import com.anish.owee.data.model.GroupMemberBalance
import com.anish.owee.ui.components.ExpenseDetailBottomSheet
import com.anish.owee.ui.components.MemberBalanceBottomSheet
import com.anish.owee.viewmodel.GroupDetailViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    onAddExpenseClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: GroupDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var selectedBalanceUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(groupId) {
        viewModel.loadGroupData(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.group?.name ?: "Group Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddExpenseClick(groupId) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (uiState.isLoading && uiState.group == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Summary Section
                    item {
                        GroupSummarySection(uiState.balances)
                    }

                    // Balances Section
                    item {
                        SectionHeader("Group Balances")
                    }

                    if (uiState.balances.isEmpty()) {
                        item {
                            EmptyStateMessage("No balances yet. Everyone is settled up!")
                        }
                    } else {
                        items(uiState.balances) { balance ->
                            val member = uiState.members.firstOrNull { it.id == balance.userId }
                            BalanceItem(
                                memberName = member?.displayName ?: "Unknown",
                                photoUrl = member?.photoUrl,
                                amount = balance.amount,
                                onClick = { selectedBalanceUserId = balance.userId }
                            )
                        }
                    }

                    // Expenses Section
                    item {
                        SectionHeader("Recent Expenses")
                    }

                    if (uiState.expenses.isEmpty()) {
                        item {
                            EmptyStateMessage("No expenses added yet.")
                        }
                    } else {
                        items(uiState.expenses) { expense ->
                            val payerName = uiState.members
                                .firstOrNull { it.id == expense.payerId }
                                ?.displayName ?: "Unknown"

                            ExpenseItem(
                                expense = expense,
                                payerName = payerName,
                                onClick = { selectedExpense = expense }
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheets
    selectedExpense?.let { expense ->
        val payerName = uiState.members.firstOrNull { it.id == expense.payerId }?.displayName ?: "Unknown"
        ExpenseDetailBottomSheet(
            expense = expense,
            payerName = payerName,
            onDismiss = { selectedExpense = null }
        )
    }

    selectedBalanceUserId?.let { memberId ->
        val member = uiState.members.firstOrNull { it.id == memberId }
        val currentUserId = viewModel.getCurrentUserId()
        if (member != null && currentUserId != null) {
            MemberBalanceBottomSheet(
                memberName = member.displayName,
                memberId = member.id,
                currentUserId = currentUserId,
                expenses = uiState.expenses,
                participantsByExpense = uiState.participantsByExpense,
                onDismiss = { selectedBalanceUserId = null }
            )
        }
    }
}

@Composable
fun GroupSummarySection(balances: List<GroupMemberBalance>) {
    val totalOwed = balances.filter { it.amount > 0 }.sumOf { it.amount }
    val totalOwe = balances.filter { it.amount < 0 }.sumOf { abs(it.amount) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val netBalance = totalOwed - totalOwe
                    Text(
                        text = "₹${"%.2f".format(netBalance)}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (netBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "You are owed: ₹${"%.2f".format(totalOwed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "You owe: ₹${"%.2f".format(totalOwe)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun EmptyStateMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun BalanceItem(memberName: String, photoUrl: String?, amount: Double, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = memberName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = memberName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                val text = when {
                    amount > 0 -> "gets back"
                    amount < 0 -> "owes"
                    else -> "settled"
                }
                val color = when {
                    amount > 0 -> Color(0xFF2E7D32)
                    amount < 0 -> Color(0xFFC62828)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
                Text(
                    text = "₹${"%.2f".format(abs(amount))}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, payerName: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "Paid by $payerName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "₹${"%.2f".format(expense.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
