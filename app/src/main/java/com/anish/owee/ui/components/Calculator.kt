package com.anish.owee.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorSheet(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var expression by remember { mutableStateOf(if (initialValue == "0") "" else initialValue) }
    var result by remember { mutableStateOf("") }

    val decimalFormat = DecimalFormat("#.##")

    fun calculateResult(expr: String): String {
        return try {
            val eval = evaluateExpression(expr)
            if (eval.isNaN()) "" else decimalFormat.format(eval)
        } catch (e: Exception) {
            ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onDismiss,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                BottomSheetDefaults.DragHandle()
            }
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expression.ifEmpty { "0" },
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 44.sp,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
                AnimatedVisibility(visible = result.isNotEmpty()) {
                    Text(
                        text = "≈ $result",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Grid of Buttons
            val buttons = listOf(
                listOf("C", "/", "×", "DEL"),
                listOf("7", "8", "9", "-"),
                listOf("4", "5", "6", "+"),
                listOf("1", "2", "3", "="),
                listOf("0", "00", ".", "TICK")
            )

            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { label ->
                        val isOperator = label in listOf("/", "×", "-", "+", "=")
                        val isSpecial = label in listOf("C", "DEL")
                        
                        CalculatorButton(
                            label = label,
                            onClick = {
                                when (label) {
                                    "C" -> {
                                        expression = ""
                                        result = ""
                                    }
                                    "DEL" -> {
                                        if (expression.isNotEmpty()) {
                                            expression = expression.dropLast(1)
                                            result = calculateResult(expression)
                                        }
                                    }
                                    "TICK" -> {
                                        val finalVal = result.ifEmpty { expression }.ifEmpty { "0" }
                                        // Ensure positive value for expense/request UX
                                        val sanitizedVal = try {
                                            val d = finalVal.toDouble()
                                            if (d < 0) kotlin.math.abs(d).toString() else finalVal
                                        } catch (_: Exception) { "0" }
                                        onConfirm(sanitizedVal)
                                    }
                                    "=" -> {
                                        if (result.isNotEmpty()) {
                                            expression = result
                                            result = ""
                                        }
                                    }
                                    "/", "×", "-", "+" -> {
                                        if (expression.isNotEmpty() && expression.last().toString() !in listOf("/", "×", "-", "+", ".")) {
                                            expression += label
                                        }
                                    }
                                    "." -> {
                                        val lastPart = expression.split(Regex("[/×\\-+]")).last()
                                        if (!lastPart.contains(".")) {
                                            expression += "."
                                        }
                                    }
                                    else -> {
                                        if (expression.length < 12) {
                                            expression += label
                                            result = calculateResult(expression)
                                        }
                                    }
                                }
                            },
                            containerColor = when {
                                label == "TICK" -> MaterialTheme.colorScheme.primary
                                isOperator -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                isSpecial -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            },
                            contentColor = when {
                                label == "TICK" -> MaterialTheme.colorScheme.onPrimary
                                isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier
                                .weight(if (label == "TICK") 1.2f else 1f)
                                .height(64.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun CalculatorButton(
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (label) {
                "DEL" -> Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = null, modifier = Modifier.size(24.dp))
                "TICK" -> Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(32.dp))
                else -> Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (label.length > 1) 20.sp else 24.sp
                    )
                )
            }
        }
    }
}

// Simple Expression Evaluator
private fun evaluateExpression(expression: String): Double {
    try {
        val sanitized = expression.replace("×", "*").replace("÷", "/")
        if (sanitized.isEmpty()) return 0.0
        
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < sanitized.length) sanitized[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < sanitized.length) return Double.NaN
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = sanitized.substring(startPos, pos).toDouble()
                } else {
                    return Double.NaN
                }
                return x
            }
        }.parse()
    } catch (_: Exception) {
        return Double.NaN
    }
}
