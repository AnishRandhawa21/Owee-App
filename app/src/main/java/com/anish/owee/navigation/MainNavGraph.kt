package com.anish.owee.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.anish.owee.ui.screen.friend.CreateFriendRequestScreen
import com.anish.owee.ui.screen.friend.FriendDetailScreen
import com.anish.owee.ui.screen.friend.FriendsScreen
import com.anish.owee.ui.screen.group.CreateExpenseScreen
import com.anish.owee.ui.screen.group.CreateGroupScreen
import com.anish.owee.ui.screen.group.GroupDetailScreen
import com.anish.owee.ui.screen.group.GroupsScreen
import com.anish.owee.ui.screen.home.HomeScreen
import com.anish.owee.ui.screen.profile.ProfileScreen
import com.anish.owee.viewmodel.SessionViewModel
import com.anish.owee.ui.screen.settlement.SettlementScreen
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
                },
                onRequestMoneyClick = { selectedFriendId, selectedFriendName ->

                    navController.navigate(
                        "${Route.CreateFriendRequest.route}/$selectedFriendId/$selectedFriendName"
                    )
                },
                onSettlementClick = { selectedFriendId, _, amount ->
                    navController.navigate(
                        "${Route.Settlement.route}/FRIEND/$selectedFriendId/ME/$amount"
                    )
                }
            )
        }

        composable(
            route = "${Route.CreateFriendRequest.route}/{friendId}/{friendName}"
        ) { backStackEntry ->

            val friendId =
                backStackEntry.arguments
                    ?.getString("friendId")
                    .orEmpty()
            val friendName =
                backStackEntry.arguments
                    ?.getString("friendName")
                    .orEmpty()

            CreateFriendRequestScreen(
                friendId = friendId,
                friendName = friendName,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.Groups.route) {
            GroupsScreen(
                onCreateGroupClick = {
                    navController.navigate(
                        Route.CreateGroup.route
                    )
                },
                onGroupClick = { groupId ->
                    navController.navigate(
                        "${Route.GroupDetail.route}/$groupId"
                    )
                }
            )
        }

        composable(
            route = "${Route.CreateExpense.route}/{groupId}"
        ) { backStackEntry ->

            val groupId =
                backStackEntry.arguments
                    ?.getString("groupId")
                    .orEmpty()

            CreateExpenseScreen(
                groupId = groupId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = "${Route.GroupDetail.route}/{groupId}"
        ) { backStackEntry ->

            val groupId =
                backStackEntry.arguments
                    ?.getString("groupId")
                    .orEmpty()

            GroupDetailScreen(
                groupId = groupId,
                onAddExpenseClick = { selectedGroupId ->
                    navController.navigate(
                        "${Route.CreateExpense.route}/$selectedGroupId"
                    )
                },
                onBackClick = {
                    navController.popBackStack()
                },
                onSettlementClick = {
                        selectedGroupId,
                        memberId,
                        amount ->

                    navController.navigate(
                        "${Route.Settlement.route}/GROUP/$selectedGroupId/$memberId/$amount"
                    )
                }
            )
        }
        composable(Route.CreateGroup.route) {
            CreateGroupScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route =
                "${Route.Settlement.route}/{sourceType}/{sourceId}/{userId}/{amount}"
        ) { backStackEntry ->

            val sourceType =
                backStackEntry.arguments
                    ?.getString("sourceType")
                    .orEmpty()

            val sourceId =
                backStackEntry.arguments
                    ?.getString("sourceId")
                    .orEmpty()

            val userId =
                backStackEntry.arguments
                    ?.getString("userId")
                    .orEmpty()

            val amount =
                backStackEntry.arguments
                    ?.getString("amount")
                    ?.toDoubleOrNull()
                    ?: 0.0

            SettlementScreen(
                sourceType = sourceType,
                sourceId = sourceId,
                userId = userId,
                amount = amount,
                onSettlementSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.Profile.route) {
            ProfileScreen(sessionViewModel)
        }
    }
}
