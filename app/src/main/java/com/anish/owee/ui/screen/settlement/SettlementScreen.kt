package com.anish.owee.ui.screen.settlement

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.ui.screen.settlement.components.RecipientCard
import com.anish.owee.ui.screen.settlement.components.SettlementActionSection
import com.anish.owee.ui.screen.settlement.components.SettlementInfoCard
import com.anish.owee.utils.UpiPaymentManager
import com.anish.owee.viewmodel.SettlementViewModel

@Composable
fun SettlementScreen(
    sourceType: String,
    sourceId: String,
    userId: String,
    amount: Double,
    onBackClick: () -> Unit = {},
    onSettlementSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: SettlementViewModel = viewModel()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(uiState.value.settlementSuccess) {
        if (uiState.value.settlementSuccess) {
            onSettlementSuccess()
        }
    }

    LaunchedEffect(userId, amount, sourceType, sourceId) {
        viewModel.loadSettlementData(userId, amount, sourceType, sourceId)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.value.isPaymentInProgress) {
                viewModel.showConfirmationDialog()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.value.showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmationDialog() },
            title = { Text("Payment Status") },
            text = { Text("Was the payment completed successfully?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createSettlement()
                    viewModel.dismissConfirmationDialog()
                }) { Text("YES") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmationDialog() }) { Text("NO") }
            }
        )
    }

    if (uiState.value.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp)
        ) {
            IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text("Settlement", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
        }

        Spacer(Modifier.height(32.dp))

        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            uiState.value.user?.let { user ->
                RecipientCard(displayName = user.displayName, username = user.username, upiId = user.upiId)
            }
            Spacer(Modifier.height(24.dp))
            SettlementInfoCard(amount = uiState.value.amount, sourceType = uiState.value.sourceType)
        }

        Spacer(Modifier.weight(1f))

        Box(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            SettlementActionSection(
                amount = uiState.value.amount,
                onPayClick = {
                    val user = uiState.value.user
                    if (user?.upiId != null) {
                        UpiPaymentManager.launchUpiPayment(
                            context = context,
                            upiId = user.upiId,
                            payeeName = user.displayName,
                            amount = uiState.value.amount
                        )
                        viewModel.setPaymentInProgress(true)
                    }
                }
            )
        }
    }
}
