package com.anish.owee.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.anish.owee.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val TAG = "OWEE_AUTH"

    data class GoogleSignInResult(
        val idToken: String,
        val rawNonce: String?
    )

    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    private fun hashNonce(nonce: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(nonce.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    suspend fun signIn(): Result<GoogleSignInResult> {
        val activity = findActivity(context) ?: return Result.failure(Exception("Activity context missing"))
        
        return try {
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = hashNonce(rawNonce)

            val response: GetCredentialResponse = withContext(Dispatchers.Main) {
                delay(300)

                // Strategy: Include both modern Bottom Sheet and Legacy dialog in ONE request.
                // This is the stable, recommended way to handle fallback without double-popups.
                Log.d(TAG, "Requesting Google Sign-In UI (Combined options)")
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .setNonce(hashedNonce)
                    .build()

                val signInOption = GetSignInWithGoogleOption.Builder(BuildConfig.WEB_CLIENT_ID)
                    .setNonce(hashedNonce)
                    .build()
                
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .addCredentialOption(signInOption)
                    .build()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    executeWithPrepare(activity, request)
                } else {
                    credentialManager.getCredential(activity, request)
                }
            }

            val credential = response.credential
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(GoogleSignInResult(googleIdTokenCredential.idToken, rawNonce))
            } else {
                Result.failure(Exception("Unexpected credential type: ${credential.type}"))
            }

        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "SignIn Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun executeWithPrepare(
        activity: Activity,
        request: GetCredentialRequest
    ): GetCredentialResponse {
        val prepareResponse = try {
            credentialManager.prepareGetCredential(request)
        } catch (e: Exception) {
            null
        }

        return if (prepareResponse != null && prepareResponse.hasCredentialResults(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            val handle = prepareResponse.pendingGetCredentialHandle
            if (handle != null) {
                credentialManager.getCredential(activity, handle)
            } else {
                credentialManager.getCredential(activity, request)
            }
        } else {
            credentialManager.getCredential(activity, request)
        }
    }
}
