package com.anish.owee.data.repository

import com.anish.owee.data.model.OweeNotification
import com.anish.owee.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationRepositoryImpl : NotificationRepository {
    private val postgrest = SupabaseProvider.client.postgrest

    override suspend fun sendNotification(notification: OweeNotification): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest["notifications"].insert(notification)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNotifications(): List<OweeNotification> = withContext(Dispatchers.IO) {
        try {
            postgrest["notifications"].select().decodeList<OweeNotification>()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
