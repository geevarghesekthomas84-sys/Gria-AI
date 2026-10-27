package com.griaai.app.data.repository

import android.net.Uri
import android.util.Base64
import com.griaai.app.GriaApplication

import com.griaai.app.core.database.GriaDatabaseHelper
import com.griaai.app.core.datastore.SettingsDataStore
import com.griaai.app.core.llm.OnDeviceLlmEngine
import com.griaai.app.core.network.LmStudioApi
import com.griaai.app.core.network.LmStudioStreamClient
import com.griaai.app.core.network.StreamEvent
import com.griaai.app.data.remote.dto.ChatCompletionRequestDto
import com.griaai.app.data.remote.dto.ChatMessageDto
import com.griaai.app.domain.model.AIModel
import com.griaai.app.domain.model.Conversation
import com.griaai.app.domain.model.Message
import com.griaai.app.domain.model.MessageRole
import com.griaai.app.domain.model.Persona
import com.griaai.app.domain.model.ServerConfig
import com.griaai.app.domain.repository.ChatRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class ChatRepositoryImpl(
    private val dbHelper: GriaDatabaseHelper,
    private val dataStore: SettingsDataStore,
    private val streamClient: LmStudioStreamClient = LmStudioStreamClient(),
    override val onDeviceEngine: OnDeviceLlmEngine
) : ChatRepository {

    companion object {
        const val ON_DEVICE_MODEL_ID = "ondevice:gemma"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val pingClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private fun createApi(baseUrl: String, client: OkHttpClient = okHttpClient): LmStudioApi {
        val sanitized = baseUrl.trim()
        val normalized = when {
            sanitized.startsWith("http://", ignoreCase = true) || sanitized.startsWith("https://", ignoreCase = true) -> sanitized
            else -> "http://$sanitized"
        }.removeSuffix("/v1/models")
            .removeSuffix("/v1/chat/completions")
            .removeSuffix("/v1")
            .trimEnd('/')

        val urlWithSlash = "$normalized/"
        return Retrofit.Builder()
            .baseUrl(urlWithSlash)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LmStudioApi::class.java)
    }

    override fun getConversationsFlow(): Flow<List<Conversation>> {
        return dbHelper.dbChangeTrigger.map {
            dbHelper.getConversations()
        }
    }

    override suspend fun getConversations(): List<Conversation> {
        return dbHelper.getConversations()
    }

    override suspend fun getConversationById(id: String): Conversation? {
        return dbHelper.getConversationById(id)
    }

    override suspend fun saveConversation(conversation: Conversation) {
        dbHelper.insertOrUpdateConversation(conversation)
    }

    override suspend fun deleteConversation(id: String) {
        dbHelper.deleteConversation(id)
    }

    override suspend fun deleteAllConversations() {
        dbHelper.deleteAllConversations()
    }

    override fun getMessagesFlow(conversationId: String): Flow<List<Message>> {
        return dbHelper.dbChangeTrigger.map {
            dbHelper.getMessagesForConversation(conversationId)
        }
    }

    override suspend fun getMessages(conversationId: String): List<Message> {
        return dbHelper.getMessagesForConversation(conversationId)
    }

    override suspend fun saveMessage(message: Message) {
        dbHelper.insertOrUpdateMessage(message)
    }

    override suspend fun deleteMessage(messageId: String) {
        dbHelper.deleteMessage(messageId)
    }

    override suspend fun searchMessages(query: String): List<Message> {
        return dbHelper.searchMessages(query)
    }

    override suspend fun testConnection(baseUrl: String): Result<List<AIModel>> = withContext(Dispatchers.IO) {
        try {
            val api = createApi(baseUrl, pingClient)
            val response = api.getModels()
            val models = response.data.map {
                AIModel(id = it.id, objectType = it.objectType, ownedBy = it.ownedBy)
            }
            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchModels(baseUrl: String): Result<List<AIModel>> = withContext(Dispatchers.IO) {
        val discoveredFiles = onDeviceEngine.discoverAllAvailableModels()
        val onDeviceModels = if (discoveredFiles.isNotEmpty()) {
            discoveredFiles.map { file ->
                val sizeMb = file.length() / (1024 * 1024)
                val typeTag = if (file.name.contains("gpu", ignoreCase = true)) "GPU" else "CPU"
                AIModel(
                    id = "ondevice:${file.absolutePath}",
                    objectType = "on-device",
                    ownedBy = "⚡ Local (${file.name} • ${sizeMb}MB $typeTag)"
                )
            }
        } else {
            listOf(
                AIModel(
                    id = ON_DEVICE_MODEL_ID,
                    objectType = "on-device",
                    ownedBy = if (onDeviceEngine.isModelLoaded) "⚡ On-Device (${onDeviceEngine.loadedModelName})" else "⚡ On-Device (Gemma Local)"
                )
            )
        }

        try {
            val remoteResult = testConnection(baseUrl)
            if (remoteResult.isSuccess) {
                val remoteModels = remoteResult.getOrNull().orEmpty()
                Result.success(onDeviceModels + remoteModels)
            } else {
                // If remote LM Studio is offline, still enable on-device local AI
                Result.success(onDeviceModels)
            }
        } catch (e: Exception) {
            Result.success(onDeviceModels)
        }
    }

    private fun getBase64Image(uriStr: String): String? {
        return try {
            val uri = Uri.parse(uriStr)
            val context = GriaApplication.instance.applicationContext
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun streamChat(
        baseUrl: String,
        modelId: String,
        systemPrompt: String,
        messages: List<Message>,
        temperature: Float,
        maxTokens: Int,
        maxContextMessages: Int
    ): Flow<StreamEvent> {
        // Context window optimization: take the last N messages to respect context budget
        val contextBoundedMessages = if (messages.size > maxContextMessages) {
            messages.takeLast(maxContextMessages)
        } else {
            messages
        }

        // Check if user selected the on-device local model
        if (modelId.startsWith("ondevice:") || modelId == ON_DEVICE_MODEL_ID) {
            return flow {
                val customPath = if (modelId.startsWith("ondevice:") && modelId != ON_DEVICE_MODEL_ID) {
                    modelId.removePrefix("ondevice:")
                } else null

                if (customPath != null && onDeviceEngine.loadedModelPath != customPath) {
                    val targetFile = File(customPath)
                    if (targetFile.exists() && targetFile.canRead()) {
                        onDeviceEngine.loadModel(targetFile)
                    }
                }

                if (!onDeviceEngine.isModelLoaded) {
                    val helpText = "⚡ **Gemma 100% On-Device Local AI**\n\n" +
                            "No on-device model weights are loaded into GPU memory.\n\n" +
                            "To run Gemma directly on your Snapdragon 8 Gen 1 Adreno GPU:\n" +
                            "1. Open **Settings** → **On-Device Local AI**.\n" +
                            "2. Tap **Select Model (.bin)** to load your downloaded Gemma INT4 weights (or place `gemma-2b-it-gpu-int4.bin` in Downloads).\n" +
                            "3. Enjoy ultra-fast, 100% offline private local AI with zero latency!"

                    for (word in helpText.split(" ")) {
                        currentCoroutineContext().ensureActive()
                        emit(StreamEvent.Token("$word "))
                        delay(12)
                    }
                    emit(StreamEvent.Completed)
                    return@flow
                }

                try {
                    val prompt = onDeviceEngine.formatGemmaPrompt(systemPrompt, contextBoundedMessages)
                    onDeviceEngine.generateStream(prompt).collect { chunk ->
                        currentCoroutineContext().ensureActive()
                        emit(StreamEvent.Token(chunk))
                    }
                    emit(StreamEvent.Completed)
                } catch (ce: CancellationException) {
                    onDeviceEngine.stop()
                    throw ce
                } catch (e: Exception) {
                    emit(StreamEvent.Error(e))
                }
            }
        }

        val dtoList = mutableListOf<ChatMessageDto>()
        if (systemPrompt.isNotBlank()) {
            dtoList.add(ChatMessageDto(role = "system", content = systemPrompt))
        }

        contextBoundedMessages.forEach { msg ->
            val roleStr = when (msg.role) {
                MessageRole.SYSTEM -> "system"
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
            }
            val contentWithImage = if (msg.imageUri != null) {
                val b64 = getBase64Image(msg.imageUri)
                if (b64 != null) {
                    val promptText = msg.content.ifBlank { "Describe this image" }
                    "$promptText\n\n![image](data:image/jpeg;base64,$b64)"
                } else {
                    msg.content
                }
            } else {
                msg.content
            }
            dtoList.add(ChatMessageDto(role = roleStr, content = contentWithImage))
        }

        val request = ChatCompletionRequestDto(
            model = modelId,
            messages = dtoList,
            temperature = temperature,
            maxTokens = maxTokens,
            stream = true
        )

        return streamClient.streamChatCompletion(baseUrl, request)
    }

    override val serverConfigFlow: Flow<ServerConfig> = dataStore.serverConfigFlow
    override val isOnboardingCompleted: Flow<Boolean> = dataStore.isOnboardingCompleted

    override suspend fun updateServerConfig(config: ServerConfig) {
        dataStore.updateServerConfig(config)
    }

    override suspend fun setDefaultModel(modelId: String) {
        dataStore.setDefaultModel(modelId)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.setOnboardingCompleted(completed)
    }

    override suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.setDarkTheme(isDark)
    }

    override suspend fun getCustomPersonas(): List<Persona> = dbHelper.getCustomPersonas()

    override suspend fun saveCustomPersona(persona: Persona) {
        dbHelper.insertOrUpdateCustomPersona(persona)
    }

    override suspend fun deleteCustomPersona(id: String) {
        dbHelper.deleteCustomPersona(id)
    }
}
