package com.nexusai.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexusai.app.domain.repository.ChatRepository
import com.nexusai.app.feature.about.AboutScreen
import com.nexusai.app.feature.chat.ChatScreen
import com.nexusai.app.feature.chat.ChatViewModel
import com.nexusai.app.feature.connection.ConnectionScreen
import com.nexusai.app.feature.history.SearchScreen
import com.nexusai.app.feature.onboarding.OnboardingScreen
import com.nexusai.app.feature.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun NexusNavHost(
    navController: NavHostController = rememberNavController(),
    repository: ChatRepository,
    chatViewModel: ChatViewModel,
    startDestination: String = Screen.Chat.route,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onGetStarted = {
                    scope.launch {
                        repository.setOnboardingCompleted(true)
                    }
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Chat.route) {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToConnection = { navController.navigate(Screen.Connection.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) }
            )
        }

        composable(Screen.Connection.route) {
            ConnectionScreen(
                repository = repository,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.Connection.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConnection = { navController.navigate(Screen.Connection.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToIntro = { navController.navigate(Screen.Onboarding.route) }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onSelectConversation = { convId ->
                    chatViewModel.selectConversation(convId)
                }
            )
        }
    }
}
