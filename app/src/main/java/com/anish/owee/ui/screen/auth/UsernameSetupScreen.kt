package com.anish.owee.ui.screen.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val scrollState = rememberScrollState()

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
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val textPrimaryColor = MaterialTheme.colorScheme.onBackground
        val textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
        val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
        val successColor = MaterialTheme.colorScheme.secondary
        val errorColor = MaterialTheme.colorScheme.error

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(scrollState)
        ) {

            // ── Header ────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(top = 48.dp)) {
                Text(
                    text = "Welcome to Owee",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Set up your\nprofile.",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        lineHeight = 44.sp,
                        letterSpacing = (-1.5).sp
                    ),
                    color = textPrimaryColor
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Pick a unique username so friends can find you easily.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textSecondaryColor,
                    lineHeight = 26.sp
                )
            }

            // ── Fields ────────────────────────────────────────────────────
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 32.dp)
            ) {
                OweeTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Display Name",
                    placeholder = "Your full name",
                    isFocused = nameFocused,
                    primaryColor = primaryColor,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    surfaceVariantColor = surfaceVariantColor,
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
                    primaryColor = primaryColor,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    surfaceVariantColor = surfaceVariantColor,
                    errorColor = errorColor,
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
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(successColor.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = successColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "@$username is available",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = successColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── CTA ───────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
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
                        .height(60.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = primaryColor.copy(alpha = 0.3f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Complete Setup",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OweeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isFocused: Boolean,
    primaryColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    surfaceVariantColor: Color,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    errorColor: Color = Color.Red,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isFocused) primaryColor else textSecondaryColor
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
            color = surfaceVariantColor
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
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isFocused) primaryColor else textSecondaryColor,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = textPrimaryColor,
                        fontWeight = FontWeight.SemiBold
                    ),
                    singleLine = true,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textSecondaryColor.copy(alpha = 0.4f)
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
                    style = MaterialTheme.typography.labelSmall,
                    color = errorColor,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
