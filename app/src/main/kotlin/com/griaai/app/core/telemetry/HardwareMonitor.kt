package com.griaai.app.core.telemetry

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.griaai.app.core.llm.OnDeviceLlmEngine
import java.io.File
import java.util.Locale

data class HardwareSnapshot(
    val totalRamBytes: Long,
    val availRamBytes: Long,
    val usedRamBytes: Long,
    val ramUsedPercent: Int,
    val appHeapUsedBytes: Long,
    val appHeapMaxBytes: Long,
    val socModel: String,
    val cpuCores: Int,
    val gpuRenderer: String,
    val activeBackend: String,
    val thermalStatus: String,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val modelName: String?,
    val modelSizeBytes: Long?,
    val modelBackend: String?
) {
    val totalRamGb: String get() = String.format(Locale.US, "%.1f GB", totalRamBytes / (1024.0 * 1024.0 * 1024.0))
    val usedRamGb: String get() = String.format(Locale.US, "%.1f GB", usedRamBytes / (1024.0 * 1024.0 * 1024.0))
    val availRamGb: String get() = String.format(Locale.US, "%.1f GB", availRamBytes / (1024.0 * 1024.0 * 1024.0))
    val appHeapUsedMb: String get() = String.format(Locale.US, "%d MB", appHeapUsedBytes / (1024 * 1024))
    val appHeapMaxMb: String get() = String.format(Locale.US, "%d MB", appHeapMaxBytes / (1024 * 1024))
    val modelSizeFormatted: String? get() = modelSizeBytes?.let {
        if (it >= 1024 * 1024 * 1024) {
            String.format(Locale.US, "%.2f GB", it / (1024.0 * 1024.0 * 1024.0))
        } else {
            String.format(Locale.US, "%d MB", it / (1024 * 1024))
        }
    }
}

class HardwareMonitor(
    private val context: Context,
    private val onDeviceEngine: OnDeviceLlmEngine
) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    fun getSnapshot(): HardwareSnapshot {
        // System RAM
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem
        val availRam = memInfo.availMem
        val usedRam = (totalRam - availRam).coerceAtLeast(0L)
        val ramPercent = if (totalRam > 0) ((usedRam.toDouble() / totalRam.toDouble()) * 100).toInt() else 0

        // App Process RAM (Heap)
        val runtime = Runtime.getRuntime()
        val heapUsed = runtime.totalMemory() - runtime.freeMemory()
        val heapMax = runtime.maxMemory()

        // SOC & CPU
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.SOC_MODEL.isNotBlank()) {
            Build.SOC_MODEL
        } else {
            Build.HARDWARE
        }
        val friendlySoc = when {
            soc.contains("sm8", ignoreCase = true) || soc.contains("taro", ignoreCase = true) || soc.contains("cape", ignoreCase = true) ->
                "Snapdragon 8+ Gen 1"
            soc.contains("qcom", ignoreCase = true) -> "Qualcomm Snapdragon"
            else -> soc.ifEmpty { "Snapdragon Platform" }
        }
        val cores = runtime.availableProcessors()

        // GPU / Backend
        val loadedName = onDeviceEngine.loadedModelName
        val loadedPath = onDeviceEngine.loadedModelPath
        val isGpuModel = loadedName?.contains("gpu", ignoreCase = true) == true
        val activeBackend = if (isGpuModel) "Adreno 730 GPU (OpenCL)" else "Qualcomm CPU (NEON)"
        val gpuRenderer = "Qualcomm Adreno 730"

        // Model File Size
        val modelSize = loadedPath?.let { path ->
            val file = File(path)
            if (file.exists()) file.length() else null
        }

        // Thermal Headroom
        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Cool (Optimal)"
                PowerManager.THERMAL_STATUS_LIGHT -> "Normal (Light Load)"
                PowerManager.THERMAL_STATUS_MODERATE -> "Warm (Moderate)"
                PowerManager.THERMAL_STATUS_SEVERE -> "Hot (Throttling)"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Heat"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency Cooldown"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Thermal Shutdown"
                else -> "Normal"
            }
        } else {
            "Normal"
        }

        // Battery Status
        var batteryPct = 100
        var isCharging = false
        try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                batteryPct = ((level.toFloat() / scale.toFloat()) * 100).toInt()
            }
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (_: Exception) {}

        return HardwareSnapshot(
            totalRamBytes = totalRam,
            availRamBytes = availRam,
            usedRamBytes = usedRam,
            ramUsedPercent = ramPercent,
            appHeapUsedBytes = heapUsed,
            appHeapMaxBytes = heapMax,
            socModel = friendlySoc,
            cpuCores = cores,
            gpuRenderer = gpuRenderer,
            activeBackend = activeBackend,
            thermalStatus = thermal,
            batteryPercent = batteryPct,
            isCharging = isCharging,
            modelName = loadedName,
            modelSizeBytes = modelSize,
            modelBackend = if (isGpuModel) "GPU (INT4)" else "CPU (INT8)"
        )
    }
}
