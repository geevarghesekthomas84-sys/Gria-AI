package com.griaai.app.core.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.griaai.app.core.theme.LocalGriaColors
import com.griaai.app.core.theme.GriaTypography
import com.griaai.app.core.utils.GriaHaptic
import com.griaai.app.domain.model.ConnectionStatus

@Composable
fun MessageComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isStreaming: Boolean,
    onStartVoiceLive: (() -> Unit)? = null,
    selectedImageUri: String? = null,
    onSelectImage: (String?) -> Unit = {},
    onClearImage: () -> Unit = {},
    onEnhancePrompt: () -> Unit = {},
    modifier: Modifier = Modifier,
    placeholder: String = "Message Gria-AI..."
) {
    val colors = LocalGriaColors.current
    val context = LocalContext.current
    val view = LocalView.current
    val canSend = text.isNotBlank() || selectedImageUri != null

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            GriaHaptic.confirm(view)
            onSelectImage(uri.toString())
        }
    }

    var isListening by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                val updated = if (text.isBlank()) spoken else "$text $spoken"
                onTextChange(updated)
                GriaHaptic.confirm(view)
            }
        }
    }

    val startVoiceRecognition = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gria-AI...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            isListening = true
            GriaHaptic.click(view)
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(context, "Voice recognition not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleVoice = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startVoiceRecognition()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(22.dp),
        color = colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp
    ) {
        Column {
            // Multimodal image attachment thumbnail preview
            if (selectedImageUri != null) {
                val bitmap = remember(selectedImageUri) {
                    try {
                        val uri = Uri.parse(selectedImageUri)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 64.dp, height = 64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Attached image preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Close / Remove image button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated.copy(alpha = 0.9f))
                                .clickable {
                                    GriaHaptic.tick(view)
                                    onClearImage()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove attached image",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = if (isListening) "Listening..." else placeholder,
                            style = GriaTypography.bodyLarge,
                            color = if (isListening) colors.accent else colors.textTertiary
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 22.dp, max = 130.dp)
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.key == Key.Enter && !keyEvent.isShiftPressed && canSend && !isStreaming) {
                                    GriaHaptic.confirm(view)
                                    onSend()
                                    true
                                } else {
                                    false
                                }
                            },
                        textStyle = GriaTypography.bodyLarge.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.accent),
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (canSend && !isStreaming) {
                                    GriaHaptic.confirm(view)
                                    onSend()
                                }
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Attach Image Button (Multimodal input)
                if (!isStreaming) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (selectedImageUri != null) colors.accent.copy(alpha = 0.15f) else colors.surfaceSubtle)
                            .clickable {
                                GriaHaptic.click(view)
                                photoPickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach image",
                            tint = if (selectedImageUri != null) colors.accent else colors.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Prompt "Magic Wand" Auto-Enhancer
                if (text.isNotBlank() && !isStreaming) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.12f))
                            .clickable {
                                GriaHaptic.confirm(view)
                                onEnhancePrompt()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Enhance prompt",
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Speech-to-text on-device voice button
            if (!isStreaming) {
                val micTransition = rememberInfiniteTransition(label = "mic_pulse")
                val micScale by micTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isListening) 1.22f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 650, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "mic_pulse_scale"
                )
                val micHaloAlpha by micTransition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = if (isListening) 0.05f else 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 650, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "mic_halo_alpha"
                )

                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .scale(micScale)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = micHaloAlpha))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isListening) colors.accent else colors.surfaceSubtle)
                            .clickable {
                                if (onStartVoiceLive != null) {
                                    GriaHaptic.confirm(view)
                                    onStartVoiceLive()
                                } else {
                                    toggleVoice()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "Voice input",
                            tint = if (isListening) colors.surfaceElevated else colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))
            }

            // Action Button: Minimal circle with upward arrow (Apple / ChatGPT style)
            if (isStreaming) {
                val infiniteTransition = rememberInfiniteTransition(label = "composer_stop_pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 950, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "stop_pulse_scale"
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 0.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 950, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "stop_pulse_alpha"
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            GriaHaptic.tick(view)
                            onStop()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Soft breathing halo ring
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = pulseAlpha))
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop generation",
                            tint = colors.surfaceElevated,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            } else {
                val buttonBg = if (canSend) colors.accent else colors.surfaceSubtle
                val iconTint = if (canSend) colors.surfaceElevated else colors.textTertiary

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .clickable(
                            enabled = canSend,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                GriaHaptic.tick(view)
                                onSend()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Send message",
                        tint = iconTint,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
}

@Composable
fun ConnectionBadge(
    status: ConnectionStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGriaColors.current
    val dotColor = when (status) {
        ConnectionStatus.CONNECTED -> colors.statusSuccess
        ConnectionStatus.CONNECTING -> colors.textTertiary
        ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> colors.statusError
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}
