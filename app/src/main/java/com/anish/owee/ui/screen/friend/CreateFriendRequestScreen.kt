package com.anish.owee.ui.screen.friend

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.ui.theme.*
import com.anish.owee.viewmodel.CreateFriendRequestViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFriendRequestScreen(
    friendId: String,
    friendName: String = "",
    onBack: () -> Unit = {},
    viewModel: CreateFriendRequestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasAmount = uiState.amount.isNotBlank() && uiState.amount != "0"
    var triedToSubmit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBack()
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Status Bar Loading ---
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.TopCenter).zIndex(1f)
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        // --- Scrollable Content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp), // Space for hovering button
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 20.dp),
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
                    text = "Request Money",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(Modifier.height(32.dp))

            // Recipient Section
            Text(
                text = "Requesting from",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = friendName.ifBlank { "Friend" },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(32.dp))

            // Amount Input Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (hasAmount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
                
                BasicTextField(
                    value = uiState.amount,
                    onValueChange = {
                        if (it.length <= 7) viewModel.updateAmount(it)
                    },
                    modifier = Modifier.width(IntrinsicSize.Min),
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
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
                                        fontSize = 64.sp,
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

            Spacer(Modifier.height(24.dp))

            // Note Section
            OutlinedTextField(
                value = uiState.note,
                onValueChange = {
                    viewModel.updateNote(it)
                    triedToSubmit = false
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                placeholder = {
                    Text(
                        text = "What's this for?",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            
            // Shake indicator
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(1.5.dp)
                    .offset(x = shakeOffset.value.dp)
                    .background(
                        if (triedToSubmit && uiState.note.isBlank()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
            )
        }

        // --- Action Button (Truly Hovering) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding() // Only the button moves up, keyboard background will be screen background
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Button(
                onClick = { 
                    if (uiState.note.isBlank()) {
                        triedToSubmit = true
                        scope.launch {
                            shakeOffset.animateTo(10f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh))
                            shakeOffset.animateTo(0f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh))
                        }
                    } else {
                        viewModel.createRequest(friendId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                shape = MaterialTheme.shapes.medium,
                enabled = hasAmount && !uiState.isLoading,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Text(
                        text = if (hasAmount) "Send Request" else "Enter Details",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
