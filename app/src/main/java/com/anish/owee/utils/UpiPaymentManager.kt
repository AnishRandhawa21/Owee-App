package com.anish.owee.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri

data class UpiApp(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

object UpiPaymentManager {

    fun getInstalledUpiApps(context: Context): List<UpiApp> {
        val packageManager = context.packageManager
        
        // 1. Try to find apps by Intent query for upi://pay
        val uri = Uri.parse("upi://pay")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        
        val apps = resolveInfoList.map {
            UpiApp(
                name = it.loadLabel(packageManager).toString(),
                packageName = it.activityInfo.packageName,
                icon = it.loadIcon(packageManager)
            )
        }.toMutableList()

        // 2. Fallback: Check common UPI packages manually
        val commonPackages = listOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "net.one97.paytm",
            "in.org.npci.upiapp",
            "com.amazon.mShop.android.shopping",
            "money.super.payments"
        )

        for (pkg in commonPackages) {
            if (apps.none { it.packageName == pkg }) {
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    if (appInfo.enabled) {
                        apps.add(UpiApp(
                            name = packageManager.getApplicationLabel(appInfo).toString(),
                            packageName = pkg,
                            icon = packageManager.getApplicationIcon(appInfo)
                        ))
                    }
                } catch (e: Exception) {
                    // App not installed
                }
            }
        }

        val priorityMap = mapOf(
            "com.google.android.apps.nbu.paisa.user" to 1,
            "money.super.payments" to 2,
            "com.phonepe.app" to 3,
            "net.one97.paytm" to 4
        )

        return apps.distinctBy { it.packageName }
            .sortedWith(compareBy<UpiApp> { priorityMap[it.packageName] ?: Int.MAX_VALUE }.thenBy { it.name })
    }

    fun copyUpiId(context: Context, upiId: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UPI ID", upiId.trim())
        clipboard.setPrimaryClip(clip)
    }

    fun launchUpiApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
        }
    }
}
