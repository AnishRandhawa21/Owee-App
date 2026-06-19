package com.anish.owee.ui.screen.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.ui.theme.Background
import com.anish.owee.ui.theme.OnPrimary
import com.anish.owee.ui.theme.Outline
import com.anish.owee.ui.theme.Primary
import com.anish.owee.ui.theme.PrimaryContainer
import com.anish.owee.ui.theme.TextPrimary
import com.anish.owee.ui.theme.TextSecondary
import com.anish.owee.ui.theme.OweeTheme
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .navigationBarsPadding()
            .imePadding()
    ) {

        // ── Back ──────────────────────────────────────────────────────────
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }

        // ── Content ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Recipient name
            Text(
                text = friendName.ifBlank { "Friend" },
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )

            Spacer(Modifier.height(40.dp))

            // Amount — OutlinedTextField with border stripped out so the
            // native cursor renders correctly (no double-bar issue)
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::updateAmount,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    color = TextPrimary
                ),
                placeholder = {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 48.sp,
                            color = TextSecondary.copy(alpha = 0.35f)
                        )
                    )
                },
                prefix = {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 48.sp,
                            color = if (hasAmount) TextPrimary
                            else TextSecondary.copy(alpha = 0.5f)
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Primary
                )
            )

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = Outline)
            Spacer(Modifier.height(24.dp))

            // Note
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::updateNote,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                placeholder = {
                    Text(
                        text = "Add a note",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Primary
                )
            )
        }

        // ── Pay button — pinned above keyboard ────────────────────────────
        Surface(
            onClick = { viewModel.createRequest(friendId) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            shape = MaterialTheme.shapes.medium,
            color = if (hasAmount) Primary else PrimaryContainer,
            enabled = hasAmount
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (hasAmount) "Pay ₹${uiState.amount}" else "Pay ₹0",
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp,
                    color = if (hasAmount) OnPrimary else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF9F8)
@Composable
private fun Preview() {
    OweeTheme {
        CreateFriendRequestScreen(
            friendId = "123",
            friendName = "Akshat Paul"
        )
    }
}