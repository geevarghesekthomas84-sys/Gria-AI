package com.nexusai.app.data

import com.nexusai.app.core.network.StreamEvent
import com.nexusai.app.data.remote.dto.ChatCompletionChunkDto
import com.nexusai.app.data.remote.dto.ChatCompletionRequestDto
import com.nexusai.app.data.remote.dto.ChatMessageDto
import com.nexusai.app.data.remote.dto.ChunkChoiceDto
import com.nexusai.app.data.remote.dto.ChunkDeltaDto
import com.nexusai.app.data.remote.dto.ModelsResponseDto
import com.nexusai.app.domain.model.Conversation
import com.nexusai.app.domain.model.Message
import com.nexusai.app.domain.model.MessageRole
import com.nexusai.app.domain.model.MessageStatus
import com.nexusai.app.domain.model.ServerConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NexusDataAndModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun testServerConfigBaseUrlGeneration() {
        val configHttp = ServerConfig(ipAddress = "192.168.1.150", port = 1234, useHttps = false)
        assertEquals("http://192.168.1.150:1234", configHttp.baseUrl)

        val configHttps = ServerConfig(ipAddress = "nexus.local", port = 8443, useHttps = true)
        assertEquals("https://nexus.local:8443", configHttps.baseUrl)
    }

    @Test
    fun testModelsResponseDeserialization() {
        val jsonString = """
            {
                "object": "list",
                "data": [
                    {
                        "id": "qwen2.5-coder-7b-instruct",
                        "object": "model",
                        "owned_by": "organization-owner"
                    },
                    {
                        "id": "meta-llama-3.1-8b-instruct",
                        "object": "model",
                        "owned_by": "meta"
                    }
                ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString(ModelsResponseDto.serializer(), jsonString)
        assertEquals(2, parsed.data.size)
        assertEquals("qwen2.5-coder-7b-instruct", parsed.data[0].id)
        assertEquals("meta-llama-3.1-8b-instruct", parsed.data[1].id)
    }

    @Test
    fun testChatCompletionChunkDeserialization() {
        val chunkJson = """
            {
                "id": "chatcmpl-12345",
                "object": "chat.completion.chunk",
                "created": 1700000000,
                "model": "qwen2.5-coder-7b-instruct",
                "choices": [
                    {
                        "index": 0,
                        "delta": {
                            "role": "assistant",
                            "content": "Hello, world!"
                        },
                        "finish_reason": null
                    }
                ]
            }
        """.trimIndent()

        val chunk = json.decodeFromString(ChatCompletionChunkDto.serializer(), chunkJson)
        assertEquals("chatcmpl-12345", chunk.id)
        assertEquals("Hello, world!", chunk.choices[0].delta.content)
    }

    @Test
    fun testChatCompletionRequestSerialization() {
        val request = ChatCompletionRequestDto(
            model = "qwen2.5-coder-7b-instruct",
            messages = listOf(
                ChatMessageDto(role = "system", content = "System prompt"),
                ChatMessageDto(role = "user", content = "Explain Kotlin coroutines")
            ),
            temperature = 0.7f,
            maxTokens = 2048,
            stream = true
        )

        val encoded = json.encodeToString(ChatCompletionRequestDto.serializer(), request)
        println("ENCODED_JSON: $encoded")
        assertTrue("Expected stream in $encoded", encoded.contains("stream"))
        assertTrue("Expected model in $encoded", encoded.contains("qwen2.5-coder-7b-instruct"))
    }

    @Test
    fun testConversationAndMessageDomainInvariants() {
        val conv = Conversation(
            id = "c1",
            title = "Kotlin Discussion",
            modelId = "llama-3-8b"
        )
        assertFalse(conv.isPinned)
        assertEquals("Kotlin Discussion", conv.title)

        val msg = Message(
            id = "m1",
            conversationId = conv.id,
            role = MessageRole.USER,
            content = "Hello Nexus!",
            status = MessageStatus.COMPLETE
        )
        assertEquals(MessageRole.USER, msg.role)
        assertEquals(MessageStatus.COMPLETE, msg.status)
        assertEquals("Hello Nexus!", msg.content)
    }
}
