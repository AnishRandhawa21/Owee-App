package com.anish.owee.data.repository

import com.anish.owee.data.model.OweeNotification

interface NotificationRepository {
    suspend fun sendNotification(notification: OweeNotification): Result<Unit>
    suspend fun getNotifications(): List<OweeNotification>
}
