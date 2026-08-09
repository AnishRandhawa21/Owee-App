package com.anish.owee.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OfflineBanner(visible: Boolean) {
    ConnectivityBanner(
        visible = visible,
        message = "You are offline. Some features may be limited.",
        backgroundColor = MaterialTheme.colorScheme.error,
        icon = Icons.Rounded.CloudOff
    )
}

@Composable
fun OnlineBanner(visible: Boolean) {
    ConnectivityBanner(
        visible = visible,
        message = "You are back online!",
        backgroundColor = Color(0xFF4CAF50), // Premium Green
        icon = Icons.Rounded.CloudDone
    )
}

@Composable
private fun ConnectivityBanner(
    visible: Boolean,
    message: String,
    backgroundColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(animationSpec = tween(600)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow
            )
        ) + fadeOut(animationSpec = tween(600))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .statusBarsPadding()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
