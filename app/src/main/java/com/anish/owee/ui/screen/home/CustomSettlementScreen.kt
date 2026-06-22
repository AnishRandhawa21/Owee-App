package com.anish.owee.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anish.owee.utils.UpiPaymentManager
import com.anish.owee.viewmodel.CustomSettlementViewModel
import com.anish.owee.ui.theme.*
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSettlementScreen(
    userId: String,
    onBack: () -> Unit = {},
    viewModel: CustomSettlementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(userId) {
        viewModel.loadUserDebts(userId)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBack()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.isPaymentInProgress) {
                viewModel.showConfirmationDialog()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (uiState.showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmationDialog() },
            title = { Text("Payment Status") },
            text = { Text("Was the payment completed successfully?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createSettlements()
                    viewModel.dismissConfirmationDialog()
                }) { Text("YES") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmationDialog() }) { Text("NO") }
            }
        )
    }

    val isOwedByThem = uiState.totalDebt > 0.01
    val amountToPay = uiState.amount.toDoubleOrNull() ?: 0.0
    val totalDebtAbs = abs(uiState.totalDebt)
    val hasAmount = amountToPay > 0.01
    val exceedsDebt = !isOwedByThem && amountToPay > totalDebtAbs + 0.01

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Status Bar Loading ---
        AnimatedVisibility(
            visible = uiState.isLoading,
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
                .statusBarsPadding()
        ) {
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
                Text(
                    text = "Settle Balance",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))

                // User Profile
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (uiState.targetUser?.photoUrl != null) {
                        AsyncImage(
                            model = uiState.targetUser?.photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = uiState.targetUser?.displayName?.take(1)?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (isOwedByThem) "${uiState.targetUser?.displayName} owes you" else "You owe ${uiState.targetUser?.displayName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", abs(uiState.totalDebt))}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isOwedByThem) Success else Error
                    )
                )

                Spacer(Modifier.height(64.dp))

                // Amount Input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 56.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (hasAmount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                    
                    BasicTextField(
                        value = uiState.amount,
                        onValueChange = { viewModel.updateAmount(it) },
                        modifier = Modifier.width(IntrinsicSize.Min),
                        textStyle = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 56.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (exceedsDebt) Error else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (uiState.amount.isEmpty()) {
                                    Text(
                                        text = "0",
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontSize = 56.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                AnimatedVisibility(visible = exceedsDebt) {
                    Text(
                        text = "Cannot exceed total debt",
                        style = MaterialTheme.typography.labelMedium,
                        color = Error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Quick Selection Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAmountButton(
                        label = "HALF",
                        onClick = { viewModel.setAmountRatio(0.5) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickAmountButton(
                        label = "FULL",
                        onClick = { viewModel.setAmountRatio(1.0) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.weight(1f))

                // Bottom Action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                ) {
                    Button(
                        onClick = {
                            if (isOwedByThem) {
                                // Remind Logic (Temporary clipboard copy)
                                android.util.Log.d("OWEE", "Reminding user...")
                            } else {
                                val upiId = uiState.targetUser?.upiId
                                if (upiId != null) {
                                    UpiPaymentManager.launchUpiPayment(
                                        context = context,
                                        upiId = upiId,
                                        payeeName = uiState.targetUser?.displayName ?: "User",
                                        amount = amountToPay
                                    )
                                    viewModel.setPaymentInProgress(true)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = MaterialTheme.shapes.medium,
                        enabled = (isOwedByThem || (hasAmount && !exceedsDebt)) && !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOwedByThem) MaterialTheme.colorScheme.secondary else if (exceedsDebt) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOwedByThem) Icons.Rounded.NotificationsActive else Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (isOwedByThem) "Remind ${uiState.targetUser?.displayName?.split(" ")?.first()}" else "Pay ₹${String.format(Locale.US, "%.2f", amountToPay)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAmountButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
