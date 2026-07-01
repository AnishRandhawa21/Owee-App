package com.anish.owee.navigation

import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.anish.owee.MainScreen
import com.anish.owee.data.model.SessionState
import com.anish.owee.ui.screen.auth.SplashScreen
import com.anish.owee.viewmodel.SessionViewModel
import com.anish.owee.viewmodel.ThemeViewModel
import com.anish.owee.animations.NavAnimations
import kotlinx.coroutines.delay

@Composable
fun RootNavGraph(
    navController: NavHostController,
    sessionViewModel: SessionViewModel,
    themeViewModel: ThemeViewModel,
    startRoute: String? = null
) {
    val sessionState by sessionViewModel.sessionState.collectAsState()
    var isMinSplashTimePassed by remember { mutableStateOf(false) }
    var isSplashFinished by remember { mutableStateOf(false) }

    val isReadyToNavigate = sessionState !is SessionState.Loading && isMinSplashTimePassed

    LaunchedEffect(Unit) {
        delay(1500) // Minimum splash duration
        isMinSplashTimePassed = true
    }

    LaunchedEffect(sessionState, isSplashFinished) {
        // Global navigation handler for session state changes
        // This handles cases like auto-login after splash or logout from any screen
        val currentRoute = navController.currentBackStackEntry?.destination?.route

        // Wait for session to load
        if (sessionState is SessionState.Loading) return@LaunchedEffect

        // If on splash, wait for both session and animation to finish
        if (currentRoute == Route.Splash.route && !isSplashFinished) return@LaunchedEffect

        when (sessionState) {
            SessionState.Unauthenticated -> {
                if (currentRoute != Route.Login.route) {
                    navController.navigate(Graph.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            SessionState.UsernameRequired -> {
                if (currentRoute != Route.UsernameSetup.route) {
                    navController.navigate(Route.UsernameSetup.route) {
                        launchSingleTop = true
                    }
                }
            }
            SessionState.Authenticated -> {
                // Determine destination: Priority is startRoute (Deep Link), then Home
                val destination = if (startRoute != null && currentRoute == Route.Splash.route) {
                    startRoute
                } else {
                    Graph.MAIN
                }

                if (currentRoute != Route.Home.route && currentRoute != null) {
                    navController.navigate(destination) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = Route.Splash.route,
        route = Graph.ROOT,
        enterTransition = NavAnimations.enterTransition,
        exitTransition = NavAnimations.exitTransition,
        popEnterTransition = NavAnimations.popEnterTransition,
        popExitTransition = NavAnimations.popExitTransition
    ) {
        composable(Route.Splash.route) {
            SplashScreen(
                isReadyToNavigate = isReadyToNavigate,
                onNavigateNext = {
                    isSplashFinished = true
                }
            )
        }

        navigation(
            route = Graph.AUTH,
            startDestination = Route.Login.route
        ) {
            authNavGraph(navController, sessionViewModel)
        }

        navigation(
            route = Graph.MAIN,
            startDestination = Route.Home.route,
            enterTransition = {
                fadeIn(animationSpec = tween(700))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(700))
            }
        ) {
            composable(Route.Home.route) {
                MainScreen(
                    sessionViewModel = sessionViewModel,
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}
