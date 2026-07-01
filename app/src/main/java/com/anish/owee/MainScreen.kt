package com.anish.owee

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
        SharedTransitionLayout {
            Box(modifier = Modifier.fillMaxSize()) {
                MainNavGraph(
                    navController = navController,
                    sessionViewModel = sessionViewModel,
                    themeViewModel = themeViewModel,
                    snackbarHostState = snackbarHostState,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    modifier = Modifier.fillMaxSize()
                )

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
