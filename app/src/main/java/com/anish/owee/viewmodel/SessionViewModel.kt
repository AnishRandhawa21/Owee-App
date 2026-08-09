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

    private val _isOfflineLoading = MutableStateFlow(false)
    val isOfflineLoading: StateFlow<Boolean> = _isOfflineLoading.asStateFlow()

    private val _showOfflineBanner = MutableStateFlow(false)
    val showOfflineBanner: StateFlow<Boolean> = _showOfflineBanner.asStateFlow()

    private val _showOnlineBanner = MutableStateFlow(false)
    val showOnlineBanner: StateFlow<Boolean> = _showOnlineBanner.asStateFlow()

    fun triggerOfflineBanner() {
        viewModelScope.launch {
            _showOnlineBanner.value = false // Hide online if offline triggered
            _showOfflineBanner.value = true
            kotlinx.coroutines.delay(3000)
            _showOfflineBanner.value = false
        }
    }

    fun triggerOnlineBanner() {
        viewModelScope.launch {
            _showOfflineBanner.value = false // Hide offline if online triggered
            _showOnlineBanner.value = true
            kotlinx.coroutines.delay(3000)
            _showOnlineBanner.value = false
        }
    }

    fun triggerOfflineRefresh() {
        viewModelScope.launch {
            _isOfflineLoading.value = true
            kotlinx.coroutines.delay(1500)
            _isOfflineLoading.value = false
            triggerOfflineBanner()
        }
    }

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

    fun onSignInStarted() {
        Log.d(TAG, "onSignInStarted() - Setting state to Loading")
        _sessionState.value = SessionState.Loading
    }

    fun onSignInFailed(error: Throwable) {
        Log.e(TAG, "onSignInFailed() - Error: ${error.message}", error)
        _sessionState.value = SessionState.Unauthenticated
    }

    fun signInWithGoogle(idToken: String, nonce: String? = null) {
        viewModelScope.launch {
            val threadName = Thread.currentThread().name
            Log.d(TAG, "signInWithGoogle() called on thread: $threadName")
            // Ensure we are in loading state (should already be, but just in case)
            _sessionState.value = SessionState.Loading
            
            try {
                Log.d(TAG, "Calling authRepository.signInWithGoogle(idToken, nonce)...")
                val result = authRepository.signInWithGoogle(idToken, nonce)
                Log.d(TAG, "authRepository.signInWithGoogle result: ${if (result.isSuccess) "Success" else "Failure"}")

                if (result.isSuccess) {
                    val needsSetup = authRepository.needsUsernameSetup()
                    Log.d(TAG, "Checking needsUsernameSetup: $needsSetup")
                    if (needsSetup) {
                        Log.d(TAG, "Sign in success: Transitioning to UsernameRequired")
                        _sessionState.value = SessionState.UsernameRequired
                    } else {
                        Log.d(TAG, "Sign in success: Transitioning to Authenticated")
                        _sessionState.value = SessionState.Authenticated
                        updateFcmToken()
                    }
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Sign in failed in Repository", error)
                    _sessionState.value = SessionState.Unauthenticated
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in SessionViewModel.signInWithGoogle", e)
                _sessionState.value = SessionState.Unauthenticated
            } finally {
                Log.d(TAG, "signInWithGoogle() execution completed. Final state: ${_sessionState.value}")
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
