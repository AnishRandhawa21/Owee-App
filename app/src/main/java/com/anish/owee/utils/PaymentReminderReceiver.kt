package com.anish.owee.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anish.owee.data.local.PreferenceManager
import java.util.Locale

class PaymentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preferenceManager = PreferenceManager(context)
        val pendingPayment = preferenceManager.getPendingPayment()

        if (pendingPayment != null) {
            PaymentReminderWorker.showNotification(
                context,
                "Forgot to settle?",
                "Swipe to confirm your ₹${String.format(Locale.getDefault(), "%.2f", pendingPayment.amount)} payment to ${pendingPayment.recipientName}"
            )
        }
    }
}
