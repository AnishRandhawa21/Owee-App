package com.anish.owee.navigation

sealed class Route(val route: String) {

    // Root
    data object Splash : Route("splash")

    // Auth
    data object Login : Route("login")
    data object UsernameSetup : Route("username_setup")

    // Bottom Navigation
    data object Home : Route("home")
    data object Friends : Route("friends")
    data object FriendDetail : Route("friend_detail")
    data object Groups : Route("groups")
    data object Profile : Route("profile")
}