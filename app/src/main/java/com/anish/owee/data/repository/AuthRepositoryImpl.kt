package com.anish.owee.data.repository

import com.anish.owee.data.model.User
import com.anish.owee.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive

class AuthRepositoryImpl : AuthRepository {

    private val client = SupabaseProvider.client
    private val auth = client.auth
    private val postgrest = client.postgrest

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
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
            // Using a count or a check if any user has this username
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
        return auth.currentSessionOrNull() != null
    }

    override suspend fun needsUsernameSetup(): Boolean = withContext(Dispatchers.IO) {
        if (!hasActiveSession()) return@withContext false
        val user = getCurrentUser()
        // Record missing or username not set means setup is required
        user == null || user.username.isBlank()
    }

    override fun getCurrentEmail(): String? = auth.currentUserOrNull()?.email

    override fun getCurrentPhotoUrl(): String? {
        return auth.currentUserOrNull()?.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
    }

    override fun getCurrentFullName(): String? {
        return auth.currentUserOrNull()?.userMetadata?.get("full_name")?.jsonPrimitive?.content
    }
}
