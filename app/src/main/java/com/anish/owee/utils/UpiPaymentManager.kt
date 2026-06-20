package com.anish.owee.utils

import android.content.Intent
import android.net.Uri

object UpiPaymentManager {

    fun buildUpiUri(
        upiId: String,
        payeeName: String,
        amount: Double
    ): Uri {

        return Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("am", amount.toString())
            .appendQueryParameter("cu", "INR")
            .build()
    }

    fun createIntent(
        uri: Uri
    ): Intent {

        return Intent(Intent.ACTION_VIEW).apply {
            data = uri
        }
    }
}