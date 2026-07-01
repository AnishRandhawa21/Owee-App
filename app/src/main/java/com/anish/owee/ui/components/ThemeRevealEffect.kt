package com.anish.owee.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.anish.owee.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun ThemeRevealEffect(
    themeMode: ThemeMode,
    clickOffset: Offset?,
    onAnimationFinished: () -> Unit
) {
    val radius = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var revealColor by remember { mutableStateOf(Color.Transparent) }
    var revealCenter by remember { mutableStateOf(Offset.Zero) }
    
    // We use the current background color from the theme.
    // Since this component is inside the theme provider, it will update
    // as soon as the theme changes.
    val currentThemeBackground = MaterialTheme.colorScheme.background

    LaunchedEffect(themeMode, clickOffset) {
        if (clickOffset != null && clickOffset != Offset.Zero) {
            revealCenter = clickOffset
            revealColor = currentThemeBackground
            
            // Calculate a radius that definitely covers any screen
            val maxRadius = 3000f 
            
            radius.snapTo(0f)
            scope.launch {
                radius.animateTo(
                    targetValue = maxRadius,
                    animationSpec = tween(durationMillis = 1200)
                )
                onAnimationFinished()
                radius.snapTo(0f)
            }
        }
    }

    if (radius.value > 0f) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = revealColor,
                radius = radius.value,
                center = revealCenter
            )
        }
    }
}
