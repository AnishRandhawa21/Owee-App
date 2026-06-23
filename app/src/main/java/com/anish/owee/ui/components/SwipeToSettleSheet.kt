package com.anish.owee.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.owee.data.model.PendingPayment
import com.anish.owee.ui.theme.Success
import com.anish.owee.ui.theme.Error
import com.anish.owee.ui.theme.SuccessContainer
import com.anish.owee.ui.theme.OnPrimary
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToSettleSheet(
    payment: PendingPayment,
    isConfirming: Boolean,
    isSuccess: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { if (!isConfirming && !isSuccess) onCancel() },
        sheetState = rememberModalBottomSheetState(confirmValueChange = { !isConfirming && !isSuccess }),
        dragHandle = null,
        containerColor = if (isSuccess) SuccessContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isSuccess) "Payment Settled!" else "Confirm Payment",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSuccess) Success else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isSuccess) "Successfully recorded" else "Did you complete the payment of",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSuccess) Success.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "₹${String.format(Locale.getDefault(), "%.2f", payment.amount)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = if (isSuccess) Success else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Text(
                text = if (isSuccess) "to ${payment.recipientName}" else "to ${payment.recipientName}?",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isSuccess) Success.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            SwipeActionControl(
                isLoading = isConfirming,
                isSuccess = isSuccess,
                onConfirm = onConfirm,
                onCancel = onCancel
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SwipeActionControl(
    isLoading: Boolean,
    isSuccess: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val trackWidth = 300.dp
    val trackWidthPx = with(density) { trackWidth.toPx() }
    val thumbSize = 56.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    
    val maxOffset = (trackWidthPx - thumbSizePx) / 2
    var offsetX by remember { mutableFloatStateOf(0f) }
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isLoading || isSuccess) maxOffset else offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "offset"
    )
    
    LaunchedEffect(offsetX) {
        if (offsetX >= maxOffset * 0.95f) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onConfirm()
        } else if (offsetX <= -maxOffset * 0.95f) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            onCancel()
        }
    }

    Box(
        modifier = Modifier
            .width(trackWidth)
            .height(thumbSize)
            .clip(CircleShape)
            .background(
                if (isSuccess) Success.copy(alpha = 0.3f)
                else if (isLoading) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isLoading && !isSuccess) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(if (offsetX < 0) 1f else 0.3f)) {
                    Icon(Icons.Default.KeyboardArrowLeft, null, tint = Error)
                    Text("NO", color = Error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(if (offsetX > 0) 1f else 0.3f)) {
                    Text("YES", color = Success, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = Success)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .size(thumbSize)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (isSuccess) Success else if (isLoading) Success else MaterialTheme.colorScheme.primary)
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = !isLoading && !isSuccess,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(-maxOffset, maxOffset)
                        if (kotlin.math.abs(offsetX) > maxOffset * 0.5f) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    },
                    onDragStopped = {
                        if (offsetX < maxOffset * 0.95f && offsetX > -maxOffset * 0.95f) {
                            offsetX = 0f
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = if (isSuccess) "success" else if (isLoading) "loading" else if (offsetX > 20) "yes" else if (offsetX < -20) "no" else "arrow",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "icon"
            ) { state ->
                when (state) {
                    "success" -> Icon(Icons.Default.Check, null, tint = OnPrimary)
                    "loading" -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = OnPrimary,
                        strokeWidth = 2.dp
                    )
                    "yes" -> Icon(Icons.Default.Check, null, tint = OnPrimary)
                    "no" -> Icon(Icons.Default.Close, null, tint = OnPrimary)
                    else -> Icon(Icons.Default.KeyboardArrowRight, null, tint = OnPrimary)
                }
            }
        }
    }
}

