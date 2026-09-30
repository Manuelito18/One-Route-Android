package com.onerouteandroid.oneroute.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object LiveNav : Screen("live_nav")
    object MyRides : Screen("my_rides")
    object Profile : Screen("profile")
    object RideDetail : Screen("ride_detail")
}
