package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.model.SessionState
import com.anish.owee.data.model.User
import com.anish.owee.data.repository.AuthRepository
import com.anish.owee.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionViewModel : ViewModel() {

    private val authRepository: AuthRepository = AuthRepositoryImpl()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _sessionState.value = SessionState.Loading
            if (authRepository.hasActiveSession()) {
                if (authRepository.needsUsernameSetup()) {
                    _sessionState.value = SessionState.UsernameRequired
                } else {
                    _sessionState.value = SessionState.Authenticated
                }
            } else {
                _sessionState.value = SessionState.Unauthenticated
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _sessionState.value = SessionState.Loading
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                if (authRepository.needsUsernameSetup()) {
                    _sessionState.value = SessionState.UsernameRequired
                } else {
                    _sessionState.value = SessionState.Authenticated
                }
            } else {
                _sessionState.value = SessionState.Unauthenticated
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            _sessionState.value = SessionState.Unauthenticated
        }
    }

    fun getGoogleFullName(): String? = authRepository.getCurrentFullName()

    fun completeUsernameSetup(displayName: String, username: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            // Check if username is available first
            val isAvailable = authRepository.isUsernameAvailable(username)
            if (!isAvailable) {
                onResult(Result.failure(Exception("Username already taken")))
                return@launch
            }

            val result = authRepository.createUserProfile(displayName, username)
            if (result.isSuccess) {
                _sessionState.value = SessionState.Authenticated
            }
            onResult(result)
        }
    }

    fun usernameSetupCompleted() {
        _sessionState.value = SessionState.Authenticated
    }
}
