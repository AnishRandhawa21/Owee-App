package com.anish.owee.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import com.anish.owee.MainActivity
import com.anish.owee.R
import com.anish.owee.data.local.PreferenceManager
import java.util.Locale

class PaymentReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result {
        if (isAppInForeground(applicationContext)) {
            return ListenableWorker.Result.success()
        }

        val preferenceManager = PreferenceManager(applicationContext)
        val pendingPayment = preferenceManager.getPendingPayment()

        if (pendingPayment != null) {
            showNotification(
                applicationContext,
                "Complete your settlement",
                "Confirm your repayment of ₹${String.format(Locale.getDefault(), "%.2f", pendingPayment.amount)} to ${pendingPayment.recipientName}"
            )
        }

        return ListenableWorker.Result.success()
    }

    companion object {
        fun isAppInForeground(context: Context): Boolean {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager.runningAppProcesses ?: return false
            return appProcesses.any { 
                it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && 
                it.processName == context.packageName 
            }
        }

        fun showNotification(context: Context, title: String, body: String) {
            val channelId = "payment_reminders"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Payment Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableVibration(true)
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                putExtra("OPEN_SETTLEMENT_CONFIRMATION", true)
            } ?: Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_SETTLEMENT_CONFIRMATION", true)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Use MAX for reminders
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            notificationManager.notify(1001, notificationBuilder.build())
        }
    }
}
