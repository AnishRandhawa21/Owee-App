package com.anish.owee.ui.screen.auth

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.owee.R
import com.anish.owee.ui.theme.Background
import com.anish.owee.ui.theme.OnPrimary
import com.anish.owee.ui.theme.Outline
import com.anish.owee.ui.theme.Primary
import com.anish.owee.ui.theme.TextPrimary
import com.anish.owee.ui.theme.TextSecondary
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .drawBehind { drawAmbientBlobs(blobProgress) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ── Branding ──────────────────────────────────────────────────
            Column(modifier = Modifier.padding(top = 72.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Primary,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "O",
                            style = MaterialTheme.typography.headlineMedium,
                            color = OnPrimary,
                            fontSize = 26.sp
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Split bills,\nnot friendships.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    lineHeight = 40.sp
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Track shared expenses with anyone,\nsettle up in seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }

            // ── Sign-in CTA ───────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 52.dp)
            ) {
                GoogleSignInButton(
                    onClick = {
                        coroutineScope.launch {
                            googleAuthManager.signIn().fold(
                                onSuccess = { idToken ->
                                    sessionViewModel.signInWithGoogle(idToken)
                                },
                                onFailure = {
                                    // Error handling could be added here (e.g., Snackbar)
                                }
                            )
                        }
                    }
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "By continuing, you agree to our Terms of Service\nand Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ── Google button ─────────────────────────────────────────────────────────────

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium,
        color = androidx.compose.ui.graphics.Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "Google",
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                fontSize = 15.sp
            )
        }
    }
}

// ── Background blobs ──────────────────────────────────────────────────────────

private fun DrawScope.drawAmbientBlobs(progress: Float) {
    // Top-right blue blob drifts slowly
    val cx = size.width * 0.78f + progress * 40.dp.toPx()
    val cy = size.height * 0.16f - progress * 30.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF0055FF).copy(alpha = 0.08f), Color.Transparent),
            center = Offset(cx, cy),
            radius = 260.dp.toPx()
        ),
        radius = 260.dp.toPx(),
        center = Offset(cx, cy)
    )
    // Bottom-left green blob is static
    val bx = size.width * 0.12f
    val by = size.height * 0.65f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF4F6354).copy(alpha = 0.06f), Color.Transparent),
            center = Offset(bx, by),
            radius = 200.dp.toPx()
        ),
        radius = 200.dp.toPx(),
        center = Offset(bx, by)
    )
}
