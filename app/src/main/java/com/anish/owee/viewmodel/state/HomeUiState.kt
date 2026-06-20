package com.anish.owee.viewmodel.state

data class HomeUiState(

    val isLoading: Boolean = false,

    val totalBalance: Double = 0.0,

    val groupBalance: Double = 0.0,

    val friendBalance: Double = 0.0,

    val error: String? = null
)