package com.nexusai.app.data.repository

import com.nexusai.app.core.database.NexusDatabaseHelper
import com.nexusai.app.core.datastore.SettingsDataStore
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
import kotlinx.coroutines.flow.Flow
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
    private val streamClient: LmStudioStreamClient = LmStudioStreamClient()
) : ChatRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun createApi(baseUrl: String): LmStudioApi {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
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
            val api = createApi(baseUrl)
            val response = api.getModels()
            val models = response.data.map {
                AIModel(id = it.id, objectType = it.objectType, ownedBy = it.ownedBy)
            }
            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchModels(baseUrl: String): Result<List<AIModel>> {
        return testConnection(baseUrl)
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
