package com.nexusai.app.data.repository

import com.nexusai.app.core.database.NexusDatabaseHelper
import com.nexusai.app.core.datastore.SettingsDataStore
import com.nexusai.app.core.llm.OnDeviceLlmEngine
import com.nexusai.app.core.network.LmStudioApi
import com.nexusai.app.core.network.LmStudioStreamClient
import com.nexusai.app.core.network.StreamEvent
import com.nexusai.app.data.remote.dto.ChatCompletionRequestDto
import com.nexusai.app.data.remote.dto.ChatMessageDto
import com.nexusai.app.domain.model.AIModel
import com.nexusai.app.domain.model.Conversation
import com.nexusai.app.domain.model.Message
import com.nexusai.app.domain.model.MessageRole
import com.nexusai.app.domain.model.ServerConfig
import com.nexusai.app.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
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
    private val dbHelper: NexusDatabaseHelper,
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
        val onDeviceModel = AIModel(
            id = ON_DEVICE_MODEL_ID,
            objectType = "on-device",
            ownedBy = if (onDeviceEngine.isModelLoaded) "⚡ On-Device (${onDeviceEngine.loadedModelName})" else "⚡ On-Device (Gemma Local)"
        )

        try {
            val remoteResult = testConnection(baseUrl)
            if (remoteResult.isSuccess) {
                val remoteModels = remoteResult.getOrNull().orEmpty()
                Result.success(listOf(onDeviceModel) + remoteModels)
            } else {
                // If remote LM Studio is offline, still enable on-device local AI
                Result.success(listOf(onDeviceModel))
            }
        } catch (e: Exception) {
            Result.success(listOf(onDeviceModel))
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
                if (!onDeviceEngine.isModelLoaded) {
                    val helpText = "⚡ **Gemma 100% On-Device Local AI**\n\n" +
                            "No on-device model weights are loaded into GPU memory.\n\n" +
                            "To run Gemma directly on your Snapdragon 8 Gen 1 Adreno GPU:\n" +
                            "1. Open **Settings** → **On-Device Local AI**.\n" +
                            "2. Tap **Select Model (.bin)** to load your downloaded Gemma INT4 weights (or place `gemma-2b-it-gpu-int4.bin` in Downloads).\n" +
                            "3. Enjoy ultra-fast, 100% offline private local AI with zero latency!"

                    for (word in helpText.split(" ")) {
                        emit(StreamEvent.Token("$word "))
                        delay(12)
                    }
                    emit(StreamEvent.Completed)
                    return@flow
                }

                try {
                    val prompt = onDeviceEngine.formatGemmaPrompt(systemPrompt, contextBoundedMessages)
                    onDeviceEngine.generateStream(prompt).collect { chunk ->
                        emit(StreamEvent.Token(chunk))
                    }
                    emit(StreamEvent.Completed)
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
            dtoList.add(ChatMessageDto(role = roleStr, content = msg.content))
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
}
