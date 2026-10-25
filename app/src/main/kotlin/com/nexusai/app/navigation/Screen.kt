package com.nexusai.app.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Chat : Screen("chat")
    data object Connection : Screen("connection")
    data object Settings : Screen("settings")
    data object About : Screen("about")
    data object Search : Screen("search")
}
