package com.anish.owee.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anish.owee.ui.theme.Error
import com.anish.owee.viewmodel.SessionViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.viewmodel.ProfileViewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
@Composable
fun ProfileScreen(
    sessionViewModel: SessionViewModel
) {

    val profileViewModel: ProfileViewModel =
        viewModel()

    val uiState by profileViewModel
        .uiState
        .collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        AsyncImage(
            model = uiState.user?.photoUrl,
            contentDescription = "Profile Photo",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )
        Text(
            text = uiState.user?.displayName ?: "Loading...",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(
                modifier = Modifier.height(8.dp)
                )

        Text(
            text = "@${uiState.user?.username.orEmpty()}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = uiState.user?.email.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        OutlinedTextField(
            value = uiState.upiId,
            onValueChange = {
                profileViewModel.updateUpiId(it)
            },
            label = {
                Text("UPI ID")
            },
            placeholder = {
                Text("anish@oksbi")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            onClick = {
                profileViewModel.saveUpiId()
            },
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {

            if (uiState.isSaving) {

                CircularProgressIndicator()

            } else {

                Text(
                    text = "Save UPI ID"
                )
            }
        }

        if (uiState.saveSuccess) {

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "UPI ID saved successfully",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { sessionViewModel.logout() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Logout")
        }
    }
}
