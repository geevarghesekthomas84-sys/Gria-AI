package com.nexusai.app.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusai.app.core.theme.LocalNexusColors
import com.nexusai.app.core.theme.NexusTypography
import com.nexusai.app.domain.model.ServerConfig
import com.nexusai.app.domain.repository.ChatRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: ChatRepository,
    onNavigateBack: () -> Unit,
    onNavigateToConnection: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToIntro: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalNexusColors.current
    val currentConfig by repository.serverConfigFlow.collectAsState(initial = ServerConfig())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var temperature by remember(currentConfig) { mutableFloatStateOf(currentConfig.temperature) }
    var maxTokens by remember(currentConfig) { mutableIntStateOf(currentConfig.maxTokens) }
    var maxContext by remember(currentConfig) { mutableIntStateOf(currentConfig.maxContextMessages) }
    var systemPrompt by remember(currentConfig) { mutableStateOf(currentConfig.systemPrompt) }
    var isDarkTheme by remember(currentConfig) { mutableStateOf(currentConfig.isDarkTheme) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                        IconButton(onClick = {
                            scope.launch {
                                repository.updateServerConfig(
                                    currentConfig.copy(
                                        temperature = temperature,
                                        maxTokens = maxTokens,
                                        maxContextMessages = maxContext,
                                        systemPrompt = systemPrompt,
                                        isDarkTheme = isDarkTheme
                                    )
                                )
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary)
                        }
                        Text(
                            text = "Settings",
                            style = NexusTypography.titleMedium.copy(color = colors.textPrimary),
                            modifier = Modifier.weight(1f)
                        )
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
                .padding(20.dp)
        ) {
            // Group 1: Server Connection
            Text(
                text = "SERVER CONNECTION",
                style = NexusTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToConnection)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LM Studio Server",
                            style = NexusTypography.bodyMedium.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentConfig.baseUrl,
                            style = NexusTypography.bodySmall.copy(color = colors.textSecondary)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group 2: AI Behavior & Parameters
            Text(
                text = "AI BEHAVIOR",
                style = NexusTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Temperature
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Temperature",
                            style = NexusTypography.bodyMedium.copy(color = colors.textPrimary),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%.2f", temperature),
                            style = NexusTypography.labelMedium.copy(color = colors.accent)
                        )
                    }
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.surfaceSubtle
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Response Length
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Response Length",
                            style = NexusTypography.bodyMedium.copy(color = colors.textPrimary),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$maxTokens tokens",
                            style = NexusTypography.labelMedium.copy(color = colors.accent)
                        )
                    }
                    Slider(
                        value = maxTokens.toFloat(),
                        onValueChange = { maxTokens = it.toInt() },
                        valueRange = 256f..8192f,
                        steps = 30,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.surfaceSubtle
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Context Window
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Context Memory",
                            style = NexusTypography.bodyMedium.copy(color = colors.textPrimary),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$maxContext messages",
                            style = NexusTypography.labelMedium.copy(color = colors.accent)
                        )
                    }
                    Slider(
                        value = maxContext.toFloat(),
                        onValueChange = { maxContext = it.toInt() },
                        valueRange = 4f..60f,
                        steps = 27,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.surfaceSubtle
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group 3: System Instructions
            Text(
                text = "SYSTEM INSTRUCTIONS",
                style = NexusTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Custom Persona or Behavior",
                        style = NexusTypography.bodySmall.copy(color = colors.textSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
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

            Spacer(modifier = Modifier.height(24.dp))

            // Group 4: Appearance
            Text(
                text = "APPEARANCE",
                style = NexusTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dark Graphite Theme",
                            style = NexusTypography.bodyMedium.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "Toggle between Warm White and Deep Graphite",
                            style = NexusTypography.bodySmall.copy(color = colors.textTertiary)
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { 
                            isDarkTheme = it
                            scope.launch {
                                repository.setDarkTheme(it)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.surfaceElevated,
                            checkedTrackColor = colors.accent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group 5: Privacy & Data
            Text(
                text = "PRIVACY & STORAGE",
                style = NexusTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Local-Only Guarantee",
                                style = NexusTypography.bodyMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "100% private. All chats stored locally in SQLite.",
                                style = NexusTypography.bodySmall.copy(color = colors.textSecondary)
                            )
                        }
                    }

                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeleteConfirmDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = colors.statusError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clear Chat History",
                                style = NexusTypography.bodyMedium.copy(
                                    color = colors.statusError,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Permanently removes all saved conversations",
                                style = NexusTypography.bodySmall.copy(color = colors.textTertiary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group 6: About & Intro
            Text(
                text = "ABOUT & INTRO",
                style = NexusTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToIntro)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Application Intro",
                                style = NexusTypography.bodyMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "View architecture highlights & feature overview",
                                style = NexusTypography.bodySmall.copy(color = colors.textSecondary)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToAbout)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "About NEXUS AI",
                                style = NexusTypography.bodyMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Built by GG · GitHub, LinkedIn & Support",
                                style = NexusTypography.bodySmall.copy(color = colors.textSecondary)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Settings Button
            Button(
                onClick = {
                    val updated = currentConfig.copy(
                        temperature = temperature,
                        maxTokens = maxTokens,
                        maxContextMessages = maxContext,
                        systemPrompt = systemPrompt.trim(),
                        isDarkTheme = isDarkTheme
                    )
                    scope.launch {
                        repository.updateServerConfig(updated)
                        Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text(
                    text = "Save Settings",
                    style = NexusTypography.labelLarge.copy(color = colors.surfaceElevated)
                )
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Clear All Conversations?",
                    style = NexusTypography.titleMedium.copy(color = colors.textPrimary)
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all conversation history from your device. This cannot be undone.",
                    style = NexusTypography.bodyMedium.copy(color = colors.textSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteAllConversations()
                            showDeleteConfirmDialog = false
                            Toast.makeText(context, "All conversations cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Delete", color = colors.statusError, style = NexusTypography.labelMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = colors.textPrimary, style = NexusTypography.labelMedium)
                }
            },
            containerColor = colors.surfaceElevated,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary
        )
    }
}
