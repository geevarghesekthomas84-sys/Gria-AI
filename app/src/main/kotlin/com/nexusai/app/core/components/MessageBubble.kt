package com.nexusai.app.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusai.app.core.theme.LocalNexusColors
import com.nexusai.app.core.theme.NexusTypography
import com.nexusai.app.core.utils.NexusHaptic
import com.nexusai.app.domain.model.Message
import com.nexusai.app.domain.model.MessageRole
import com.nexusai.app.domain.model.MessageStatus
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    isStreaming: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNexusColors.current
    val isUser = message.role == MessageRole.USER
    val isAssistant = message.role == MessageRole.ASSISTANT
    var showActions by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isUser) 16.dp else 20.dp,
                vertical = if (isUser) 6.dp else 12.dp
            ),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // User Message: Compact, lightly tinted rounded bubble
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.userBubble,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.userBubbleBorder),
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clickable { showActions = !showActions }
            ) {
                Text(
                    text = message.content,
                    style = NexusTypography.bodyLarge.copy(color = colors.textPrimary),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        } else {
            // Assistant Message: Appears directly on the background with generous margins
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showActions = !showActions }
            ) {
                if (message.content.isEmpty() && isStreaming && message.status == MessageStatus.STREAMING) {
                    AiRespondingIndicator(
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                    )
                } else {
                    MarkdownViewer(
                        content = message.content,
                        textColor = colors.textPrimary,
                        isStreaming = isStreaming && message.status == MessageStatus.STREAMING
                    )
                }

                if (message.status == MessageStatus.ERROR && message.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.errorMessage,
                        color = colors.statusError,
                        style = NexusTypography.bodySmall
                    )
                }

                // Inline discreet action bar below assistant response (only when completed & has content)
                if ((!isStreaming || message.status != MessageStatus.STREAMING) && message.content.isNotEmpty()) {
                    val view = LocalView.current
                    val telemetryText = remember(message) {
                        val tokPerSec = message.tokensPerSecond
                        val tokens = message.tokensCount
                        val durationSec = message.generationDurationMs?.let { it / 1000f }
                        when {
                            tokPerSec != null && tokens != null && durationSec != null ->
                                String.format(Locale.US, "%.1f tok/s · %d tokens · %.1fs", tokPerSec, tokens, durationSec)
                            tokPerSec != null && tokens != null ->
                                String.format(Locale.US, "%.1f tok/s · %d tokens", tokPerSec, tokens)
                            tokens != null ->
                                "$tokens tokens"
                            else -> null
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                NexusHaptic.confirm(view)
                                onCopy()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy response",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                NexusHaptic.tick(view)
                                onRegenerate()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate response",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                NexusHaptic.click(view)
                                onShare()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        if (showActions) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    NexusHaptic.click(view)
                                    onDelete()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = colors.statusError,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Subtle right-aligned generation telemetry
                        if (telemetryText != null) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = telemetryText,
                                style = NexusTypography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = colors.textTertiary
                                ),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // User edit action on tap
        if (isUser && showActions) {
            val view = LocalView.current
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { NexusHaptic.confirm(view); onCopy() }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.ContentCopy, "Copy", tint = colors.textTertiary, modifier = Modifier.size(13.dp))
                }
                IconButton(onClick = { NexusHaptic.click(view); onEdit() }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Edit, "Edit", tint = colors.textTertiary, modifier = Modifier.size(13.dp))
                }
                IconButton(onClick = { NexusHaptic.click(view); onDelete() }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.DeleteOutline, "Delete", tint = colors.statusError, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}
