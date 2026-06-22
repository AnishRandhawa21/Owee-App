package com.anish.owee.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.anish.owee.ui.screen.friend.CreateFriendRequestScreen
import com.anish.owee.ui.screen.friend.FriendDetailScreen
import com.anish.owee.ui.screen.friend.FriendHistoryScreen
import com.anish.owee.ui.screen.friend.FriendsScreen
import com.anish.owee.ui.screen.group.CreateExpenseScreen
import com.anish.owee.ui.screen.group.CreateGroupScreen
import com.anish.owee.ui.screen.group.GroupDetailScreen
import com.anish.owee.ui.screen.group.GroupHistoryScreen
import com.anish.owee.ui.screen.group.GroupsScreen
import com.anish.owee.ui.screen.home.CustomSettlementScreen
import com.anish.owee.ui.screen.home.HomeScreen
import com.anish.owee.ui.screen.profile.ProfileScreen
import com.anish.owee.viewmodel.SessionViewModel
import com.anish.owee.ui.screen.settlement.SettlementScreen
import com.anish.owee.animations.NavAnimations
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainNavGraph(
    navController: NavHostController,
    sessionViewModel: SessionViewModel,
    snackbarHostState: SnackbarHostState,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.route,
        modifier = modifier,
        enterTransition = NavAnimations.enterTransition,
        exitTransition = NavAnimations.exitTransition,
        popEnterTransition = NavAnimations.popEnterTransition,
        popExitTransition = NavAnimations.popExitTransition
    ) {
        composable(Route.Home.route) {
            HomeScreen(
                onUserClick = { userId ->
                    navController.navigate("${Route.CustomSettlement.route}/$userId")
                }
            )
        }

        composable(Route.Friends.route) {
            FriendsScreen(
                navController = navController,
                snackbarHostState = snackbarHostState,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this
            )
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
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this,
                onBackClick = {
                    navController.popBackStack()
                },
                onRequestMoneyClick = { selectedFriendId, selectedFriendName ->

                    navController.navigate(
                        "${Route.CreateFriendRequest.route}/$selectedFriendId/$selectedFriendName"
                    )
                },
                onSettlementClick = { friendshipId, recipientUserId, amount ->
                    navController.navigate(
                        "${Route.Settlement.route}/FRIEND/$friendshipId/$recipientUserId/$amount"
                    )
                },
                onViewHistoryClick = { id, name ->
                    navController.navigate("${Route.FriendHistory.route}/$id/$name")
                }
            )
        }

        composable(
            route = "${Route.FriendHistory.route}/{friendId}/{friendName}"
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId").orEmpty()
            val friendName = backStackEntry.arguments?.getString("friendName").orEmpty()
            FriendHistoryScreen(
                friendId = friendId,
                friendName = friendName,
                onBack = { navController.popBackStack() }
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
                snackbarHostState = snackbarHostState,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this,
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
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this,
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
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this,
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
                },
                onViewHistoryClick = { id ->
                    navController.navigate("${Route.GroupHistory.route}/$id")
                }
            )
        }

        composable(
            route = "${Route.GroupHistory.route}/{groupId}"
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId").orEmpty()
            GroupHistoryScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Route.CreateGroup.route) {
            CreateGroupScreen(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = this,
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
                onBackClick = {
                    navController.popBackStack()
                },
                onSettlementSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "${Route.CustomSettlement.route}/{userId}"
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId").orEmpty()
            CustomSettlementScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.Profile.route) {
            ProfileScreen(sessionViewModel)
        }
    }
}
