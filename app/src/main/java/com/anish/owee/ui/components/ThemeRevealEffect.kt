package com.anish.owee.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.anish.owee.ui.theme.ThemeMode

@Composable
fun ThemeRevealEffect(
    themeMode: ThemeMode,
    targetColor: Color,
    clickOffset: Offset?,
    onAnimationFinished: () -> Unit
) {
    val radius = remember { Animatable(0f) }
    
    // Capture the color and center accurately for the current animation
    val revealColor = remember(targetColor) { targetColor }
    val revealCenter = remember(clickOffset) { clickOffset ?: Offset.Zero }

    LaunchedEffect(themeMode, clickOffset) {
        if (clickOffset != null) {
            // Start from 0 for every new click
            radius.snapTo(0f)
            radius.animateTo(
                targetValue = 4000f, // Ensure it covers all screen sizes/orientations
                animationSpec = tween(
                    durationMillis = 1400,
                    easing = FastOutSlowInEasing
                )
            )
            onAnimationFinished()
        }
    }

    if (clickOffset != null) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (radius.value > 0f) {
                drawCircle(
                    color = revealColor,
                    radius = radius.value,
                    center = revealCenter
                )
            }
        }
    }
}
