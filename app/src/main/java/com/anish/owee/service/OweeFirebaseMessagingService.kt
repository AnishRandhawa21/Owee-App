package com.anish.owee.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.anish.owee.MainActivity
import com.anish.owee.R
import com.anish.owee.data.local.PreferenceManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class OweeFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Store the token in preferences
        PreferenceManager(applicationContext).saveFcmToken(token)
        // Note: In a real implementation, we would also upload this token to Supabase here 
        // if the user is already logged in.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // Data payload usually contains our custom logic
        val type = message.data["type"] // e.g., "group_expense", "friend_expense"
        val groupName = message.data["group_name"]
        val payerName = message.data["payer_name"]
        val amount = message.data["amount"]
        val title = message.data["expense_title"]

        val notificationTitle: String
        val notificationBody: String

        when (type) {
            "group_expense" -> {
                notificationTitle = groupName ?: "New Group Expense"
                notificationBody = "$payerName added \"$title\": ₹$amount"
            }
            "friend_expense" -> {
                notificationTitle = "New Expense"
                notificationBody = "$payerName added \"$title\": ₹$amount"
            }
            "friend_request" -> {
                notificationTitle = "Friend Request"
                notificationBody = "$payerName sent you a friend request"
            }
            else -> {
                notificationTitle = message.notification?.title ?: "Owee"
                notificationBody = message.notification?.body ?: "New activity in Owee"
            }
        }
        
        showNotification(notificationTitle, notificationBody)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "owee_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Owee Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon for now
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
