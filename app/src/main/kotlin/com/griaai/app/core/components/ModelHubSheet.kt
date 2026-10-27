package com.griaai.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.griaai.app.core.llm.ModelHubCatalog
import com.griaai.app.core.theme.GriaTypography
import com.griaai.app.core.theme.LocalGriaColors
import com.griaai.app.core.utils.GriaHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelHubSheet(
    onDismiss: () -> Unit,
    onModelDownloaded: () -> Unit = {}
) {
    val colors = LocalGriaColors.current
    val context = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Model Hub & Formats",
                        style = GriaTypography.headlineSmall.copy(color = colors.textPrimary)
                    )
                    Text(
                        text = "Supported formats: .bin, .task, .tflite, .litert",
                        style = GriaTypography.bodySmall.copy(color = colors.textSecondary)
                    )
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

            Spacer(modifier = Modifier.height(12.dp))

            // Architecture & Formats Info Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.accent.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "On-Device Neural Engine",
                            style = GriaTypography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Supports Google MediaPipe / LiteRT compiled models (.bin and .task bundles for Gemma 2B).\nFor Qwen 2.5, DeepSeek, Llama 3, or any GGUF models, use LM Studio Mode over local Wi-Fi.",
                            style = GriaTypography.bodySmall.copy(
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            ModelHubCatalog.AVAILABLE_MODELS.forEach { model ->
                val isDownloaded = model.isDownloaded(context)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceSubtle,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = model.name,
                                    style = GriaTypography.bodyMedium.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.accent.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = model.format,
                                        style = GriaTypography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.accent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.borderSubtle
                                ) {
                                    Text(
                                        text = model.sizeString,
                                        style = GriaTypography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.textTertiary,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = model.description,
                                style = GriaTypography.bodySmall.copy(color = colors.textSecondary),
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Target: ${model.accelerator} · ${model.fileName}",
                                style = GriaTypography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textTertiary,
                                    fontSize = 10.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        if (isDownloaded) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.statusSuccess.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Downloaded",
                                        tint = colors.statusSuccess,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ready",
                                        style = GriaTypography.labelSmall.copy(
                                            color = colors.statusSuccess,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (model.infoUrl != null) {
                                    IconButton(
                                        onClick = {
                                            GriaHaptic.click(view)
                                            ModelHubCatalog.openInfoPage(context, model.infoUrl)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInBrowser,
                                            contentDescription = "View Info / Kaggle",
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.accent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            GriaHaptic.confirm(view)
                                            ModelHubCatalog.startDownload(context, model)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download",
                                            tint = colors.surfaceElevated,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Get",
                                            style = GriaTypography.labelSmall.copy(
                                                color = colors.surfaceElevated,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
