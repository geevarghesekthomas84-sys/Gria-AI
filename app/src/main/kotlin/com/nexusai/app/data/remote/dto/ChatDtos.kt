package com.nexusai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelsResponseDto(
    val data: List<ModelItemDto> = emptyList(),
    @SerialName("object") val objectType: String = "list"
)

@Serializable
data class ModelItemDto(
    val id: String,
    @SerialName("object") val objectType: String = "model",
    @SerialName("owned_by") val ownedBy: String = "lmstudio"
)

@Serializable
data class ChatCompletionRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Float = 0.7f,
    @SerialName("max_tokens") val maxTokens: Int = 2048,
    val stream: Boolean = true
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionChunkDto(
    val id: String = "",
    @SerialName("object") val objectType: String = "chat.completion.chunk",
    val created: Long = 0,
    val model: String = "",
    val choices: List<ChunkChoiceDto> = emptyList()
)

@Serializable
data class ChunkChoiceDto(
    val index: Int = 0,
    val delta: ChunkDeltaDto = ChunkDeltaDto(),
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChunkDeltaDto(
    val role: String? = null,
    val content: String? = null
)
