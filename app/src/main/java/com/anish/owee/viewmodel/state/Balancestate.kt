package com.anish.owee.viewmodel.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.anish.owee.ui.theme.Error
import com.anish.owee.ui.theme.Success
import com.anish.owee.ui.theme.TextSecondary
import kotlin.math.abs

sealed class BalanceState {
    data class YouOwe(val amount: Int) : BalanceState()
    data class OwesYou(val amount: Int) : BalanceState()
    data object SettledUp : BalanceState()
}


fun BalanceState.shortLabel(): String = when (this) {
    is BalanceState.YouOwe -> "You owe ₹$amount"
    is BalanceState.OwesYou -> "Owes you ₹$amount"
    is BalanceState.SettledUp -> "Settled up"
}

/** Sentence-form label for the detail screen, e.g. "You owe Rahul ₹250". */
fun BalanceState.sentenceLabel(friendFirstName: String): String = when (this) {
    is BalanceState.YouOwe -> "You owe $friendFirstName ₹$amount"
    is BalanceState.OwesYou -> "$friendFirstName owes you ₹$amount"
    is BalanceState.SettledUp -> "Settled up"
}

fun Double.toBalanceState(): BalanceState {
    return when {
        this > 0 -> BalanceState.OwesYou(this.toInt())
        this < 0 -> BalanceState.YouOwe(kotlin.math.abs(this).toInt())
        else -> BalanceState.SettledUp
    }
}

@Composable
fun BalanceState.color(): Color = when (this) {
    is BalanceState.YouOwe -> Error
    is BalanceState.OwesYou -> Success
    is BalanceState.SettledUp -> TextSecondary
}