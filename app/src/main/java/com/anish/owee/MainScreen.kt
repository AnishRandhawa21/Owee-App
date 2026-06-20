package com.anish.owee

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anish.owee.navigation.MainNavGraph
import com.anish.owee.navigation.Route
import com.anish.owee.ui.components.BottomNavigationBar
import com.anish.owee.viewmodel.SessionViewModel

@Composable
fun MainScreen(
    sessionViewModel: SessionViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = listOf(
        Route.Home.route,
        Route.Friends.route,
        Route.Groups.route,
        Route.Profile.route
    )
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
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
        // To achieve the "Floating" effect where content flows behind the bar,
        // we don't apply the bottom padding from the scaffold to the MainNavGraph.
        Box(modifier = Modifier.fillMaxSize()) {
            MainNavGraph(
                navController = navController,
                sessionViewModel = sessionViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
