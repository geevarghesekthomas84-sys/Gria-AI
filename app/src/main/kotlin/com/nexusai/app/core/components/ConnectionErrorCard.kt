package com.nexusai.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusai.app.core.theme.LocalNexusColors
import com.nexusai.app.core.theme.NexusTypography

@Composable
fun ConnectionErrorCard(
    errorMessage: String,
    onRetry: () -> Unit,
    onEditSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNexusColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Soft muted status icon: small circular outline with subtle exclamation
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.accentSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Connection Status",
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Cannot Reach LM Studio",
                style = NexusTypography.headlineMedium.copy(color = colors.textPrimary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (errorMessage.isNotBlank()) errorMessage else "Unable to reach your laptop. Ensure LM Studio is running with local server started.",
                style = NexusTypography.bodySmall.copy(color = colors.textSecondary),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step-by-step calm checklist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .padding(12.dp)
            ) {
                Text(
                    text = "1. Open LM Studio on your computer",
                    style = NexusTypography.bodySmall.copy(color = colors.textSecondary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "2. Navigate to Developer / Local Server tab (<->)",
                    style = NexusTypography.bodySmall.copy(color = colors.textSecondary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "3. Confirm server is started on port 1234",
                    style = NexusTypography.bodySmall.copy(color = colors.textSecondary)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEditSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Text(
                        text = "Edit Settings",
                        style = NexusTypography.labelMedium.copy(color = colors.textPrimary)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = colors.surfaceElevated
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Retry",
                        style = NexusTypography.labelMedium.copy(color = colors.surfaceElevated)
                    )
                }
            }
        }
    }
}
