package com.anish.owee.ui.screen.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.owee.R
import com.anish.owee.data.model.SessionState
import com.anish.owee.ui.theme.*
import com.anish.owee.utils.GoogleAuthManager
import com.anish.owee.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    sessionViewModel: SessionViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val googleAuthManager = remember { GoogleAuthManager(context) }
    val sessionState by sessionViewModel.sessionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val blobProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .drawBehind { drawAmbientBlobs(blobProgress) }
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                // ── Branding ──────────────────────────────────────────────────
                Column(modifier = Modifier.padding(top = 100.dp)) {
                    Text(
                        text = "OWEE",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            fontSize = 42.sp
                        ),
                        color = Primary
                    )

                    Spacer(Modifier.height(48.dp))

                    Text(
                        text = "Split smart.\nLive better.",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 44.sp,
                            letterSpacing = (-1.5).sp
                        ),
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "The simplest way to track shared expenses and settle up instantly.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        lineHeight = 26.sp
                    )
                }

                // ── Sign-in CTA ───────────────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 64.dp)
                ) {
                    GoogleSignInButton(
                        isLoading = sessionState is SessionState.Loading,
                        onClick = {
                            coroutineScope.launch {
                                googleAuthManager.signIn().fold(
                                    onSuccess = { idToken ->
                                        sessionViewModel.signInWithGoogle(idToken)
                                    },
                                    onFailure = { error ->
                                        snackbarHostState.showSnackbar(
                                            message = "Login failed: ${error.localizedMessage ?: "Unknown error"}",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "By continuing, you agree to our Terms and Privacy Policy.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = if (!isLoading) onClick else ({ }),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = MaterialTheme.shapes.medium,
        color = SurfaceVariant, // Flat grey background
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Primary,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google",
                        modifier = Modifier.size(22.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Get Started with Google",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawAmbientBlobs(progress: Float) {
    val cx = size.width * 0.85f + progress * 30.dp.toPx()
    val cy = size.height * 0.15f - progress * 20.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Primary.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(cx, cy),
            radius = 300.dp.toPx()
        ),
        radius = 300.dp.toPx(),
        center = Offset(cx, cy)
    )
    val bx = size.width * 0.1f
    val by = size.height * 0.75f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Success.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(bx, by),
            radius = 240.dp.toPx()
        ),
        radius = 240.dp.toPx(),
        center = Offset(bx, by)
    )
}
