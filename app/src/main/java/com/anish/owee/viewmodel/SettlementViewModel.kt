package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.repository.AuthRepository
import com.anish.owee.data.repository.AuthRepositoryImpl
import com.anish.owee.data.repository.SettlementRepositoryImpl
import com.anish.owee.viewmodel.state.SettlementUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettlementViewModel : ViewModel() {

    private val authRepository: AuthRepository =
        AuthRepositoryImpl()

    private val _uiState =
        MutableStateFlow(SettlementUiState())


    val uiState: StateFlow<SettlementUiState> =
        _uiState.asStateFlow()

    fun loadSettlementData(
        userId: String,
        amount: Double,
        sourceType: String,
        sourceId: String
    ) {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            try {

                val user =
                    authRepository.getUserById(
                        userId
                    )

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        amount = amount,
                        sourceType = sourceType,
                        sourceId = sourceId
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
            }
        }
    }

    fun selectPaymentApp(
        appName: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                selectedApp = appName
            )
    }

    fun setPaymentInProgress(
        value: Boolean
    ) {

        _uiState.value =
            _uiState.value.copy(
                isPaymentInProgress = value
            )
    }

    private val settlementRepository =
        SettlementRepositoryImpl()

    fun createSettlement() {

        viewModelScope.launch {

            val currentUser =
                authRepository.getCurrentUser()
                    ?: return@launch

            val targetUser =
                _uiState.value.user
                    ?: return@launch

            val result =
                settlementRepository.createSettlement(
                    sourceType = _uiState.value.sourceType,
                    sourceId = _uiState.value.sourceId,
                    payerId = currentUser.id,
                    receiverId = targetUser.id,
                    amount = _uiState.value.amount
                )

            result.onSuccess {

                android.util.Log.d(
                    "OWEE_SETTLEMENT",
                    "Settlement created"
                )
            }

            result.onFailure {

                android.util.Log.e(
                    "OWEE_SETTLEMENT",
                    "Settlement failed",
                    it
                )
            }
        }
    }

    fun showConfirmationDialog() {

        _uiState.value =
            _uiState.value.copy(
                showConfirmationDialog = true
            )
    }

    fun dismissConfirmationDialog() {

        _uiState.value =
            _uiState.value.copy(
                showConfirmationDialog = false,
                isPaymentInProgress = false
            )
    }
}