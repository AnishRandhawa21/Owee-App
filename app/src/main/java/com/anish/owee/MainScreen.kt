package com.anish.owee

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anish.owee.navigation.MainNavGraph
import com.anish.owee.ui.components.BottomNavigationBar
import com.anish.owee.viewmodel.SessionViewModel

@Composable
fun MainScreen(
    sessionViewModel: SessionViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
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
    ) { paddingValues ->
        MainNavGraph(
            navController = navController,
            sessionViewModel = sessionViewModel,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
