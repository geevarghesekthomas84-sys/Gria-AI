package com.nexusai.app.core.components

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexusai.app.NexusApplication
import com.nexusai.app.core.theme.LocalNexusColors
import com.nexusai.app.core.utils.NexusHaptic
import com.nexusai.app.core.theme.NexusTypography
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun NexusVoiceLiveDialog(
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit,
    lastAssistantResponse: String?,
    isAssistantStreaming: Boolean
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = LocalNexusColors.current
    val voiceEngine = remember { NexusApplication.instance.voiceEngine }
    val isSpeaking by voiceEngine.isSpeaking.collectAsState()

    var voiceState by remember { mutableStateOf(VoiceState.IDLE) }
    var userSpeechTranscript by remember { mutableStateOf("") }
    var assistantSpokenText by remember { mutableStateOf("") }
    var lastHandledResponse by remember { mutableStateOf("") }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Start offline voice recognition
    val startListening = {
        try {
            voiceEngine.stop()
            userSpeechTranscript = ""
            voiceState = VoiceState.LISTENING

            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    voiceState = VoiceState.THINKING
                }

                override fun onError(error: Int) {
                    if (voiceState == VoiceState.LISTENING) {
                        voiceState = VoiceState.IDLE
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrBlank()) {
                        userSpeechTranscript = text
                        voiceState = VoiceState.THINKING
                        NexusHaptic.confirm(view)
                        onSendMessage(text)
                    } else {
                        voiceState = VoiceState.IDLE
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        userSpeechTranscript = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            voiceState = VoiceState.IDLE
            Toast.makeText(context, "Voice recognition unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-listen when dialog opens
    LaunchedEffect(Unit) {
        startListening()
    }

    // React to assistant generation completion -> speak response aloud
    LaunchedEffect(lastAssistantResponse, isAssistantStreaming) {
        if (!isAssistantStreaming && !lastAssistantResponse.isNullOrBlank() && lastAssistantResponse != lastHandledResponse) {
            lastHandledResponse = lastAssistantResponse
            assistantSpokenText = lastAssistantResponse
            voiceState = VoiceState.SPEAKING
            voiceEngine.speak("voice_live_reply", lastAssistantResponse)
        }
    }

    // When TTS finishes speaking -> automatically transition back to listening for seamless hands-free loop
    LaunchedEffect(isSpeaking, voiceState) {
        if (!isSpeaking && voiceState == VoiceState.SPEAKING) {
            voiceState = VoiceState.LISTENING
            startListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            voiceEngine.stop()
        }
    }

    // Animations for Glowing Animated Orb
    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (voiceState == VoiceState.SPEAKING) 800 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val orbRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    Dialog(
        onDismissRequest = {
            voiceEngine.stop()
            speechRecognizer?.destroy()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16))
                .padding(24.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (voiceState) {
                                    VoiceState.LISTENING -> colors.statusSuccess
                                    VoiceState.THINKING -> colors.accent
                                    VoiceState.SPEAKING -> Color(0xFF8B5CF6)
                                    VoiceState.IDLE -> colors.textTertiary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nexus Live Voice",
                        style = NexusTypography.titleMedium.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                IconButton(
                    onClick = {
                        voiceEngine.stop()
                        speechRecognizer?.destroy()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Voice Mode",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Central Animated Orb & Dynamic Transcript
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Outer Blur Glow
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .scale(pulseScale)
                            .blur(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = when (voiceState) {
                                        VoiceState.LISTENING -> listOf(Color(0xFF10B981).copy(alpha = 0.5f), Color.Transparent)
                                        VoiceState.THINKING -> listOf(Color(0xFF06B6D4).copy(alpha = 0.5f), Color.Transparent)
                                        VoiceState.SPEAKING -> listOf(Color(0xFF8B5CF6).copy(alpha = 0.6f), Color(0xFF06B6D4).copy(alpha = 0.3f), Color.Transparent)
                                        VoiceState.IDLE -> listOf(Color(0xFF334155).copy(alpha = 0.3f), Color.Transparent)
                                    }
                                )
                            )
                    )

                    // Core Interactive Orb
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .scale(if (voiceState == VoiceState.SPEAKING || voiceState == VoiceState.LISTENING) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = when (voiceState) {
                                        VoiceState.LISTENING -> listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF064E3B))
                                        VoiceState.THINKING -> listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                        VoiceState.SPEAKING -> listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4))
                                        VoiceState.IDLE -> listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    }
                                )
                            )
                            .border(
                                2.dp,
                                Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)
                                ),
                                CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                NexusHaptic.click(view)
                                if (voiceState == VoiceState.SPEAKING) {
                                    voiceEngine.stop()
                                    startListening()
                                } else if (voiceState == VoiceState.LISTENING) {
                                    speechRecognizer?.stopListening()
                                } else {
                                    startListening()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (voiceState) {
                                VoiceState.LISTENING -> Icons.Default.Mic
                                VoiceState.THINKING -> Icons.Default.Mic
                                VoiceState.SPEAKING -> Icons.Default.VolumeUp
                                VoiceState.IDLE -> Icons.Default.MicOff
                            },
                            contentDescription = "Voice State",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Status Label
                Text(
                    text = when (voiceState) {
                        VoiceState.LISTENING -> "Listening to you..."
                        VoiceState.THINKING -> "Thinking on-device..."
                        VoiceState.SPEAKING -> "Nexus speaking..."
                        VoiceState.IDLE -> "Tap orb to speak"
                    },
                    style = NexusTypography.titleMedium.copy(
                        color = when (voiceState) {
                            VoiceState.LISTENING -> colors.statusSuccess
                            VoiceState.THINKING -> colors.accent
                            VoiceState.SPEAKING -> Color(0xFFA78BFA)
                            VoiceState.IDLE -> colors.textSecondary
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Transcript Preview
                val displayText = when {
                    voiceState == VoiceState.LISTENING && userSpeechTranscript.isNotBlank() -> userSpeechTranscript
                    voiceState == VoiceState.SPEAKING && assistantSpokenText.isNotBlank() -> voiceEngine.cleanMarkdownForSpeech(assistantSpokenText).take(180) + "..."
                    voiceState == VoiceState.THINKING -> "Processing your request locally with Gemma..."
                    else -> "Hands-free two-way conversation ready"
                }

                Text(
                    text = displayText,
                    style = NexusTypography.bodyMedium.copy(
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    maxLines = 4
                )
            }

            // Bottom Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(24.dp))
                        .clickable {
                            NexusHaptic.click(view)
                            if (voiceState == VoiceState.SPEAKING) {
                                voiceEngine.stop()
                                startListening()
                            } else {
                                startListening()
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (voiceState == VoiceState.SPEAKING) "Tap to interrupt & speak" else "Tap to speak",
                        style = NexusTypography.labelMedium.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
