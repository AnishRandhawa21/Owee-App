package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.User

data class SettlementUiState(

    val isLoading: Boolean = false,

    val user: User? = null,

    val amount: Double = 0.0,

    val sourceType: String = "",

    val sourceId: String = "",

    val selectedApp: String? = null,

    val isPaymentInProgress: Boolean = false,

    val error: String? = null,

    val showConfirmationDialog: Boolean = false,

    val settlementSuccess: Boolean = false
)