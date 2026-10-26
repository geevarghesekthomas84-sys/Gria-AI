package com.nexusai.app.domain.repository

import com.nexusai.app.core.llm.OnDeviceLlmEngine
import com.nexusai.app.core.network.StreamEvent
import com.nexusai.app.domain.model.AIModel
import com.nexusai.app.domain.model.ConnectionStatus
import com.nexusai.app.domain.model.Conversation
import com.nexusai.app.domain.model.Message
import com.nexusai.app.domain.model.ServerConfig
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    // On-Device Local LLM Engine
    val onDeviceEngine: OnDeviceLlmEngine
    // Conversations
    fun getConversationsFlow(): Flow<List<Conversation>>
    suspend fun getConversations(): List<Conversation>
    suspend fun getConversationById(id: String): Conversation?
    suspend fun saveConversation(conversation: Conversation)
    suspend fun deleteConversation(id: String)
    suspend fun deleteAllConversations()

    // Messages
    fun getMessagesFlow(conversationId: String): Flow<List<Message>>
    suspend fun getMessages(conversationId: String): List<Message>
    suspend fun saveMessage(message: Message)
    suspend fun deleteMessage(messageId: String)
    suspend fun searchMessages(query: String): List<Message>

    // Remote LM Studio
    suspend fun testConnection(baseUrl: String): Result<List<AIModel>>
    suspend fun fetchModels(baseUrl: String): Result<List<AIModel>>
    fun streamChat(
        baseUrl: String,
        modelId: String,
        systemPrompt: String,
        messages: List<Message>,
        temperature: Float,
        maxTokens: Int,
        maxContextMessages: Int
    ): Flow<StreamEvent>

    // Settings
    val serverConfigFlow: Flow<ServerConfig>
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun updateServerConfig(config: ServerConfig)
    suspend fun setDefaultModel(modelId: String)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setDarkTheme(isDark: Boolean)
}
