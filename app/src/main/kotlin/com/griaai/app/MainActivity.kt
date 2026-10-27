package com.griaai.app

import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.griaai.app.core.theme.GriaAITheme
import com.griaai.app.core.theme.GriaTypography
import com.griaai.app.core.theme.LocalGriaColors
import com.griaai.app.feature.chat.ChatViewModel
import com.griaai.app.navigation.GriaNavHost
import com.griaai.app.navigation.Screen

class MainActivity : ComponentActivity() {

    private fun promptBiometric(onSuccess: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val prompt = BiometricPrompt.Builder(this)
                    .setTitle("Unlock Gria-AI")
                    .setSubtitle("Authenticate using fingerprint or screen lock")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()

                prompt.authenticate(
                    CancellationSignal(),
                    mainExecutor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            onSuccess()
                        }
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            // User canceled or failed
                        }
                    }
                )
            } catch (e: Exception) {
                // Fallback unlock if device lacks hardware
                onSuccess()
            }
        } else {
            onSuccess()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GriaApplication
        val repository = app.chatRepository

        setContent {
            val serverConfig by repository.serverConfigFlow.collectAsState(initial = null)
            val isOnboardingCompleted by repository.isOnboardingCompleted.collectAsState(initial = null)
            val isBiometricEnabled by repository.isBiometricEnabled.collectAsState(initial = false)

            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = serverConfig?.isDarkTheme ?: systemDark

            var isUnlocked by remember { mutableStateOf(!isBiometricEnabled) }

            LaunchedEffect(isBiometricEnabled) {
                if (isBiometricEnabled && !isUnlocked) {
                    promptBiometric { isUnlocked = true }
                } else if (!isBiometricEnabled) {
                    isUnlocked = true
                }
            }

            GriaAITheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isBiometricEnabled && !isUnlocked) {
                        BiometricLockScreen(
                            onUnlock = { promptBiometric { isUnlocked = true } }
                        )
                    } else if (isOnboardingCompleted != null) {
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

@Composable
fun BiometricLockScreen(onUnlock: () -> Unit) {
    val colors = LocalGriaColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Lock",
                    tint = colors.accent,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Gria Vault Locked",
                style = GriaTypography.headlineSmall.copy(color = colors.textPrimary)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Biometric authentication is required to access your conversations.",
                style = GriaTypography.bodySmall.copy(color = colors.textSecondary),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text(
                    text = "Unlock",
                    color = colors.surfaceElevated,
                    style = GriaTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
