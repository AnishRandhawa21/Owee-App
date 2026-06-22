package com.anish.owee.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.local.PreferenceManager
import com.anish.owee.data.model.SessionState
import com.anish.owee.data.repository.AuthRepository
import com.anish.owee.data.repository.AuthRepositoryImpl
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = AuthRepositoryImpl()
    private val preferenceManager = PreferenceManager(application)
    private val TAG = "OWEE_AUTH"

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        Log.d(TAG, "SessionViewModel initialized, checking session...")
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            Log.d(TAG, "checkSession() execution started")
            _sessionState.value = SessionState.Loading
            if (authRepository.hasActiveSession()) {
                if (authRepository.needsUsernameSetup()) {
                    Log.d(TAG, "Session state transition: UsernameRequired")
                    _sessionState.value = SessionState.UsernameRequired
                } else {
                    Log.d(TAG, "Session state transition: Authenticated")
                    _sessionState.value = SessionState.Authenticated
                    updateFcmToken()
                }
            } else {
                Log.d(TAG, "Session state transition: Unauthenticated")
                _sessionState.value = SessionState.Unauthenticated
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            Log.d(TAG, "signInWithGoogle() called")
            _sessionState.value = SessionState.Loading
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                if (authRepository.needsUsernameSetup()) {
                    Log.d(TAG, "Sign in success: UsernameRequired")
                    _sessionState.value = SessionState.UsernameRequired
                } else {
                    Log.d(TAG, "Sign in success: Authenticated")
                    _sessionState.value = SessionState.Authenticated
                    updateFcmToken()
                }
            } else {
                Log.d(TAG, "Sign in failed: Unauthenticated")
                _sessionState.value = SessionState.Unauthenticated
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "logout() called")
            authRepository.signOut()
            preferenceManager.clearAll()
            _sessionState.value = SessionState.Unauthenticated
        }
    }

    fun getGoogleFullName(): String? = authRepository.getCurrentFullName()

    fun completeUsernameSetup(displayName: String, username: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            Log.d(TAG, "completeUsernameSetup() called for $username")
            // Check if username is available first
            val isAvailable = authRepository.isUsernameAvailable(username)
            if (!isAvailable) {
                onResult(Result.failure(Exception("Username already taken")))
                return@launch
            }

            val result = authRepository.createUserProfile(displayName, username)
            if (result.isSuccess) {
                Log.d(TAG, "Profile creation success: Authenticated")
                _sessionState.value = SessionState.Authenticated
                updateFcmToken()
            }
            onResult(result)
        }
    }

    fun usernameSetupCompleted() {
        _sessionState.value = SessionState.Authenticated
        updateFcmToken()
    }

    private fun updateFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "FCM Token retrieved: $token")
                viewModelScope.launch {
                    authRepository.updateFcmToken(token)
                    preferenceManager.saveFcmToken(token)
                }
            } else {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
            }
        }
    }
}
