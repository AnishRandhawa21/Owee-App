package com.anish.owee.data.repository

import com.anish.owee.data.model.User

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String, nonce: String? = null): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun getCurrentUser(): User?
    suspend fun isUsernameAvailable(username: String): Boolean
    suspend fun createUserProfile(displayName: String, username: String): Result<Unit>
    suspend fun hasActiveSession(): Boolean
    suspend fun needsUsernameSetup(): Boolean
    fun getCurrentEmail(): String?
    fun getCurrentPhotoUrl(): String?
    fun getCurrentFullName(): String?
    suspend fun updateUpiId(
        upiId: String
    ): Result<Unit>

    suspend fun updateFcmToken(
        token: String
    ): Result<Unit>

    suspend fun getUserById(
        userId: String
    ): User?
}
