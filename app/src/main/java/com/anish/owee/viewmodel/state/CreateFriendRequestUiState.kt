package com.anish.owee.viewmodel.state

data class CreateFriendRequestUiState(

    val amount: String = "",

    val note: String = "",

    val isLoading: Boolean = false,

    val isSuccess: Boolean = false,

    val error: String? = null
)