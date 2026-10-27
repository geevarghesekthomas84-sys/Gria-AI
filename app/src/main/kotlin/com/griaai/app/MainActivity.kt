package com.griaai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.griaai.app.core.theme.GriaAITheme
import com.griaai.app.feature.chat.ChatViewModel
import com.griaai.app.navigation.GriaNavHost
import com.griaai.app.navigation.Screen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GriaApplication
        val repository = app.chatRepository

        setContent {
            val serverConfig by repository.serverConfigFlow.collectAsState(initial = null)
            val isOnboardingCompleted by repository.isOnboardingCompleted.collectAsState(initial = null)

            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = serverConfig?.isDarkTheme ?: systemDark

            GriaAITheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isOnboardingCompleted != null) {
                        val startDestination = if (isOnboardingCompleted == true) {
                            Screen.Chat.route
                        } else {
                            Screen.Onboarding.route
                        }

                        val chatViewModel: ChatViewModel = viewModel {
                            ChatViewModel(repository)
                        }

                        GriaNavHost(
                            repository = repository,
                            chatViewModel = chatViewModel,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}
