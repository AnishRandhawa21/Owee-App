package com.anish.owee.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.anish.owee.MainScreen
import com.anish.owee.data.model.SessionState
import com.anish.owee.ui.screen.auth.SplashScreen
import com.anish.owee.viewmodel.SessionViewModel
import com.anish.owee.animations.NavAnimations

@Composable
fun RootNavGraph(
    navController: NavHostController,
    sessionViewModel: SessionViewModel = viewModel()
) {
    val sessionState by sessionViewModel.sessionState.collectAsState()

    LaunchedEffect(sessionState) {
        // Global navigation handler for session state changes
        // This handles cases like auto-login after splash or logout from any screen
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute != Route.Splash.route) {
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
                    if (currentRoute != Route.Home.route && currentRoute != null) {
                        navController.navigate(Graph.MAIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
                else -> {}
            }
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
                onNavigateNext = {
                    when (sessionState) {
                        SessionState.Unauthenticated -> {
                            navController.navigate(Graph.AUTH) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                            }
                        }
                        SessionState.UsernameRequired -> {
                            navController.navigate(Route.UsernameSetup.route) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                            }
                        }
                        SessionState.Authenticated -> {
                            navController.navigate(Graph.MAIN) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                            }
                        }
                        SessionState.Loading -> {
                            // If still loading after splash animation, 
                            // we could show a loading indicator or wait
                        }
                    }
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
            startDestination = Route.Home.route
        ) {
            composable(Route.Home.route) {
                MainScreen(sessionViewModel)
            }
        }
    }
}
