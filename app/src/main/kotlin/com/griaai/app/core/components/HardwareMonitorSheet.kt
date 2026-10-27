package com.griaai.app.core.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.griaai.app.GriaApplication
import com.griaai.app.core.telemetry.HardwareSnapshot
import com.griaai.app.core.theme.LocalGriaColors
import com.griaai.app.core.utils.GriaHaptic
import com.griaai.app.core.theme.GriaTypography
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareMonitorSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hardwareMonitor = remember { GriaApplication.instance.hardwareMonitor }
    var snapshot by remember { mutableStateOf(hardwareMonitor.getSnapshot()) }
    val colors = LocalGriaColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Auto-refresh snapshot every 2 seconds while open
    LaunchedEffect(Unit) {
        while (true) {
            snapshot = hardwareMonitor.getSnapshot()
            delay(2000L)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hardware & Memory Monitor",
                        style = GriaTypography.titleMedium.copy(color = colors.textPrimary)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            snapshot = hardwareMonitor.getSnapshot()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: System RAM Meter
            RamMeterCard(snapshot = snapshot)

            Spacer(modifier = Modifier.height(12.dp))

            // Section 2: Neural & GPU Acceleration
            ComputeEngineCard(snapshot = snapshot)

            Spacer(modifier = Modifier.height(12.dp))

            // Section 3: Model & Process Footprint
            ModelMemoryCard(snapshot = snapshot)

            Spacer(modifier = Modifier.height(12.dp))

            // Section 4: Device Thermals & Battery
            DeviceHealthCard(snapshot = snapshot)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RamMeterCard(snapshot: HardwareSnapshot) {
    val colors = LocalGriaColors.current
    val progress by animateFloatAsState(
        targetValue = snapshot.ramUsedPercent / 100f,
        label = "ram_progress"
    )

    val progressColor = when {
        snapshot.ramUsedPercent > 85 -> Color(0xFFEF4444)
        snapshot.ramUsedPercent > 70 -> Color(0xFFF59E0B)
        else -> colors.statusSuccess
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System RAM Usage",
                    style = GriaTypography.labelMedium.copy(color = colors.textSecondary)
                )
                Text(
                    text = "${snapshot.ramUsedPercent}%",
                    style = GriaTypography.titleMedium.copy(
                        color = progressColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = colors.surfaceSubtle
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "Used", value = snapshot.usedRamGb)
                MetricItem(label = "Available", value = snapshot.availRamGb)
                MetricItem(label = "Total RAM", value = snapshot.totalRamGb)
            }
        }
    }
}

@Composable
private fun ComputeEngineCard(snapshot: HardwareSnapshot) {
    val colors = LocalGriaColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Processor & Neural Compute",
                style = GriaTypography.labelMedium.copy(color = colors.textSecondary)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SoC Platform",
                        style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
                    )
                    Text(
                        text = snapshot.socModel,
                        style = GriaTypography.bodySmall.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CPU Cores",
                        style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
                    )
                    Text(
                        text = "${snapshot.cpuCores} High-Perf Cores",
                        style = GriaTypography.bodySmall.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GPU Acceleration",
                        style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
                    )
                    Text(
                        text = snapshot.gpuRenderer,
                        style = GriaTypography.bodySmall.copy(
                            color = colors.accent,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Inference Engine",
                        style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
                    )
                    Text(
                        text = snapshot.activeBackend,
                        style = GriaTypography.bodySmall.copy(
                            color = colors.statusSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelMemoryCard(snapshot: HardwareSnapshot) {
    val colors = LocalGriaColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Model & Process Memory Footprint",
                style = GriaTypography.labelMedium.copy(color = colors.textSecondary)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "Loaded Model",
                        style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
                    )
                    Text(
                        text = snapshot.modelName ?: "None (Remote Mode)",
                        style = GriaTypography.bodySmall.copy(
                            color = colors.textPrimary,
                            fontFamily = FontFamily.Monospace
                        ),
                        maxLines = 1
                    )
                }

                Column(modifier = Modifier.weight(0.8f)) {
                    Text(
                        text = "Model Size",
                        style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
                    )
                    Text(
                        text = snapshot.modelSizeFormatted ?: "--",
                        style = GriaTypography.bodySmall.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "App Heap Allocated",
                        style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
                    )
                    Text(
                        text = "${snapshot.appHeapUsedMb} / ${snapshot.appHeapMaxMb} max",
                        style = GriaTypography.bodySmall.copy(
                            color = colors.textSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Column(modifier = Modifier.weight(0.8f)) {
                    Text(
                        text = "Quantization",
                        style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
                    )
                    Text(
                        text = snapshot.modelBackend ?: "--",
                        style = GriaTypography.bodySmall.copy(
                            color = colors.accent,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceHealthCard(snapshot: HardwareSnapshot) {
    val colors = LocalGriaColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Thermal Headroom",
                    style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
                )
                Text(
                    text = snapshot.thermalStatus,
                    style = GriaTypography.bodySmall.copy(
                        color = if (snapshot.thermalStatus.contains("Cool") || snapshot.thermalStatus.contains("Normal")) colors.statusSuccess else Color(0xFFF59E0B),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Battery",
                    style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
                )
                Text(
                    text = "${snapshot.batteryPercent}% ${if (snapshot.isCharging) "⚡ Charging" else ""}",
                    style = GriaTypography.bodySmall.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    val colors = LocalGriaColors.current
    Column {
        Text(
            text = label,
            style = GriaTypography.labelSmall.copy(color = colors.textTertiary)
        )
        Text(
            text = value,
            style = GriaTypography.labelMedium.copy(
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
