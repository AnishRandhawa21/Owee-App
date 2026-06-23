package com.anish.owee.ui.screen.settlement

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.ui.components.PaymentConfirmationDialog
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
    val activity = context as Activity
    val viewModel: SettlementViewModel = viewModel()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadInstalledUpiApps(context)
    }

    LaunchedEffect(uiState.value.settlementSuccess) {
        if (uiState.value.settlementSuccess) {
            onSettlementSuccess()
        }
    }

    LaunchedEffect(userId, amount, sourceType, sourceId) {
        viewModel.loadSettlementData(userId, amount, sourceType, sourceId)
    }

    if (uiState.value.showTargetUpiMissingDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTargetUpiDialog() },
            title = { Text("UPI ID Missing") },
            text = { Text("${uiState.value.user?.displayName} has not set their UPI ID yet. You can remind them to add it to their profile so you can pay them easily.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissTargetUpiDialog() }) {
                    Text("OK")
                }
            }
        )
    }

    if (uiState.value.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Custom Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
            }
            Text(
                "Settlement",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        }

        Spacer(Modifier.height(24.dp))

        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            uiState.value.user?.let { user ->
                RecipientCard(
                    displayName = user.displayName,
                    username = user.username,
                    upiId = user.upiId
                )
            }
            Spacer(Modifier.height(24.dp))
            SettlementInfoCard(
                amount = uiState.value.amount,
                sourceType = uiState.value.sourceType
            )
        }

        Spacer(Modifier.weight(1f))

        // UPI App Selector
        if (uiState.value.installedUpiApps.isNotEmpty()) {
            Text(
                text = "Select UPI App",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                items(uiState.value.installedUpiApps) { app ->
                    val isSelected = uiState.value.selectedApp == app.packageName
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(64.dp)
                            .clickable { viewModel.selectPaymentApp(app.packageName) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .padding(if (isSelected) 4.dp else 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = app.icon.toBitmap().asImageBitmap(),
                                contentDescription = app.name,
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            Text(
                text = "No UPI apps found on this device",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }

        Box(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            SettlementActionSection(
                amount = uiState.value.amount,
                onPayClick = {
                    viewModel.handlePayClick(context)
                }
            )
        }
    }
}
