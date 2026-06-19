package com.anish.owee.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable


private val OweeColorScheme = lightColorScheme(

    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,

    secondary = Success,
    onSecondary = OnPrimary,

    background = Background,
    onBackground = TextPrimary,

    surface = Surface,
    onSurface = TextPrimary,

    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline = Outline,

    error = Error,
    onError = OnPrimary,
    errorContainer = ErrorContainer
)

@Composable
fun OweeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OweeColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}