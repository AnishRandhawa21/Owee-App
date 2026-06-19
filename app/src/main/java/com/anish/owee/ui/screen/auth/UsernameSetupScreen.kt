package com.anish.owee.ui.screen.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.ui.theme.Background
import com.anish.owee.ui.theme.Error
import com.anish.owee.ui.theme.OnPrimary
import com.anish.owee.ui.theme.OnPrimaryContainer
import com.anish.owee.ui.theme.Outline
import com.anish.owee.ui.theme.Primary
import com.anish.owee.ui.theme.PrimaryContainer
import com.anish.owee.ui.theme.Success
import com.anish.owee.ui.theme.SuccessContainer
import com.anish.owee.ui.theme.TextPrimary
import com.anish.owee.ui.theme.TextSecondary
import com.anish.owee.viewmodel.SessionViewModel

@Composable
fun UsernameSetupScreen(
    sessionViewModel: SessionViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var nameFocused by remember { mutableStateOf(false) }
    var usernameFocused by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val nameFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val isFormValid = name.trim().isNotEmpty()
            && username.trim().length >= 3
            && usernameError == null

    LaunchedEffect(Unit) {
        sessionViewModel.getGoogleFullName()?.let {
            name = it
        }
    }

    LaunchedEffect(username) {
        if (username.isNotEmpty()) {
            usernameError = when {
                username.length < 3 -> "At least 3 characters"
                !username.matches(Regex("^[a-z0-9._]+$")) -> "Only lowercase letters, numbers, . and _"
                else -> null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ── Header ────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(top = 72.dp)) {
                StepIndicator(activeIndex = 1, total = 2)

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Set up your\nprofile.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    lineHeight = 40.sp
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "This is how others will find and\nrecognise you on Owee.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }

            // ── Fields ────────────────────────────────────────────────────
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                OweeTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Your name",
                    placeholder = "Name",
                    isFocused = nameFocused,
                    modifier = Modifier
                        .focusRequester(nameFocusRequester)
                        .onFocusChanged { nameFocused = it.isFocused },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { usernameFocusRequester.requestFocus() }
                    )
                )

                OweeTextField(
                    value = username,
                    onValueChange = { username = it.lowercase().replace(" ", "") },
                    label = "Username",
                    placeholder = "username",
                    isFocused = usernameFocused,
                    prefix = "@",
                    errorMessage = usernameError,
                    modifier = Modifier
                        .focusRequester(usernameFocusRequester)
                        .onFocusChanged { usernameFocused = it.isFocused },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isFormValid) {
                                focusManager.clearFocus()
                                isSaving = true
                                sessionViewModel.completeUsernameSetup(name.trim(), username.trim()) { result ->
                                    isSaving = false
                                    result.onFailure { error ->
                                        usernameError = error.message
                                    }
                                }
                            }
                        }
                    )
                )

                // Inline success hint
                AnimatedVisibility(
                    visible = username.length >= 3 && usernameError == null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SuccessContainer,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Success,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            text = "@$username looks good",
                            style = MaterialTheme.typography.bodySmall,
                            color = Success
                        )
                    }
                }
            }

            // ── CTA ───────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(bottom = 52.dp)) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        isSaving = true
                        sessionViewModel.completeUsernameSetup(name.trim(), username.trim()) { result ->
                            isSaving = false
                            result.onFailure { error ->
                                usernameError = error.message
                            }
                        }
                    },
                    enabled = isFormValid && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = OnPrimary,
                        disabledContainerColor = PrimaryContainer,
                        disabledContentColor = OnPrimaryContainer.copy(alpha = 0.45f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = OnPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Step indicator ────────────────────────────────────────────────────────────

@Composable
private fun StepIndicator(activeIndex: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            StepDot(active = index == activeIndex)
        }
    }
}

@Composable
private fun StepDot(active: Boolean) {
    val width by animateDpAsState(
        targetValue = if (active) 20.dp else 6.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dot_width"
    )
    Box(
        modifier = Modifier
            .height(6.dp)
            .width(width)
            .clip(RoundedCornerShape(50))
            .background(if (active) Primary else Outline)
    )
}

// ── Text field ────────────────────────────────────────────────────────────────

@Composable
private fun OweeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            errorMessage != null -> Error
            isFocused -> Primary
            else -> Outline
        },
        animationSpec = tween(200),
        label = "border_color"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isFocused) Primary else TextSecondary
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(1.5.dp, borderColor, MaterialTheme.shapes.small),
            shape = MaterialTheme.shapes.small,
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (prefix != null) {
                    Text(
                        text = prefix,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    singleLine = true,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary.copy(alpha = 0.5f)
                                )
                            }
                            inner()
                        }
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Error
                )
            }
        }
    }
}
