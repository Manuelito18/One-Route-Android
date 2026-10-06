package com.onerouteandroid.oneroute.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignIn : Screen("signin")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object VerifyOtp : Screen("verify_otp")
    object Loading : Screen("loading")
    object Home : Screen("home")
    object Search : Screen("search")
    object LiveNav : Screen("live_nav")
    object MyRides : Screen("my_rides")
    object Profile : Screen("profile")
    object RideDetail : Screen("ride_detail")
}
