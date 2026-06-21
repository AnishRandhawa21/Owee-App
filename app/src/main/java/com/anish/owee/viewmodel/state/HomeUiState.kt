package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.User
import kotlinx.serialization.Serializable

@Serializable
data class DebtSource(
    val sourceType: String, // "GROUP" or "FRIEND"
    val sourceId: String,
    val amount: Double,
    val createdAt: String
)

@Serializable
data class UserTotalBalance(
    val user: User,
    val balance: Double,
    val sources: List<DebtSource>
)

data class HomeUiState(

    val isLoading: Boolean = false,

    val totalBalance: Double = 0.0,

    val groupBalance: Double = 0.0,

    val friendBalance: Double = 0.0,

    val userBalances: List<UserTotalBalance> = emptyList(),

    val error: String? = null
)
