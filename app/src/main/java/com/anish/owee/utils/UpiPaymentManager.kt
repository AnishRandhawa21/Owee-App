package com.anish.owee.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale

object UpiPaymentManager {

    fun launchUpiPayment(context: Context, upiId: String, payeeName: String, amount: Double) {
        // Step 1: Silent copy of amount
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        clipboard.setPrimaryClip(ClipData.newPlainText("Owee Amount", formattedAmount))

        val cleanId = upiId.trim()
        val cleanName = Uri.encode(payeeName.trim()) // Encode name because it has spaces
        val uriString = "upi://pay?pa=$cleanId&pn=$cleanName&cu=INR"
        
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        
        try {
            // Step 3: Direct launch via chooser
            context.startActivity(
                Intent.createChooser(intent, "Pay via UPI")
            )
        } catch (e: Exception) {
            Toast.makeText(context, "No UPI app installed", Toast.LENGTH_SHORT).show()
        }
    }
}
