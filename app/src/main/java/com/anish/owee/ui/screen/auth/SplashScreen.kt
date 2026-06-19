package com.anish.owee.ui.screen.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.owee.ui.theme.Background
import com.anish.owee.ui.theme.OnPrimary
import com.anish.owee.ui.theme.Primary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit
) {
    val scale = remember { Animatable(0.72f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Logo pops in
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(480, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(380)
        )

        delay(1100)

        // Fade out before handoff
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(280)
        )

        onNavigateNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(72.dp)
                .scale(scale.value)
                .graphicsLayer { this.alpha = alpha.value },
            shape = RoundedCornerShape(20.dp),
            color = Primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "O",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnPrimary,
                    fontSize = 34.sp
                )
            }
        }
    }
}