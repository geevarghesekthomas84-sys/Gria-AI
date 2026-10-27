package com.griaai.app.feature.about

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.griaai.app.core.components.GriaLogo
import com.griaai.app.core.theme.LocalGriaColors
import com.griaai.app.core.theme.GriaTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGriaColors.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val safeOpenUrl = { url: String ->
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
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
                        Text(
                            text = "About",
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // App Logo
            GriaLogo(
                size = 76.dp,
                showContainer = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GRIA-AI",
                style = GriaTypography.headlineLarge.copy(
                    color = colors.textPrimary,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Personal local AI assistant powered by LM Studio",
                style = GriaTypography.bodySmall.copy(color = colors.textSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Creator Section: Built by GG
            Text(
                text = "CREATOR & DEVELOPMENT",
                style = GriaTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.accent.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "GG",
                                    style = GriaTypography.labelLarge.copy(
                                        color = colors.accent,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Built by GG",
                                style = GriaTypography.bodyMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Geevarghese K Thomas",
                                style = GriaTypography.bodySmall.copy(color = colors.textSecondary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // GitHub Link Row
                    SocialLinkRow(
                        title = "GitHub",
                        subtitle = "geevarghesekthomas84-sys",
                        icon = Icons.Default.Code,
                        onClick = { safeOpenUrl("https://github.com/geevarghesekthomas84-sys") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // LinkedIn Link Row
                    SocialLinkRow(
                        title = "LinkedIn",
                        subtitle = "geevarghese-k-thomas",
                        icon = Icons.Default.Person,
                        onClick = { safeOpenUrl("https://www.linkedin.com/in/geevarghese-k-thomas") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Buy Me a Coffee Link Row
                    SocialLinkRow(
                        title = "Buy Me a Coffee",
                        subtitle = "buymeacoffee.com/geevarghes2",
                        icon = Icons.Default.Coffee,
                        onClick = { safeOpenUrl("https://buymeacoffee.com/geevarghes2") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Architecture & Privacy Details
            Text(
                text = "CORE PILLARS",
                style = GriaTypography.labelSmall.copy(color = colors.textTertiary, letterSpacing = 0.5.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Private by Design",
                            style = GriaTypography.bodyMedium.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your prompts, responses, and conversation histories reside strictly within your local environment. Zero telemetry or third-party cloud analytics.",
                        style = GriaTypography.bodySmall.copy(color = colors.textSecondary),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Direct Local Inference",
                            style = GriaTypography.bodyMedium.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Connects via HTTP Server-Sent Events directly to your LM Studio instance on your Wi-Fi or local IP for high throughput and minimum latency.",
                        style = GriaTypography.bodySmall.copy(color = colors.textSecondary),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Crafted with Jetpack Compose & Google Stitch\nQuiet Luxury · Modern Minimalism",
                style = GriaTypography.labelSmall.copy(color = colors.textTertiary),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SocialLinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = LocalGriaColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = GriaTypography.bodyMedium.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = subtitle,
                    style = GriaTypography.bodySmall.copy(color = colors.textSecondary)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(12.dp)
        )
    }
}
