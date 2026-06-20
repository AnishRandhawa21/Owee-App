package com.anish.owee.ui.screen.group

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anish.owee.data.model.Expense
import com.anish.owee.data.model.GroupMemberBalance
import com.anish.owee.ui.components.ExpenseDetailBottomSheet
import com.anish.owee.ui.components.MemberBalanceBottomSheet
import com.anish.owee.ui.screen.group.components.GroupMemberStack
import com.anish.owee.viewmodel.GroupDetailViewModel
import kotlin.math.abs
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    onAddExpenseClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    onSettlementClick: (
        String,
        String,
        Double
    ) -> Unit = { _, _, _ -> },
    viewModel: GroupDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    var selectedBalanceUserId by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadGroupData(groupId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Animated Status Bar Loading
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn(tween(400)) + expandVertically(tween(400)),
            exit = fadeOut(tween(400)) + shrinkVertically(tween(400)),
            modifier = Modifier.align(Alignment.TopCenter).zIndex(1f)
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = uiState.group?.name ?: "Group Details",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                if (uiState.members.isNotEmpty()) {
                    GroupMemberStack(members = uiState.members)
                }
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { uiState.group?.id?.let { viewModel.loadGroupData(it) } },
                modifier = Modifier.fillMaxSize(),
                indicator = { } // Remove default spinner
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (uiState.group != null) {
                        // Summary Card
                        item {
                            GroupSummaryPremium(uiState.balances)
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
                            items(
                                items = uiState.balances,
                                key = { "bal_${it.userId}" }
                            ) { balance ->
                                val member = uiState.members.firstOrNull { it.id == balance.userId }
                                Box(modifier = Modifier.animateItem()) {
                                    BalanceItemFlat(
                                        memberName = member?.displayName ?: "Unknown",
                                        photoUrl = member?.photoUrl,
                                        amount = balance.amount,
                                        onClick = { selectedBalanceUserId = balance.userId }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
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
                            items(
                                items = uiState.expenses,
                                key = { it.id }
                            ) { expense ->
                                val payerName = uiState.members
                                    .firstOrNull { it.id == expense.payerId }
                                    ?.displayName ?: "Unknown"

                                Box(modifier = Modifier.animateItem()) {
                                    ExpenseItemFlat(
                                        expense = expense,
                                        payerName = payerName,
                                        onClick = { selectedExpense = expense }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Branding Footer
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, bottom = 8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "OWEE",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                        fontSize = 64.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                                Text(
                                    text = "SPLIT SMART • LIVE BETTER",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    modifier = Modifier.offset(y = (-10).dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB - Matching Groups Screen
        FloatingActionButton(
            onClick = { onAddExpenseClick(groupId) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.medium,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Expense", modifier = Modifier.size(32.dp))
        }
    }

    // Bottom Sheets
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
                settlements = uiState.settlements,
                onDismiss = { selectedBalanceUserId = null },
                onSettleClick = { memberId, amount ->

                    onSettlementClick(
                        groupId,
                        memberId,
                        amount
                    )
                }
            )
        }
    }
}

@Composable
fun GroupSummaryPremium(balances: List<GroupMemberBalance>) {
    val totalOwed = balances.filter { it.amount > 0 }.sumOf { it.amount }
    val totalOwe = balances.filter { it.amount < 0 }.sumOf { abs(it.amount) }
    val netBalance = totalOwed - totalOwe

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
        border = null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Net Balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${"%.2f".format(netBalance)}",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        ),
                        color = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryIndicator(
                    label = "You are owed",
                    amount = totalOwed,
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                SummaryIndicator(
                    label = "You owe",
                    amount = totalOwe,
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.AutoMirrored.Rounded.TrendingDown,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SummaryIndicator(
    label: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${"%.2f".format(amount)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        ),
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun EmptyStateMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
fun BalanceItemFlat(memberName: String, photoUrl: String?, amount: Double, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = memberName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = memberName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            val statusText = when {
                amount > 0 -> "gets back"
                amount < 0 -> "owes"
                else -> "settled"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val color = when {
            amount > 0 -> MaterialTheme.colorScheme.primary
            amount < 0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        
        Text(
            text = "₹${"%.2f".format(abs(amount))}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = color
        )
    }
}

@Composable
fun ExpenseItemFlat(expense: Expense, payerName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Paid by $payerName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "₹${"%.2f".format(expense.amount)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
