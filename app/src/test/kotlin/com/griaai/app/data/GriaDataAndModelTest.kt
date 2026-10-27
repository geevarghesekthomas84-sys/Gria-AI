package com.griaai.app.data

import com.griaai.app.core.network.StreamEvent
import com.griaai.app.data.remote.dto.ChatCompletionChunkDto
import com.griaai.app.data.remote.dto.ChatCompletionRequestDto
import com.griaai.app.data.remote.dto.ChatMessageDto
import com.griaai.app.data.remote.dto.ChunkChoiceDto
import com.griaai.app.data.remote.dto.ChunkDeltaDto
import com.griaai.app.data.remote.dto.ModelsResponseDto
import com.griaai.app.domain.model.Conversation
import com.griaai.app.domain.model.Message
import com.griaai.app.domain.model.MessageRole
import com.griaai.app.domain.model.MessageStatus
import com.griaai.app.domain.model.Persona
import com.griaai.app.domain.model.PersonaPreset
import com.griaai.app.domain.model.ServerConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GriaDataAndModelTest {

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
            content = "Hello Gria-AI!",
            status = MessageStatus.COMPLETE,
            imageUri = "content://media/external/images/media/42"
        )
        assertEquals(MessageRole.USER, msg.role)
        assertEquals(MessageStatus.COMPLETE, msg.status)
        assertEquals("Hello Gria-AI!", msg.content)
        assertEquals("content://media/external/images/media/42", msg.imageUri)
    }

    @Test
    fun testPersonaPresetToPersonaMapping() {
        val presets = PersonaPreset.entries
        assertEquals(4, presets.size)

        val coder = PersonaPreset.CODER.toPersona()
        assertEquals("coder", coder.id)
        assertEquals("Coder", coder.title)
        assertFalse(coder.isCustom)
        assertTrue(coder.systemPrompt.contains("senior software engineer"))

        val allDefaults = Persona.DEFAULT_PERSONAS
        assertEquals(4, allDefaults.size)
        assertTrue(allDefaults.any { it.id == "personal" })
        assertTrue(allDefaults.any { it.id == "security" })
    }

    @Test
    fun testCustomPersonaCreationAndValidation() {
        val custom = Persona(
            id = "custom-123",
            title = "Python Tutor",
            subtitle = "Step-by-step programming teacher",
            systemPrompt = "You are a patient Python instructor.",
            isCustom = true
        )
        assertTrue(custom.isCustom)
        assertEquals("Python Tutor", custom.title)
        assertEquals("custom-123", custom.id)
    }

    @Test
    fun testSpecialTokenStrippingRegex() {
        val specialTokenRegex = Regex("""<bos>|<eos>|<start_of_turn>|<end_of_turn>|</s>|<\|im_end\|>|<\|im_start\|>""")
        val textWithSpecial = "<bos><start_of_turn>model\nHello! How can I assist you today?<end_of_turn><eos>"
        val sanitized = specialTokenRegex.replace(textWithSpecial, "").trim()
        assertEquals("model\nHello! How can I assist you today?", sanitized)

        val pureBos = "<bos><bos><bos>"
        val sanitizedBos = specialTokenRegex.replace(pureBos, "").trim()
        assertEquals("", sanitizedBos)
    }

    @Test
    fun testPromptEnhancerCoding() {
        val raw = "write snake game in python"
        val enhanced = com.griaai.app.core.utils.PromptEnhancer.enhance(raw)
        assertTrue(enhanced.contains("staff-level software engineer"))
        assertTrue(enhanced.contains("Idiomatic, production-grade code"))
    }

    @Test
    fun testPromptEnhancerExplain() {
        val raw = "explain quantum computing"
        val enhanced = com.griaai.app.core.utils.PromptEnhancer.enhance(raw)
        assertTrue(enhanced.contains("Core Mechanics & Fundamentals"))
        assertTrue(enhanced.contains("Common Pitfalls"))
    }

    @Test
    fun testModelHubCatalogInvariants() {
        val models = com.griaai.app.core.llm.ModelHubCatalog.AVAILABLE_MODELS
        assertTrue(models.isNotEmpty())
        assertTrue(models.any { it.id == "gemma-2b-gpu" })
        assertEquals("gemma-2b-it-gpu-int4.bin", models.first { it.id == "gemma-2b-gpu" }.fileName)
        assertTrue(models.any { it.format == ".bin" })
        assertTrue(models.any { it.format == ".task" })
    }

    @Test
    fun testThinkingTagParsing() {
        val text = "<think>Let me calculate 2+2. 2+2 equals 4.</think>The answer is 4."
        val hasThink = text.contains("<think>")
        assertTrue(hasThink)
        val think = text.substringAfter("<think>").substringBefore("</think>").trim()
        val rest = text.substringAfter("</think>").trim()
        assertEquals("Let me calculate 2+2. 2+2 equals 4.", think)
        assertEquals("The answer is 4.", rest)
    }

    @Test
    fun testSupportedModelFormats() {
        val exts = com.griaai.app.core.llm.OnDeviceLlmEngine.SUPPORTED_EXTENSIONS
        assertTrue(exts.contains(".bin"))
        assertTrue(exts.contains(".task"))
        assertTrue(exts.contains(".tflite"))
        assertTrue(exts.contains(".litert"))
    }
}
