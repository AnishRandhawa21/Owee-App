package com.anish.owee.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.anish.owee.ui.screen.friend.FriendDetailScreen
import com.anish.owee.ui.screen.friend.FriendsScreen
import com.anish.owee.ui.screen.group.GroupsScreen
import com.anish.owee.ui.screen.home.HomeScreen
import com.anish.owee.ui.screen.profile.ProfileScreen
import com.anish.owee.viewmodel.SessionViewModel

@Composable
fun MainNavGraph(
    navController: NavHostController,
    sessionViewModel: SessionViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.route,
        modifier = modifier
    ) {
        composable(Route.Home.route) {
            HomeScreen()
        }

        composable(Route.Friends.route) {
            FriendsScreen(navController = navController)
        }
        composable(
            route = "${Route.FriendDetail.route}/{friendId}"
        ) { backStackEntry ->

            val friendId =
                backStackEntry.arguments
                    ?.getString("friendId")
                    .orEmpty()

            FriendDetailScreen(
                friendId = friendId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.Groups.route) {
            GroupsScreen()
        }

        composable(Route.Profile.route) {
            ProfileScreen(sessionViewModel)
        }
    }
}
