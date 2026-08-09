package com.anish.owee

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anish.owee.navigation.MainNavGraph
import com.anish.owee.navigation.Route
import com.anish.owee.ui.components.BottomNavigationBar
import com.anish.owee.ui.components.SwipeToSettleSheet
import com.anish.owee.viewmodel.PendingPaymentViewModel
import com.anish.owee.viewmodel.SessionViewModel
import com.anish.owee.viewmodel.ThemeViewModel

import com.anish.owee.ui.components.OfflineBanner
import com.anish.owee.ui.components.OnlineBanner
import com.anish.owee.utils.ConnectivityObserver
import com.anish.owee.utils.NetworkConnectivityObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    sessionViewModel: SessionViewModel,
    themeViewModel: ThemeViewModel
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val status by connectivityObserver.observe().collectAsState(initial = ConnectivityObserver.Status.Available)
    val isOffline = status != ConnectivityObserver.Status.Available

    val showOfflineBanner by sessionViewModel.showOfflineBanner.collectAsState()
    val showOnlineBanner by sessionViewModel.showOnlineBanner.collectAsState()

    var wasOffline by remember { mutableStateOf(false) }

    LaunchedEffect(isOffline) {
        if (wasOffline && !isOffline) {
            // First time back online after being offline
            sessionViewModel.triggerOnlineBanner()
        }
        wasOffline = isOffline
    }

    val pendingViewModel: PendingPaymentViewModel = viewModel(context as ComponentActivity)
    val pendingPayment by pendingViewModel.pendingPayment.collectAsState()
    val isConfirming by pendingViewModel.isConfirming.collectAsState()
    val isSuccess by pendingViewModel.isSuccess.collectAsState()

    LaunchedEffect(Unit) {
        pendingViewModel.checkPendingPayment()
    }

    LaunchedEffect(Unit) {
        pendingViewModel.navigationEvent.collect { event ->
            when (event) {
                is com.anish.owee.viewmodel.PendingPaymentEvent.Navigate -> {
                    navController.navigate(event.route) {
                        // Clear the settlement screen from the backstack so pressing "Back"
                        // from the detail screen doesn't take the user back to settlement.
                        navController.currentDestination?.route?.let { currentRouteName ->
                            if (currentRouteName.contains("settlement", ignoreCase = true)) {
                                popUpTo(currentRouteName) { inclusive = true }
                            }
                        }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    val topLevelRoutes = listOf(
        Route.Home.route,
        Route.Friends.route,
        Route.Groups.route,
        Route.Profile.route
    )
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        snackbarHost = {
            if (showBottomBar) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 96.dp)
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        // Use paddingValues to satisfy Scaffold requirement, but we don't apply it to the main container
        // to maintain the "growing under bottom bar" effect.
        val bottomPadding = paddingValues.calculateBottomPadding()
        
        SharedTransitionLayout {
            // Restore the Box root to allow content to go under the Bottom Bar (Y=0 to Y=ScreenHeight)
            // OfflineBanner will be overlaid at the top.
            Box(modifier = Modifier.fillMaxSize()) {
                // Content (behind Bottom Bar)
                MainNavGraph(
                    navController = navController,
                    sessionViewModel = sessionViewModel,
                    themeViewModel = themeViewModel,
                    snackbarHostState = snackbarHostState,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    modifier = Modifier.fillMaxSize()
                )

                // The banners are overlaid at the top. 
                OfflineBanner(visible = showOfflineBanner)
                OnlineBanner(visible = showOnlineBanner)

                pendingPayment?.let { payment ->
                    SwipeToSettleSheet(
                        payment = payment,
                        isConfirming = isConfirming,
                        isSuccess = isSuccess,
                        onConfirm = { pendingViewModel.confirmPayment() },
                        onCancel = { pendingViewModel.cancelPayment() }
                    )
                }
            }
        }
    }
}
