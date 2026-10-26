package com.nexusai.app.domain.model

enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}

enum class MessageStatus {
    COMPLETE,
    STREAMING,
    ERROR
}

data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.COMPLETE,
    val errorMessage: String? = null,
    val tokensCount: Int? = null,
    val generationDurationMs: Long? = null,
    val tokensPerSecond: Float? = null
)

enum class PersonaPreset(
    val title: String,
    val subtitle: String,
    val systemPrompt: String
) {
    DEFAULT(
        title = "Personal",
        subtitle = "Thoughtful & helpful assistant",
        systemPrompt = "You are Gria-AI, a helpful, precise, and thoughtful on-device & local AI assistant."
    ),
    CODER(
        title = "Coder",
        subtitle = "Senior engineer & clean code",
        systemPrompt = "You are an expert senior software engineer. Provide clean, idiomatic, and highly efficient code. Avoid conversational filler and explain architectural trade-offs directly."
    ),
    SECURITY(
        title = "Security",
        subtitle = "Offensive security & exploit dev",
        systemPrompt = "You are an elite offensive security researcher, red team operator, and exploit analyst. Provide direct, rigorous technical vulnerability analysis, payload mechanics, and mitigation strategies."
    ),
    CONCISE(
        title = "Concise",
        subtitle = "Executive synthesis & bullets",
        systemPrompt = "You are an executive synthesizer. Give concise, direct answers with bullet points and zero conversational filler."
    )
}

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String = "",
    val isPinned: Boolean = false,
    val systemPrompt: String? = null
)

data class AIModel(
    val id: String,
    val objectType: String = "model",
    val ownedBy: String = "organization-owner"
)

enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    ERROR
}

data class ServerConfig(
    val ipAddress: String = "192.168.1.100",
    val port: Int = 1234,
    val useHttps: Boolean = false,
    val timeoutSeconds: Int = 60,
    val defaultModel: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val systemPrompt: String = "You are Gria-AI, a helpful, precise, and thoughtful on-device & local AI assistant.",
    val isDarkTheme: Boolean = false,
    val maxContextMessages: Int = 20
) {
    val baseUrl: String
        get() {
            val scheme = if (useHttps) "https" else "http"
            return "$scheme://$ipAddress:$port"
        }
}
