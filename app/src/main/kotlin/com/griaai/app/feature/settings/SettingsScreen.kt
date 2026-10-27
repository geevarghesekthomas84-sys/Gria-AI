package com.griaai.app.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import com.griaai.app.core.theme.LocalGriaColors
import com.griaai.app.core.theme.GriaTypography
import com.griaai.app.domain.model.ServerConfig
import com.griaai.app.domain.repository.ChatRepository
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
    val colors = LocalGriaColors.current
    val currentConfig by repository.serverConfigFlow.collectAsState(initial = ServerConfig())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var temperature by remember(currentConfig) { mutableFloatStateOf(currentConfig.temperature) }
    var maxTokens by remember(currentConfig) { mutableIntStateOf(currentConfig.maxTokens) }
    var maxContext by remember(currentConfig) { mutableIntStateOf(currentConfig.maxContextMessages) }
    var systemPrompt by remember(currentConfig) {
        val prompt = currentConfig.systemPrompt
        val sanitized = if (prompt.contains("Nexus", ignoreCase = true)) {
            prompt.replace("Nexus AI", "Gria-AI", ignoreCase = true)
                .replace("nexus ai", "Gria-AI", ignoreCase = true)
                .replace("Nexus", "Gria-AI", ignoreCase = true)
        } else {
            prompt
        }
        mutableStateOf(sanitized)
    }
    var isDarkTheme by remember(currentConfig) { mutableStateOf(currentConfig.isDarkTheme) }
    val isBiometricEnabled by repository.isBiometricEnabled.collectAsState(initial = false)

    var isModelLoaded by remember { mutableStateOf(repository.onDeviceEngine.isModelLoaded) }
    var loadedModelName by remember { mutableStateOf(repository.onDeviceEngine.loadedModelName) }
    var isImportingModel by remember { mutableStateOf(false) }
    var importProgress by remember { mutableFloatStateOf(0f) }
    var discoveredModels by remember { mutableStateOf(emptyList<File>()) }
    var hasAllFilesAccess by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else true
        )
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val list = repository.onDeviceEngine.discoverAllAvailableModels()
            discoveredModels = list
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = run {
                var name = "gemma-model.bin"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIdx)
                    }
                }
                name
            }

            scope.launch {
                isImportingModel = true
                importProgress = 0f
                Toast.makeText(context, "Importing $fileName into GPU memory...", Toast.LENGTH_SHORT).show()
                val result = repository.onDeviceEngine.loadModelFromUri(uri, fileName) { progress ->
                    importProgress = progress
                }
                isImportingModel = false
                if (result.isSuccess) {
                    isModelLoaded = repository.onDeviceEngine.isModelLoaded
                    loadedModelName = repository.onDeviceEngine.loadedModelName
                    repository.setDefaultModel("ondevice:gemma")
                    Toast.makeText(context, "Successfully loaded $fileName onto Adreno GPU!", Toast.LENGTH_LONG).show()
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to load model"
                    Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

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
                            style = GriaTypography.titleMedium.copy(color = colors.textPrimary),
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
                style = GriaTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
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
                            style = GriaTypography.bodyMedium.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentConfig.baseUrl,
                            style = GriaTypography.bodySmall.copy(color = colors.textSecondary)
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

            // Group: On-Device Local AI (Gemma)
            Text(
                text = "ON-DEVICE LOCAL AI (GEMMA)",
                style = GriaTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Snapdragon Adreno GPU",
                                style = GriaTypography.bodyMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = if (isModelLoaded) "Model: ${loadedModelName ?: "Gemma"}" else "No model loaded · Tap below to select",
                                style = GriaTypography.bodySmall.copy(color = if (isModelLoaded) colors.accent else colors.textSecondary)
                            )
                        }

                        if (isModelLoaded) {
                            TextButton(
                                onClick = {
                                    repository.onDeviceEngine.close()
                                    isModelLoaded = repository.onDeviceEngine.isModelLoaded
                                    loadedModelName = null
                                    Toast.makeText(context, "On-device model unloaded", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Unload", color = colors.statusError, style = GriaTypography.labelSmall)
                            }
                        }
                    }

                    if (isImportingModel) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { importProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = colors.accent,
                            trackColor = colors.surfaceSubtle
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Importing model: ${(importProgress * 100).toInt()}%",
                            style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Select Model",
                                style = GriaTypography.labelSmall.copy(color = colors.surfaceElevated)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    hasAllFilesAccess = Environment.isExternalStorageManager()
                                }
                                Toast.makeText(context, "Scanning local storage for models...", Toast.LENGTH_SHORT).show()
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val found = repository.onDeviceEngine.tryAutoLoadAvailableModel()
                                    val list = repository.onDeviceEngine.discoverAllAvailableModels()
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        isModelLoaded = repository.onDeviceEngine.isModelLoaded
                                        loadedModelName = repository.onDeviceEngine.loadedModelName
                                        discoveredModels = list
                                        if (found) {
                                            scope.launch {
                                                repository.setDefaultModel("ondevice:gemma")
                                            }
                                            Toast.makeText(context, "Loaded $loadedModelName onto Adreno GPU!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No Gemma model found in /sdcard/models or Downloads", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle)
                        ) {
                            Text(
                                text = "Auto-Scan",
                                style = GriaTypography.labelSmall.copy(color = colors.textPrimary)
                            )
                        }
                    }

                    if (!hasAllFilesAccess) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.accent.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        try {
                                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                            context.startActivity(intent)
                                        }
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tap to grant All Files Access to read /sdcard/models",
                                style = GriaTypography.labelSmall.copy(color = colors.accent)
                            )
                        }
                    }

                    if (discoveredModels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AVAILABLE LOCAL MODELS (${discoveredModels.size})",
                            style = GriaTypography.labelSmall.copy(
                                color = colors.textTertiary,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        discoveredModels.forEach { modelFile ->
                            val isCurrent = modelFile.name == loadedModelName
                            val isGpu = modelFile.name.contains("gpu", ignoreCase = true)
                            val fileSizeGb = String.format("%.2f", modelFile.length() / (1024.0 * 1024 * 1024))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) colors.accent.copy(alpha = 0.12f) else colors.surfaceSubtle,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (isCurrent) colors.accent else colors.borderSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        scope.launch {
                                            Toast.makeText(context, "Loading ${modelFile.name}...", Toast.LENGTH_SHORT).show()
                                            val res = repository.onDeviceEngine.loadModel(modelFile)
                                            if (res.isSuccess) {
                                                isModelLoaded = true
                                                loadedModelName = modelFile.name
                                                repository.setDefaultModel("ondevice:gemma")
                                                Toast.makeText(context, "Loaded ${modelFile.name} successfully!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val msg = res.exceptionOrNull()?.localizedMessage ?: "Failed to initialize"
                                                Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = modelFile.name,
                                            style = GriaTypography.bodySmall.copy(
                                                color = colors.textPrimary,
                                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$fileSizeGb GB",
                                                style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isGpu) "⚡ GPU Accelerated" else "CPU",
                                                style = GriaTypography.labelSmall.copy(
                                                    color = if (isGpu) colors.accent else colors.textTertiary
                                                )
                                            )
                                        }
                                    }

                                    if (isCurrent) {
                                        Text(
                                            text = "ACTIVE",
                                            style = GriaTypography.labelSmall.copy(
                                                color = colors.accent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group 2: AI Behavior & Parameters

            Text(
                text = "AI BEHAVIOR",
                style = GriaTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
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
                            style = GriaTypography.bodyMedium.copy(color = colors.textPrimary),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%.2f", temperature),
                            style = GriaTypography.labelMedium.copy(color = colors.accent)
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
                            style = GriaTypography.bodyMedium.copy(color = colors.textPrimary),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$maxTokens tokens",
                            style = GriaTypography.labelMedium.copy(color = colors.accent)
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
                            style = GriaTypography.bodyMedium.copy(color = colors.textPrimary),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$maxContext messages",
                            style = GriaTypography.labelMedium.copy(color = colors.accent)
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
                style = GriaTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Persona or Behavior",
                            style = GriaTypography.bodySmall.copy(color = colors.textSecondary)
                        )
                        Text(
                            text = "Reset to Gria-AI",
                            style = GriaTypography.labelSmall.copy(
                                color = colors.accent,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    systemPrompt = "You are Gria-AI, a helpful, precise, and thoughtful on-device & local AI assistant."
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6,
                        placeholder = {
                            Text(
                                text = "You are Gria-AI, a helpful, precise, and thoughtful on-device & local AI assistant.",
                                style = GriaTypography.bodySmall.copy(color = colors.textTertiary)
                            )
                        },
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

                    Text(
                        text = "QUICK PERSONA PRESETS",
                        style = GriaTypography.labelSmall.copy(color = colors.textTertiary, fontSize = 10.sp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            "Gria Default" to "You are Gria-AI, a helpful, precise, and thoughtful on-device & local AI assistant.",
                            "Coder" to "You are Gria-AI acting as an expert senior software engineer. Provide clean, idiomatic, and highly efficient code. Avoid conversational filler and explain architectural trade-offs directly.",
                            "Security" to "You are Gria-AI acting as an elite offensive security researcher, red team operator, and exploit analyst. Provide direct, rigorous technical vulnerability analysis, payload mechanics, and mitigation strategies.",
                            "Concise" to "You are Gria-AI acting as an executive synthesizer. Give concise, direct answers with bullet points and zero conversational filler."
                        )
                        presets.forEach { (name, promptText) ->
                            val isCurrent = systemPrompt.trim() == promptText.trim()
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) colors.accent.copy(alpha = 0.15f) else colors.surfaceSubtle,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (isCurrent) colors.accent else colors.borderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { systemPrompt = promptText }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        style = GriaTypography.labelSmall.copy(
                                            color = if (isCurrent) colors.accent else colors.textSecondary,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group 4: Appearance
            Text(
                text = "APPEARANCE",
                style = GriaTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
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
                            style = GriaTypography.bodyMedium.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "Toggle between Warm White and Deep Graphite",
                            style = GriaTypography.bodySmall.copy(color = colors.textTertiary)
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

            Spacer(modifier = Modifier.height(10.dp))

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
                            text = "Biometric Vault Lock",
                            style = GriaTypography.bodyMedium.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "Require fingerprint or screen lock to open Gria-AI",
                            style = GriaTypography.bodySmall.copy(color = colors.textTertiary)
                        )
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = {
                            scope.launch {
                                repository.setBiometricEnabled(it)
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
                style = GriaTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
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
                                style = GriaTypography.bodyMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "100% private. All chats stored locally in SQLite.",
                                style = GriaTypography.bodySmall.copy(color = colors.textSecondary)
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
                                style = GriaTypography.bodyMedium.copy(
                                    color = colors.statusError,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Permanently removes all saved conversations",
                                style = GriaTypography.bodySmall.copy(color = colors.textTertiary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Group 6: About & Intro
            Text(
                text = "ABOUT & INTRO",
                style = GriaTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
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
                                style = GriaTypography.bodyMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "View architecture highlights & feature overview",
                                style = GriaTypography.bodySmall.copy(color = colors.textSecondary)
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
                                text = "About GRIA-AI",
                                style = GriaTypography.bodyMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Built by GG · GitHub, LinkedIn & Support",
                                style = GriaTypography.bodySmall.copy(color = colors.textSecondary)
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
                    style = GriaTypography.labelLarge.copy(color = colors.surfaceElevated)
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
                    style = GriaTypography.titleMedium.copy(color = colors.textPrimary)
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all conversation history from your device. This cannot be undone.",
                    style = GriaTypography.bodyMedium.copy(color = colors.textSecondary)
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
                    Text("Delete", color = colors.statusError, style = GriaTypography.labelMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = colors.textPrimary, style = GriaTypography.labelMedium)
                }
            },
            containerColor = colors.surfaceElevated,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary
        )
    }
}
