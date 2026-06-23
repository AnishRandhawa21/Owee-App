package com.anish.owee.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PaymentReminderManager(private val context: Context) {

    fun scheduleReminders() {
        // 1. Schedule Alarms for 15 and 45 minutes (Guaranteed even if app is closed)
        scheduleAlarm(1, 15)
        scheduleAlarm(2, 45)
        
        // 2. Schedule WorkManager for EOD reminder (Can be deferred by system)
        scheduleEodReminder()
    }

    private fun scheduleAlarm(id: Int, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PaymentReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun scheduleEodReminder() {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 30)
        calendar.set(Calendar.SECOND, 0)
        
        if (calendar.timeInMillis <= now) {
            return
        }
        
        val delay = calendar.timeInMillis - now
        
        if (delay > TimeUnit.HOURS.toMillis(1)) {
            val eodReminder = OneTimeWorkRequestBuilder<PaymentReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("payment_reminder_eod")
                .build()
            WorkManager.getInstance(context).enqueue(eodReminder)
        }
    }

    fun cancelReminders() {
        // Cancel Alarms
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PaymentReminderReceiver::class.java)
        
        listOf(1, 2).forEach { id ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        // Cancel WorkManager
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("payment_reminder_eod")
    }
}
