package com.griaai.app.core.llm

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File

data class HubModel(
    val id: String,
    val name: String,
    val description: String,
    val sizeString: String,
    val accelerator: String,
    val fileName: String,
    val format: String,
    val downloadUrl: String,
    val infoUrl: String? = null
) {
    fun isDownloaded(context: Context): Boolean {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileDownloads = File(downloadDir, fileName)
        val fileAppInternal = File(File(context.filesDir, "models"), fileName)
        val fileAppExternal = File(context.getExternalFilesDir("models"), fileName)
        val minSize = 50L * 1024 * 1024 // 50MB
        return (fileDownloads.exists() && fileDownloads.length() > minSize) ||
                (fileAppInternal.exists() && fileAppInternal.length() > minSize) ||
                (fileAppExternal.exists() && fileAppExternal.length() > minSize)
    }
}

object ModelHubCatalog {
    val AVAILABLE_MODELS = listOf(
        HubModel(
            id = "gemma-2b-gpu",
            name = "Gemma 2B IT (INT4 GPU)",
            description = "Official Google Gemma 2B instruction model for Qualcomm Snapdragon Adreno GPU via MediaPipe OpenCL.",
            sizeString = "1.3 GB",
            accelerator = "Adreno GPU",
            fileName = "gemma-2b-it-gpu-int4.bin",
            format = ".bin",
            downloadUrl = "https://huggingface.co/google/gemma-2b-it/resolve/main/gemma-2b-it-gpu-int4.bin",
            infoUrl = "https://www.kaggle.com/models/google/gemma/tfLite/gemma-2b-it-gpu-int4"
        ),
        HubModel(
            id = "gemma-2b-cpu",
            name = "Gemma 2B IT (INT4 CPU)",
            description = "Optimized for universal multi-core CPU execution. High compatibility across all Android devices.",
            sizeString = "1.3 GB",
            accelerator = "Universal CPU",
            fileName = "gemma-2b-it-cpu-int4.bin",
            format = ".bin",
            downloadUrl = "https://huggingface.co/google/gemma-2b-it/resolve/main/gemma-2b-it-cpu-int4.bin",
            infoUrl = "https://www.kaggle.com/models/google/gemma/tfLite/gemma-2b-it-cpu-int4"
        ),
        HubModel(
            id = "gemma-2b-task",
            name = "Gemma 2B MediaPipe Task Bundle",
            description = "Packaged MediaPipe Task bundle containing weights and tokenizer metadata for direct offline execution.",
            sizeString = "1.4 GB",
            accelerator = "GPU / CPU",
            fileName = "gemma-2b-it.task",
            format = ".task",
            downloadUrl = "https://huggingface.co/google/gemma-2b-it/resolve/main/gemma-2b-it.task",
            infoUrl = "https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android"
        )
    )

    fun startDownload(context: Context, model: HubModel): Long {
        return try {
            val request = DownloadManager.Request(Uri.parse(model.downloadUrl))
                .setTitle("Downloading ${model.name}")
                .setDescription("Saving to Downloads/${model.fileName}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, model.fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)
            Toast.makeText(context, "Download started: ${model.name}. Check notification bar for progress.", Toast.LENGTH_LONG).show()
            downloadId
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to initiate download: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            -1L
        }
    }

    fun openInfoPage(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
