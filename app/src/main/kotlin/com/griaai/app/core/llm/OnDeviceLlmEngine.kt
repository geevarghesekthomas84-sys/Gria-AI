package com.griaai.app.core.llm

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.griaai.app.domain.model.Message
import com.griaai.app.domain.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class OnDeviceLlmEngine(private val context: Context) {

    companion object {
        private val SPECIAL_TOKEN_REGEX = Regex("<bos>|<eos>|<start_of_turn>|<end_of_turn>|<s>|</s>|<pad>|<unk>|<\\|im_end\\|>|<\\|im_start\\|>")
        private val STOP_TOKENS = listOf("<end_of_turn>", "<eos>", "</s>", "<bos>", "<|im_end|>")
    }

    private var currentModelPath: String? = null
    private var llmInference: LlmInference? = null
    
    @Volatile
    private var activeChunkListener: ((String, Boolean) -> Unit)? = null

    @Volatile
    private var activeErrorListener: ((Throwable) -> Unit)? = null

    val isModelLoaded: Boolean
        get() = llmInference != null

    val loadedModelName: String?
        get() = currentModelPath?.let { File(it).name }

    val loadedModelPath: String?
        get() = currentModelPath

    @Volatile
    var isGenerating: Boolean = false
        private set

    fun discoverAllAvailableModels(): List<File> {
        val candidateDirs = listOfNotNull(
            File("/sdcard/models"),
            File(Environment.getExternalStorageDirectory(), "models"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "models"),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            File(context.filesDir, "models"),
            context.getExternalFilesDir("models")
        )

        val filesFound = mutableListOf<File>()
        for (dir in candidateDirs) {
            try {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles { file ->
                        file.isFile && (file.name.endsWith(".bin") || file.name.endsWith(".task"))
                    }?.let { filesFound.addAll(it) }
                }
            } catch (e: Exception) {
                Log.w("OnDeviceLlmEngine", "Error scanning directory: ${dir.absolutePath}", e)
            }
        }

        // Deduplicate and prioritize:
        // 1. GPU models first (avoids OpenCL missing on CPU-only files)
        // 2. INT4 models before INT8 (prevents OOM memory kill)
        // 3. Gemma models first
        return filesFound.distinctBy { it.name }.sortedWith(
            compareByDescending<File> { it.name.contains("gpu", ignoreCase = true) }
                .thenBy { it.name.contains("int8", ignoreCase = true) }
                .thenByDescending { it.name.contains("gemma", ignoreCase = true) }
        )
    }

    fun tryAutoLoadAvailableModel(): Boolean {
        if (isModelLoaded) return true

        val candidates = discoverAllAvailableModels()
        for (candidate in candidates) {
            val res = loadModel(candidate)
            if (res.isSuccess) return true
        }

        return false
    }

    @Synchronized
    fun loadModel(modelFile: File): Result<Unit> {
        return try {
            if (!modelFile.exists() || !modelFile.canRead()) {
                return Result.failure(IllegalArgumentException("Model file does not exist or cannot be read: ${modelFile.absolutePath}"))
            }

            if (currentModelPath == modelFile.absolutePath && llmInference != null) {
                return Result.success(Unit)
            }

            close()

            // 1024 tokens balances performance and device memory safety (prevents OOM on 8-bit/2B models)
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .setTemperature(0.7f)
                .setTopK(40)
                .setResultListener { partial, done ->
                    activeChunkListener?.invoke(partial, done)
                }
                .setErrorListener { error ->
                    Log.e("OnDeviceLlmEngine", "Inference error: $error")
                    activeErrorListener?.invoke(error)
                }
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            currentModelPath = modelFile.absolutePath
            Log.i("OnDeviceLlmEngine", "Successfully loaded on-device model from ${modelFile.absolutePath}")
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e("OnDeviceLlmEngine", "Failed to load on-device model from ${modelFile.absolutePath}", t)
            close()
            Result.failure(t)
        }
    }

    suspend fun loadModelFromUri(
        uri: Uri,
        fileName: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) modelsDir.mkdirs()

            val targetFile = File(modelsDir, fileName)
            val contentResolver = context.contentResolver

            contentResolver.openInputStream(uri)?.use { input ->
                val totalBytes = contentResolver.openFileDescriptor(uri, "r")?.statSize ?: -1L
                var bytesCopied = 0L

                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        if (totalBytes > 0) {
                            onProgress(bytesCopied.toFloat() / totalBytes)
                        }
                    }
                    output.flush()
                }
            } ?: return@withContext Result.failure(IllegalArgumentException("Unable to read content URI: $uri"))

            loadModel(targetFile)
        } catch (e: Exception) {
            Log.e("OnDeviceLlmEngine", "Error importing model from URI", e)
            Result.failure(e)
        }
    }

    fun formatGemmaPrompt(systemPrompt: String, messages: List<Message>): String {
        val builder = StringBuilder("<bos>")

        val validMessages = messages.filter { it.content.isNotBlank() }

        if (validMessages.isEmpty()) {
            return "<bos><start_of_turn>user\n${systemPrompt.ifBlank { "Hello!" }}<end_of_turn>\n<start_of_turn>model\n"
        }

        var isFirstUserTurn = true
        for (msg in validMessages) {
            when (msg.role) {
                MessageRole.USER -> {
                    builder.append("<start_of_turn>user\n")
                    if (isFirstUserTurn && systemPrompt.isNotBlank()) {
                        builder.append("[System Instructions: ").append(systemPrompt.trim()).append("]\n\n")
                        isFirstUserTurn = false
                    }
                    builder.append(msg.content.trim()).append("<end_of_turn>\n")
                }
                MessageRole.ASSISTANT -> {
                    builder.append("<start_of_turn>model\n")
                    val cleanAssistant = msg.content.replace(SPECIAL_TOKEN_REGEX, "").trim()
                    builder.append(cleanAssistant).append("<end_of_turn>\n")
                }
                MessageRole.SYSTEM -> {
                    // System messages are folded into prompt context
                }
            }
        }

        builder.append("<start_of_turn>model\n")
        return builder.toString()
    }

    fun stop() {
        isGenerating = false
        activeChunkListener = null
        activeErrorListener = null
    }

    fun generateStream(prompt: String): Flow<String> = callbackFlow {
        val inference = llmInference
        if (inference == null) {
            trySend("⚠️ No on-device model loaded. Select a Gemma model in Settings.")
            close()
            return@callbackFlow
        }

        if (isGenerating) {
            trySend("⚠️ Another on-device generation is already running. Please wait or tap stop.")
            close()
            return@callbackFlow
        }

        isGenerating = true

        activeChunkListener = { partial, done ->
            if (isGenerating) {
                var reachedStop = done
                for (stopToken in STOP_TOKENS) {
                    if (partial.contains(stopToken)) {
                        reachedStop = true
                        break
                    }
                }

                val cleanChunk = partial.replace(SPECIAL_TOKEN_REGEX, "")
                if (cleanChunk.isNotEmpty()) {
                    trySend(cleanChunk)
                }

                if (reachedStop) {
                    isGenerating = false
                    close()
                }
            }
        }

        activeErrorListener = { throwable ->
            isGenerating = false
            Log.e("OnDeviceLlmEngine", "Inference error", throwable)
            trySend("\n[Inference Error: ${throwable.localizedMessage}]")
            close()
        }

        try {
            inference.generateResponseAsync(prompt)
        } catch (e: Exception) {
            isGenerating = false
            Log.e("OnDeviceLlmEngine", "Failed to launch generateResponseAsync", e)
            trySend("\n[Inference Exception: ${e.localizedMessage}]")
            close()
        }

        awaitClose {
            isGenerating = false
            activeChunkListener = null
            activeErrorListener = null
        }
    }.flowOn(Dispatchers.IO)

    @Synchronized
    fun close() {
        isGenerating = false
        activeChunkListener = null
        activeErrorListener = null
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.w("OnDeviceLlmEngine", "Error closing inference session", e)
        } finally {
            llmInference = null
            currentModelPath = null
        }
    }
}
