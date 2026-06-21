package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.User

data class CreateExpenseUiState(

    val isLoading: Boolean = false,

    val members: List<User> = emptyList(),

    val title: String = "",

    val amount: String = "",

    val isCustomSplit: Boolean = false,

    val customAmounts: Map<String, String> = emptyMap(), // userId -> amount string

    val selectedParticipantIds: Set<String> = emptySet(),

    val error: String? = null,

    val isSuccess: Boolean = false
)