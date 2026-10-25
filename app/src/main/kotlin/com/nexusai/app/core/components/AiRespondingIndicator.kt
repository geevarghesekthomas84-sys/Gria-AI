package com.nexusai.app.core.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.nexusai.app.core.theme.LocalNexusColors
import com.nexusai.app.core.theme.NexusTypography
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * Premium Quiet Luxury AI Responding Indicator.
 *
 * Displays while the local AI model is processing/thinking before tokens arrive:
 * - Animated rotating & breathing Nexus Star
 * - Liquid-smooth undulating wave dots
 * - Soft shimmering "Nexus is thinking..." label
 */
@Composable
fun AiRespondingIndicator(
    modifier: Modifier = Modifier,
    label: String = "Nexus is thinking..."
) {
    val colors = LocalNexusColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "ai_responding_transition")

    // Continuous normalized cycle: 0f to 1f over 1400ms
    val cycleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cycle_progress"
    )

    // Breathing pulse for star emblem
    val starScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_scale"
    )

    val starRotation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_rotation"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Miniature Nexus 4-point diamond star
            NexusPulsingStar(
                color = colors.accent,
                scale = starScale,
                rotation = starRotation,
                modifier = Modifier.size(15.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 3 Liquid-smooth undulating dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..2) {
                    val phase = (cycleProgress + i * 0.22f) % 1f
                    val wave = max(0f, sin(phase * 2 * PI.toFloat()))
                    val yOffset = (-3.5f * wave).dp
                    val dotAlpha = 0.35f + 0.65f * wave
                    val dotScale = 0.85f + 0.25f * wave

                    Box(
                        modifier = Modifier
                            .offset(y = yOffset)
                            .scale(dotScale)
                            .alpha(dotAlpha)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Soft shimmering label
            val textAlpha = 0.65f + 0.35f * max(0f, sin(cycleProgress * 2 * PI.toFloat()))
            Text(
                text = label,
                style = NexusTypography.bodySmall.copy(
                    color = colors.textSecondary.copy(alpha = textAlpha)
                )
            )
        }
    }
}

/**
 * Minimalist 4-pointed Nexus Star canvas.
 */
@Composable
private fun NexusPulsingStar(
    color: Color,
    scale: Float,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .scale(scale)
            .rotate(rotation)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val path = Path().apply {
            moveTo(cx, 0f)
            cubicTo(cx * 1.15f, cy * 0.75f, cx * 1.25f, cy * 0.85f, w, cy)
            cubicTo(cx * 1.25f, cy * 1.15f, cx * 1.15f, cy * 1.25f, cx, h)
            cubicTo(cx * 0.85f, cy * 1.25f, cx * 0.75f, cy * 1.15f, 0f, cy)
            cubicTo(cx * 0.75f, cy * 0.85f, cx * 0.85f, cy * 0.75f, cx, 0f)
            close()
        }
        drawPath(path = path, color = color)
    }
}
