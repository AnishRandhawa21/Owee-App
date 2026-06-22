package com.anish.owee.data.repository

import android.util.Log
import com.anish.owee.data.model.OweeNotification
import com.anish.owee.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationRepositoryImpl : NotificationRepository {
    private val postgrest = SupabaseProvider.client.postgrest
    private val TAG = "OWEE_NOTIFICATION"

    override suspend fun sendNotification(notification: OweeNotification): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to insert notification: $notification")
            val result = postgrest["notifications"].insert(notification)
            Log.d(TAG, "Notification insert result status: ${result.toString()}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert notification into database", e)
            Result.failure(e)
        }
    }

    override suspend fun getNotifications(): List<OweeNotification> = withContext(Dispatchers.IO) {
        try {
            postgrest["notifications"].select().decodeList<OweeNotification>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch notifications", e)
            emptyList()
        }
    }
}
