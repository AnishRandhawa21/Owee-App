package com.anish.owee.ui.screen.friend.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import com.anish.owee.ui.theme.Error
import com.anish.owee.ui.theme.Success
import com.anish.owee.ui.theme.TextSecondary
import kotlin.math.abs

/**
 * UI-ONLY MOCK. There is no balance/expense data in the backend yet.
 *
 * This is deterministically derived from [friendId] so the same friend
 * always shows the same state on both FriendsScreen and FriendDetailScreen,
 * without any shared state or extra wiring.
 *
 * TODO: Replace [rememberMockBalance] with a real value from a future
 * ExpenseRepository/BalanceRepository once that exists. Nothing else in
 * the UI needs to change — just swap what is passed into FriendCard /
 * the detail screen's balance section.
 */
sealed class BalanceState {
    data class YouOwe(val amount: Int) : BalanceState()
    data class OwesYou(val amount: Int) : BalanceState()
    data object SettledUp : BalanceState()
}

@Composable
fun rememberMockBalance(friendId: String): BalanceState {
    return remember(friendId) {
        val hash = abs(friendId.hashCode())
        when (hash % 3) {
            0 -> BalanceState.YouOwe(amount = 50 + (hash % 12) * 50)
            1 -> BalanceState.OwesYou(amount = 100 + (hash % 16) * 50)
            else -> BalanceState.SettledUp
        }
    }
}

/** Short label for compact contexts like a list row, e.g. "You owe ₹250". */
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

@Composable
fun BalanceState.color(): Color = when (this) {
    is BalanceState.YouOwe -> Error
    is BalanceState.OwesYou -> Success
    is BalanceState.SettledUp -> TextSecondary
}