package com.anish.owee.data.repository

import android.util.Log
import com.anish.owee.data.model.User
import com.anish.owee.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive

class AuthRepositoryImpl : AuthRepository {

    private val client = SupabaseProvider.client
    private val auth = client.auth
    private val postgrest = client.postgrest
    private val TAG = "OWEE_AUTH"

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Signing in with Google ID Token")
            auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
            }
            Log.d(TAG, "Supabase sign in success. Session: ${auth.currentSessionOrNull()}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sign in failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Signing out...")
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val userId = auth.currentUserOrNull()?.id ?: return@withContext null
        try {
            postgrest["users"].select {
                filter {
                    eq("id", userId)
                }
            }.decodeSingleOrNull<User>()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun isUsernameAvailable(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = postgrest["users"].select {
                filter {
                    eq("username", username)
                }
            }.decodeSingleOrNull<User>()
            result == null
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun createUserProfile(displayName: String, username: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUserOrNull() ?: return@withContext Result.failure(Exception("Not authenticated"))
        
        try {
            val newUser = User(
                id = currentUser.id,
                email = currentUser.email ?: "",
                displayName = displayName,
                username = username,
                photoUrl = getCurrentPhotoUrl(),
            )
            postgrest["users"].insert(newUser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasActiveSession(): Boolean {
        Log.d(TAG, "Checking for active session... Status: ${auth.sessionStatus.value}")
        
        // Wait for session to finish initializing from storage if it hasn't already
        if (auth.sessionStatus.value is SessionStatus.Initializing) {
            Log.d(TAG, "Session is still initializing, waiting...")
            auth.sessionStatus.filterNot { it is SessionStatus.Initializing }.first()
            Log.d(TAG, "Session status resolved to: ${auth.sessionStatus.value}")
        }
        
        val hasSession = auth.currentSessionOrNull() != null
        Log.d(TAG, "Has active session: $hasSession")
        return hasSession
    }

    override suspend fun needsUsernameSetup(): Boolean = withContext(Dispatchers.IO) {
        if (!hasActiveSession()) return@withContext false
        val user = getCurrentUser()
        Log.d(TAG, "Fetched user profile: $user")
        val needsSetup = user == null || user.username.isBlank()
        Log.d(TAG, "Needs username setup: $needsSetup")
        needsSetup
    }

    override fun getCurrentEmail(): String? = auth.currentUserOrNull()?.email

    override fun getCurrentPhotoUrl(): String? {
        return auth.currentUserOrNull()?.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
    }

    override fun getCurrentFullName(): String? {
        return auth.currentUserOrNull()?.userMetadata?.get("full_name")?.jsonPrimitive?.content
    }
}
