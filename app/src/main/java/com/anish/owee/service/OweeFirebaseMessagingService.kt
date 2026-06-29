package com.anish.owee.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.anish.owee.MainActivity
import com.anish.owee.R
import com.anish.owee.data.local.PreferenceManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OweeFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PreferenceManager(applicationContext).saveFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        if (isAppInForeground()) {
            return
        }

        // If the message contains a notification payload, the OS handles it 
        // when the app is in background. We only want to show a manual notification
        // if there's NO notification payload (data only) OR we want to customize it.
        // To avoid duplicates, we check if notification is null.
        if (message.notification != null) {
            // OS handled it or will handle it. 
            // However, Supabase often sends both. To fix the "Double Notification",
            // we should only proceed if we are purely data-driven.
            // If you want to keep manual control, the backend should send "data" only.
            return 
        }

        val type = message.data["type"]
        val dataTitle = message.data["title"] ?: message.notification?.title
        val dataBody = message.data["body"] ?: message.notification?.body
        val groupName = message.data["group_name"]
        val payerName = message.data["payer_name"] ?: "Someone"
        val amountStr = message.data["amount"]
        val expenseTitle = message.data["expense_title"] ?: "Expense"
        val senderPhotoUrl = message.data["sender_photo"]

        val amount = amountStr?.toDoubleOrNull()?.let {
            String.format(java.util.Locale.US, "%.2f", it)
        } ?: amountStr ?: "0.00"

        var notificationTitle: String
        var notificationBody: String

        when (type) {
            "group_expense" -> {
                notificationTitle = dataTitle ?: groupName ?: "New Group Expense"
                notificationBody = dataBody ?: "$payerName added \"$expenseTitle\": ₹$amount"
            }
            "friend_expense" -> {
                notificationTitle = dataTitle ?: "New Expense"
                notificationBody = dataBody ?: "$payerName added \"$expenseTitle\": ₹$amount"
            }
            "friend_request" -> {
                notificationTitle = dataTitle ?: "Friend Request"
                notificationBody = dataBody ?: "$payerName sent you a friend request"
            }
            "money_request", "payment_request" -> {
                notificationTitle = dataTitle ?: "Money Request"
                notificationBody = dataBody ?: "$payerName requested ₹$amount for \"$expenseTitle\""
            }
            "upi_alert" -> {
                notificationTitle = dataTitle ?: "UPI ID Missing"
                notificationBody = dataBody ?: "Add your UPI ID to receive payments"
            }
            "reminder" -> {
                notificationTitle = dataTitle ?: "Payment Reminder"
                notificationBody = dataBody ?: "A friend is reminding you about a payment"
            }
            "settlement" -> {
                notificationTitle = dataTitle ?: "Payment Received"
                notificationBody = dataBody ?: "$payerName settled ₹$amount"
            }
            else -> {
                notificationTitle = dataTitle ?: "Owee"
                notificationBody = dataBody ?: "New activity in Owee"
            }
        }
        
        // Show notification with image if available
        serviceScope.launch {
            val bitmap = if (!senderPhotoUrl.isNullOrBlank()) {
                getBitmapFromUrl(senderPhotoUrl)
            } else {
                null
            }
            showNotification(notificationTitle, notificationBody, bitmap, message.data)
        }
    }

    private suspend fun getBitmapFromUrl(url: String): Bitmap? {
        val loader = ImageLoader(this)
        val request = ImageRequest.Builder(this)
            .data(url)
            .transformations(CircleCropTransformation())
            .build()
        
        val result = loader.execute(request)
        return (result.drawable as? BitmapDrawable)?.bitmap
    }

    private fun showNotification(title: String, body: String, largeIcon: Bitmap? = null, data: Map<String, String>? = null) {
        val channelId = "owee_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Owee Updates",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data?.forEach { (key, value) ->
                putExtra(key, value)
            }
            // Add a flag to identify this was from a notification
            putExtra("is_notification", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .apply {
                if (largeIcon != null) {
                    setLargeIcon(largeIcon)
                }
            }

        // Fixed ID based on type to prevent spamming, or use unique ID if they are different events
        val notificationId = when(data?.get("type")) {
            "friend_request" -> 1001
            "group_expense" -> 1002
            else -> System.currentTimeMillis().toInt()
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        return appProcesses.any { 
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && 
            it.processName == packageName 
        }
    }
}
