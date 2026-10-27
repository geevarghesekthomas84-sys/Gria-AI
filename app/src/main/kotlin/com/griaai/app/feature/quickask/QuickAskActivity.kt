package com.griaai.app.feature.quickask

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.griaai.app.GriaApplication
import com.griaai.app.core.components.MarkdownViewer
import com.griaai.app.core.theme.GriaAITheme
import com.griaai.app.core.theme.LocalGriaColors
import com.griaai.app.core.theme.GriaTypography
import com.griaai.app.core.utils.GriaHaptic
import com.griaai.app.domain.model.Message
import com.griaai.app.domain.model.MessageRole
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class QuickAskActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            ?: intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: ""

        setContent {
            GriaAITheme {
                QuickAskBottomSheet(
                    initialText = selectedText,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAskBottomSheet(
    initialText: String,
    onDismiss: () -> Unit
) {
    val colors = LocalGriaColors.current
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var responseText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var activeAction by remember { mutableStateOf("Explain") }
    var streamingJob by remember { mutableStateOf<Job?>(null) }

    val onDeviceEngine = remember { GriaApplication.instance.onDeviceEngine }

    val runInference: (String) -> Unit = { action ->
        activeAction = action
        streamingJob?.cancel()
        isGenerating = true
        responseText = ""

        val prompt = when (action) {
            "Explain" -> "Explain the following text clearly and concisely:\n\n$initialText"
            "Summarize" -> "Summarize the key points of the following text in 3 bullet points:\n\n$initialText"
            "Fix / Improve" -> "Correct any grammar or spelling mistakes and improve clarity of this text:\n\n$initialText"
            "Translate" -> "Translate this text into clear, fluent English (or if already in English, translate to Spanish):\n\n$initialText"
            else -> "$action:\n\n$initialText"
        }

        streamingJob = scope.launch {
            if (onDeviceEngine.isModelLoaded) {
                val gemmaPrompt = onDeviceEngine.formatGemmaPrompt(
                    systemPrompt = "You are Gria-AI, a helpful, precise on-device assistant. Answer directly without fluff.",
                    messages = listOf(
                        Message(
                            id = java.util.UUID.randomUUID().toString(),
                            conversationId = "quick",
                            role = MessageRole.USER,
                            content = prompt
                        )
                    )
                )
                onDeviceEngine.generateStream(gemmaPrompt).collect { chunk ->
                    responseText += chunk
                }
            } else {
                responseText = "⚡ On-Device Gemma 2B is not loaded.\n\nPlease open Gria-AI to load your model or connect to LM Studio."
            }
            isGenerating = false
        }
    }

    LaunchedEffect(Unit) {
        if (initialText.isNotBlank()) {
            runInference("Explain")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceElevated,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ask Gria",
                    style = GriaTypography.headlineSmall.copy(color = colors.textPrimary),
                    modifier = Modifier.weight(1f)
                )

                if (isGenerating) {
                    IconButton(onClick = {
                        onDeviceEngine.stop()
                        streamingJob?.cancel()
                        isGenerating = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (responseText.isNotBlank()) {
                    IconButton(onClick = {
                        GriaHaptic.confirm(view)
                        clipboardManager.setText(AnnotatedString(responseText))
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Highlighted selection snippet
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceSubtle,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (initialText.length > 140) initialText.take(140) + "..." else initialText,
                    style = GriaTypography.bodySmall.copy(color = colors.textSecondary),
                    modifier = Modifier.padding(10.dp),
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Explain", "Summarize", "Fix / Improve", "Translate").forEach { action ->
                    val isSelected = action == activeAction
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) colors.surfaceSubtle else colors.background,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) colors.accent else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                GriaHaptic.click(view)
                                runInference(action)
                            }
                    ) {
                        Text(
                            text = action,
                            style = GriaTypography.labelSmall.copy(
                                color = if (isSelected) colors.accent else colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // AI Response Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 340.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (responseText.isEmpty() && isGenerating) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = colors.accent,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    MarkdownViewer(
                        content = responseText,
                        textColor = colors.textPrimary,
                        isStreaming = isGenerating
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

