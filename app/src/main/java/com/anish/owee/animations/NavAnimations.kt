package com.anish.owee.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.tween
import com.anish.owee.navigation.Route

/**
 * Standardized navigation animations for Owee.
 * Restores directional sliding for bottom navigation tabs while keeping 
 * card/FAB expansion backgrounds stationary.
 */
object NavAnimations {
    const val DURATION = 450

    private val bottomNavRoutes = listOf(
        Route.Home.route,
        Route.Friends.route,
        Route.Groups.route,
        Route.Profile.route
    )

    private val expandingRoutes = listOf(
        Route.CreateGroup.route,
        Route.CreateExpense.route,
        Route.GroupDetail.route,
        Route.FriendDetail.route
    )

    private fun getRouteIndex(route: String?): Int {
        if (route == null) return -1
        val baseRoute = route.split("/").firstOrNull()?.split("?")?.firstOrNull()
        return bottomNavRoutes.indexOf(baseRoute)
    }

    private fun isExpandingRoute(route: String?): Boolean {
        if (route == null) return false
        val baseRoute = route.split("/").firstOrNull()?.split("?")?.firstOrNull()
        return expandingRoutes.contains(baseRoute)
    }

    val enterTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
        val initialIndex = getRouteIndex(initialState.destination.route)
        val targetIndex = getRouteIndex(targetState.destination.route)

        if (isExpandingRoute(targetState.destination.route)) {
            fadeIn(animationSpec = tween(DURATION))
        } else if (initialIndex != -1 && targetIndex != -1) {
            // Directional slide for bottom nav
            if (targetIndex > initialIndex) {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(DURATION, easing = EaseInOutQuart)
                ) + fadeIn(animationSpec = tween(DURATION))
            } else {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(DURATION, easing = EaseInOutQuart)
                ) + fadeIn(animationSpec = tween(DURATION))
            }
        } else {
            fadeIn(animationSpec = tween(DURATION))
        }
    }

    val exitTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
        val initialIndex = getRouteIndex(initialState.destination.route)
        val targetIndex = getRouteIndex(targetState.destination.route)

        if (isExpandingRoute(targetState.destination.route)) {
            fadeOut(animationSpec = tween(DURATION))
        } else if (initialIndex != -1 && targetIndex != -1) {
            // Parallax exit for bottom nav
            if (targetIndex > initialIndex) {
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(DURATION, easing = EaseInOutQuart)
                ) + fadeOut(animationSpec = tween(DURATION))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { it / 3 },
                    animationSpec = tween(DURATION, easing = EaseInOutQuart)
                ) + fadeOut(animationSpec = tween(DURATION))
            }
        } else {
            fadeOut(animationSpec = tween(DURATION))
        }
    }

    val popEnterTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition = {
        if (isExpandingRoute(initialState.destination.route)) {
            fadeIn(animationSpec = tween(DURATION))
        } else {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(DURATION, easing = EaseInOutQuart)
            ) + fadeIn(animationSpec = tween(DURATION))
        }
    }

    val popExitTransition: AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition = {
        if (isExpandingRoute(initialState.destination.route)) {
            scaleOut(
                targetScale = 0.9f,
                animationSpec = tween(DURATION, easing = EaseInOutQuart)
            ) + fadeOut(animationSpec = tween(DURATION))
        } else {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(DURATION, easing = EaseInOutQuart)
            ) + fadeOut(animationSpec = tween(DURATION))
        }
    }
}
