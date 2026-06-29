package com.anish.owee

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.anish.owee.navigation.RootNavGraph
import com.anish.owee.ui.theme.OweeTheme
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anish.owee.ui.components.SwipeToSettleSheet
import com.anish.owee.viewmodel.PendingPaymentViewModel
import com.anish.owee.navigation.Route
import com.anish.owee.navigation.Graph

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        askNotificationPermission()

        setContent {
            OweeTheme {
                val navController = rememberNavController()
                
                // Extract destination from intent if opened from notification
                val startRoute = remember { 
                    handleNotificationIntent(intent)
                }

                RootNavGraph(
                    navController = navController,
                    startRoute = startRoute
                )
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?): String? {
        if (intent == null) return null
        
        val type = intent.getStringExtra("type")
        val isNotification = intent.getBooleanExtra("is_notification", false)
        
        if (!isNotification || type == null) return null

        return when (type) {
            "friend_request" -> Route.Friends.route
            "group_expense" -> {
                val groupId = intent.getStringExtra("group_id")
                if (groupId != null) "${Route.GroupDetail.route}/$groupId" else Route.Home.route
            }
            "settlement", "payment_request" -> Route.Home.route
            else -> null
        }
    }

    override fun onResume() {
        super.onResume()
        // Force a check when returning to app
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[PendingPaymentViewModel::class.java]
        viewModel.checkPendingPayment()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
