package com.anish.owee.ui.screen.friend

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.ui.theme.*
import com.anish.owee.viewmodel.CreateFriendRequestViewModel

@Composable
fun CreateFriendRequestScreen(
    friendId: String,
    friendName: String = "",
    onBack: () -> Unit = {},
    viewModel: CreateFriendRequestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasAmount = uiState.amount.isNotBlank() && uiState.amount != "0"

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBack()
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .imePadding()
    ) {
        // --- Custom Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 8.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Request Money",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }

        // --- Smooth Loading Indicator ---
        Box(modifier = Modifier.fillMaxWidth().height(3.dp)) {
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary,
                    trackColor = Color.Transparent
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Recipient Section
            Text(
                text = "Requesting from",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = friendName.ifBlank { "Friend" },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Spacer(Modifier.height(48.dp))

            // Amount Input Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (hasAmount) Primary else TextSecondary.copy(alpha = 0.3f)
                    )
                )
                
                BasicTextField(
                    value = uiState.amount,
                    onValueChange = {
                        if (it.length <= 7) viewModel.updateAmount(it)
                    },
                    modifier = Modifier.width(IntrinsicSize.Min),
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Start
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    cursorBrush = SolidColor(Primary),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (uiState.amount.isEmpty()) {
                                Text(
                                    text = "0",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = 56.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextSecondary.copy(alpha = 0.3f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            // Note Section
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::updateNote,
                modifier = Modifier.fillMaxWidth(0.8f),
                placeholder = {
                    Text(
                        text = "What's this for?",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary.copy(alpha = 0.4f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    color = TextPrimary
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Primary
                )
            )
            
            // Subtle indicator for note
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(1.5.dp)
                    .background(Outline.copy(alpha = 0.3f))
            )
        }

        // --- Action Button ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(56.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            Button(
                onClick = { viewModel.createRequest(friendId) },
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = hasAmount && !uiState.isLoading,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    disabledContainerColor = Primary.copy(alpha = 0.6f),
                    disabledContentColor = OnPrimary.copy(alpha = 0.8f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp
                )
            ) {
                Text(
                    text = if (uiState.isLoading) "Sending..." else if (hasAmount) "Send Request" else "Enter Amount",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}


