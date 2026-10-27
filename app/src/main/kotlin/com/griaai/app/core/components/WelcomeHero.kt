package com.griaai.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.griaai.app.core.theme.LocalGriaColors
import com.griaai.app.core.theme.GriaTypography
import java.util.Calendar

data class SuggestionRow(
    val label: String,
    val prompt: String
)

val DefaultSuggestionRows = listOf(
    SuggestionRow(
        label = "Explain how transformer attention caching works",
        prompt = "Explain how transformer attention caching (KV cache) optimizes autoregressive text generation."
    ),
    SuggestionRow(
        label = "Write an idiomatic Kotlin coroutine flow with backoff",
        prompt = "Write an idiomatic Kotlin coroutine flow implementation that demonstrates exponential retry backoff and error handling."
    ),
    SuggestionRow(
        label = "Draft a calm, professional advisory note for a client",
        prompt = "Draft a calm, polished, professional advisory email updating a client on project milestones."
    ),
    SuggestionRow(
        label = "Summarize the architectural trade-offs of local LLMs",
        prompt = "Summarize the latency, memory footprint, and privacy trade-offs of running local LLM inference versus cloud APIs."
    )
)

@Composable
fun WelcomeHero(
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier,
    modelName: String = "Qwen 2.5"
) {
    val colors = LocalGriaColors.current

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning. What's on your mind?"
            in 12..16 -> "Good afternoon. What's on your mind?"
            else -> "Good evening. What's on your mind?"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Elegant centered heading
        Text(
            text = greeting,
            style = GriaTypography.headlineLarge.copy(color = colors.textPrimary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Supporting text
        Text(
            text = "Ask, create, learn, or explore.",
            style = GriaTypography.bodyLarge.copy(color = colors.textSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Small suggestion prompts displayed as subtle text rows, not large colored cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceElevated)
        ) {
            DefaultSuggestionRows.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPrompt(item.prompt) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        style = GriaTypography.bodyMedium.copy(color = colors.textPrimary),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                }

                if (index < DefaultSuggestionRows.size - 1) {
                    HorizontalDivider(
                        color = colors.borderSubtle,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
