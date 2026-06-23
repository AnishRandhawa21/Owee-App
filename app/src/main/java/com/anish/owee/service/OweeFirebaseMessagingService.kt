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
            // Don't show notification if app is in foreground
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
            showNotification(notificationTitle, notificationBody, bitmap)
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

    private fun showNotification(title: String, body: String, largeIcon: Bitmap? = null) {
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

        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        } ?: Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
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

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
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
