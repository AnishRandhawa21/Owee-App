package com.anish.owee.ui.screen.profile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anish.owee.ui.theme.*
import com.anish.owee.viewmodel.ProfileViewModel
import com.anish.owee.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    sessionViewModel: SessionViewModel
) {
    val profileViewModel: ProfileViewModel = viewModel()
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Status Bar Loading ---
        AnimatedVisibility(
            visible = uiState.isLoading || uiState.isSaving,
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
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Large Flat Title
            Text(
                text = "Profile",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp,
                    fontSize = 34.sp
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Flat Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (uiState.user?.photoUrl != null) {
                        AsyncImage(
                            model = uiState.user?.photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = uiState.user?.displayName?.take(1)?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = uiState.user?.displayName ?: "Loading...",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "@${uiState.user?.username.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Flat Menu Items
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileSectionHeader("ACCOUNT")
                
                ProfileRow(
                    icon = Icons.Rounded.Email,
                    label = "Email",
                    value = uiState.user?.email.orEmpty()
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                ProfileSectionHeader("PAYMENT SETTINGS")
                
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    OutlinedTextField(
                        value = uiState.upiId,
                        onValueChange = { profileViewModel.updateUpiId(it) },
                        label = { Text("Your UPI ID") },
                        placeholder = { Text("username@upi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { profileViewModel.saveUpiId() },
                        enabled = !uiState.isSaving && uiState.upiId != (uiState.user?.upiId ?: ""),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.medium,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(if (uiState.isSaving) "Saving..." else "Update UPI ID", fontWeight = FontWeight.Bold)
                    }
                    
                    if (uiState.saveSuccess) {
                        Text(
                            text = "Saved successfully!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Success,
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Logout Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Logout from Owee",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { sessionViewModel.logout() }) {
                        Text("CONFIRM", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                    }
                }

                // Branding Footer (Flat)
                Spacer(modifier = Modifier.height(64.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "OWEE",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 64.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                    Text(
                        text = "SPLIT SMART • LIVE BETTER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        modifier = Modifier.offset(y = (-10).dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
    )
}

@Composable
fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
