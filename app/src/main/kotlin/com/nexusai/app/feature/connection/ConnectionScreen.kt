package com.nexusai.app.feature.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusai.app.core.theme.LocalNexusColors
import com.nexusai.app.core.theme.NexusTypography
import com.nexusai.app.domain.model.ConnectionStatus
import com.nexusai.app.domain.model.ServerConfig
import com.nexusai.app.domain.repository.ChatRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    repository: ChatRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNexusColors.current
    val currentConfig by repository.serverConfigFlow.collectAsState(initial = ServerConfig())
    val scope = rememberCoroutineScope()

    var ipAddress by remember(currentConfig) { mutableStateOf(currentConfig.ipAddress) }
    var portText by remember(currentConfig) { mutableStateOf(currentConfig.port.toString()) }
    var useHttps by remember(currentConfig) { mutableStateOf(currentConfig.useHttps) }
    var timeoutText by remember(currentConfig) { mutableStateOf(currentConfig.timeoutSeconds.toString()) }

    var testStatus by remember { mutableStateOf<ConnectionStatus?>(null) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    fun testConnectionNow() {
        val port = portText.toIntOrNull() ?: 1234
        val scheme = if (useHttps) "https" else "http"
        val testBaseUrl = "$scheme://$ipAddress:$port"

        isTesting = true
        testStatus = ConnectionStatus.CONNECTING
        testMessage = "Testing endpoint $testBaseUrl..."

        scope.launch {
            val result = repository.testConnection(testBaseUrl)
            isTesting = false
            if (result.isSuccess) {
                val models = result.getOrNull().orEmpty()
                testStatus = ConnectionStatus.CONNECTED
                testMessage = "Connected to LM Studio (${models.size} models found)"
            } else {
                testStatus = ConnectionStatus.ERROR
                testMessage = "Unable to connect: ${result.exceptionOrNull()?.localizedMessage ?: "Connection refused"}"
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        topBar = {
            Surface(
                color = colors.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary)
                        }
                    }
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Heading: Calm, friendly setup page
            Text(
                text = "Connect to your AI",
                style = NexusTypography.headlineLarge.copy(color = colors.textPrimary)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Supporting description
            Text(
                text = "Your AI runs privately on your computer. Connect both devices to the same Wi-Fi.",
                style = NexusTypography.bodyLarge.copy(color = colors.textSecondary),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Presets
            Row(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier
                        .clickable {
                            ipAddress = "192.168.1.100"
                            portText = "1234"
                        }
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Standard Wi-Fi (192.168.1.100)",
                        style = NexusTypography.labelSmall.copy(color = colors.textSecondary),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.clickable {
                        ipAddress = "10.0.2.2"
                        portText = "1234"
                    }
                ) {
                    Text(
                        text = "Emulator (10.0.2.2)",
                        style = NexusTypography.labelSmall.copy(color = colors.textSecondary),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form inputs
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Server IP Address",
                        style = NexusTypography.labelMedium.copy(color = colors.textSecondary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        placeholder = { Text("192.168.1.100", color = colors.textTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.accent
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Port",
                                style = NexusTypography.labelMedium.copy(color = colors.textSecondary)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = portText,
                                onValueChange = { portText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("1234", color = colors.textTertiary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accent,
                                    unfocusedBorderColor = colors.borderSubtle,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    cursorColor = colors.accent
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Timeout (s)",
                                style = NexusTypography.labelMedium.copy(color = colors.textSecondary)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = timeoutText,
                                onValueChange = { timeoutText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("60", color = colors.textTertiary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accent,
                                    unfocusedBorderColor = colors.borderSubtle,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    cursorColor = colors.accent
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use HTTPS",
                                style = NexusTypography.bodyMedium.copy(color = colors.textPrimary)
                            )
                            Text(
                                text = "Enable if routing through a local secure proxy",
                                style = NexusTypography.bodySmall.copy(color = colors.textTertiary)
                            )
                        }
                        Switch(
                            checked = useHttps,
                            onCheckedChange = { useHttps = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.surfaceElevated,
                                checkedTrackColor = colors.accent
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test status feedback
            if (testStatus != null && testMessage != null) {
                val statusColor = when (testStatus) {
                    ConnectionStatus.CONNECTED -> colors.statusSuccess
                    ConnectionStatus.ERROR -> colors.statusError
                    else -> colors.textSecondary
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (testStatus == ConnectionStatus.CONNECTED) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = testMessage!!,
                            style = NexusTypography.bodySmall.copy(color = statusColor),
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Test Connection button
            OutlinedButton(
                onClick = { testConnectionNow() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                enabled = !isTesting
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colors.accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Test Connection",
                        style = NexusTypography.labelMedium.copy(color = colors.textPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Continue Button
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: 1234
                    val timeout = timeoutText.toIntOrNull() ?: 60
                    val updatedConfig = currentConfig.copy(
                        ipAddress = ipAddress.trim(),
                        port = port,
                        useHttps = useHttps,
                        timeoutSeconds = timeout
                    )
                    scope.launch {
                        repository.updateServerConfig(updatedConfig)
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text(
                    text = "Continue",
                    style = NexusTypography.labelLarge.copy(color = colors.surfaceElevated)
                )
            }
        }
    }
}
