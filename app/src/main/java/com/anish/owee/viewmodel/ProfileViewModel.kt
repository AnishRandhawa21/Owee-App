package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.repository.AuthRepository
import com.anish.owee.data.repository.AuthRepositoryImpl
import com.anish.owee.viewmodel.state.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val authRepository: AuthRepository =
        AuthRepositoryImpl()

    private val _uiState =
        MutableStateFlow(ProfileUiState())

    val uiState: StateFlow<ProfileUiState> =
        _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            try {

                val user =
                    authRepository.getCurrentUser()

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        upiId = user?.upiId.orEmpty()
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

    fun updateUpiId(
        upiId: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                upiId = upiId,
                saveSuccess = false
            )
    }

    fun saveUpiId() {

        val upiId =
            _uiState.value.upiId.trim()

        if (upiId.isBlank()) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isSaving = true,
                    error = null
                )

            val result =
                authRepository.updateUpiId(
                    upiId = upiId
                )

            result.onSuccess {

                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true
                    )

                loadProfile()
            }

            result.onFailure {

                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                        error = it.message
                    )
            }
        }
    }

    fun clearSuccess() {

        _uiState.value =
            _uiState.value.copy(
                saveSuccess = false
            )
    }
}