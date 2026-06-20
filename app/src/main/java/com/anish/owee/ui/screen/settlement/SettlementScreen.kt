package com.anish.owee.ui.screen.settlement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.ui.screen.settlement.components.RecipientCard
import com.anish.owee.ui.screen.settlement.components.SettlementActionSection
import com.anish.owee.ui.screen.settlement.components.SettlementInfoCard
import com.anish.owee.viewmodel.SettlementViewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anish.owee.utils.UpiPaymentManager

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
        viewModel.loadSettlementData(
            userId = userId,
            amount = amount,
            sourceType = sourceType,
            sourceId = sourceId
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.value.isPaymentInProgress) {
                viewModel.showConfirmationDialog()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.value.error != null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = uiState.value.error!!, color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val user = uiState.value.user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 8.dp, end = 20.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Settlement",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            if (user != null) {
                RecipientCard(
                    displayName = user.displayName,
                    username = user.username,
                    upiId = user.upiId
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettlementInfoCard(
                amount = uiState.value.amount,
                sourceType = uiState.value.sourceType
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            SettlementActionSection(
                amount = uiState.value.amount,
                onPayClick = {
                    val currentUser = uiState.value.user ?: return@SettlementActionSection
                    val upiId = currentUser.upiId ?: return@SettlementActionSection
                    val uri = UpiPaymentManager.buildUpiUri(
                        upiId = upiId,
                        payeeName = currentUser.displayName,
                        amount = uiState.value.amount
                    )
                    val intent = UpiPaymentManager.createIntent(uri)
                    val chooser = android.content.Intent.createChooser(intent, "Pay with")
                    viewModel.setPaymentInProgress(true)
                    context.startActivity(chooser)
                }
            )
        }
    }
}