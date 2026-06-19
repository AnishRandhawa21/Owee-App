package com.anish.owee.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.anish.owee.ui.screen.auth.LoginScreen
import com.anish.owee.ui.screen.auth.UsernameSetupScreen
import com.anish.owee.viewmodel.SessionViewModel

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    sessionViewModel: SessionViewModel
) {
    composable(Route.Login.route) {
        LoginScreen(
            sessionViewModel = sessionViewModel
        )
    }

    composable(Route.UsernameSetup.route) {
        UsernameSetupScreen(
            sessionViewModel = sessionViewModel
        )
    }
}
