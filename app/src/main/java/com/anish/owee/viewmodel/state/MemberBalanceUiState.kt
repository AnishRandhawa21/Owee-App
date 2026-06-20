package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.MemberTransaction

data class MemberBalanceUiState(

    val isLoading: Boolean = false,

    val transactions: List<MemberTransaction> = emptyList(),

    val totalAmount: Double = 0.0,

    val error: String? = null
)