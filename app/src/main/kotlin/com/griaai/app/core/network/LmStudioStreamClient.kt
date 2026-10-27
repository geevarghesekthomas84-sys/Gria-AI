package com.griaai.app.core.network

import com.griaai.app.data.remote.dto.ChatCompletionChunkDto
import com.griaai.app.data.remote.dto.ChatCompletionRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class StreamEvent {
    data class Token(val text: String) : StreamEvent()
    data object Completed : StreamEvent()
    data class Error(val throwable: Throwable) : StreamEvent()
}

class LmStudioStreamClient(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // Long timeout for continuous token streaming
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val sseFactory = EventSources.createFactory(okHttpClient)

    fun streamChatCompletion(
        baseUrl: String,
        request: ChatCompletionRequestDto
    ): Flow<StreamEvent> = callbackFlow {
        val sanitizedBase = baseUrl.trim()
        val normalizedBase = when {
            sanitizedBase.startsWith("http://", ignoreCase = true) || sanitizedBase.startsWith("https://", ignoreCase = true) -> sanitizedBase
            else -> "http://$sanitizedBase"
        }.removeSuffix("/v1/chat/completions")
            .removeSuffix("/v1")
            .trimEnd('/')

        val url = "$normalizedBase/v1/chat/completions"

        var eventSource: EventSource? = null

        val httpRequest = try {
            val requestBodyString = json.encodeToString(ChatCompletionRequestDto.serializer(), request)
            val mediaType = "application/json; charset=utf-8".toMediaType()

            Request.Builder()
                .url(url)
                .header("Accept", "text/event-stream")
                .header("Content-Type", "application/json")
                .post(requestBodyString.toRequestBody(mediaType))
                .build()
        } catch (e: Exception) {
            trySend(StreamEvent.Error(e))
            close()
            return@callbackFlow
        }

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                // Connection successfully opened
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                val trimmed = data.trim()
                if (trimmed == "[DONE]") {
                    trySend(StreamEvent.Completed)
                    close()
                    return
                }

                try {
                    val cleanJson = if (trimmed.startsWith("data:", ignoreCase = true)) {
                        trimmed.substring(5).trim()
                    } else trimmed

                    val chunk = json.decodeFromString(ChatCompletionChunkDto.serializer(), cleanJson)
                    val token = chunk.choices.firstOrNull()?.delta?.content
                    if (!token.isNullOrEmpty()) {
                        trySend(StreamEvent.Token(token))
                    }
                } catch (e: Exception) {
                    // Ignore empty or keep-alive metadata chunks
                }
            }

            override fun onClosed(eventSource: EventSource) {
                trySend(StreamEvent.Completed)
                close()
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val errorMsg = when {
                    t != null -> t.localizedMessage ?: t.toString()
                    response != null -> "LM Studio server responded with HTTP ${response.code}: ${response.message}"
                    else -> "Connection failed to LM Studio at $normalizedBase. Ensure LM Studio is active."
                }
                val error = t ?: IOException(errorMsg)
                trySend(StreamEvent.Error(error))
                close()
            }
        }

        try {
            eventSource = sseFactory.newEventSource(httpRequest, listener)
        } catch (e: Exception) {
            trySend(StreamEvent.Error(e))
            close()
        }

        awaitClose {
            try {
                eventSource?.cancel()
            } catch (e: Exception) {
                // Ignore cancel exceptions
            }
        }
    }.flowOn(Dispatchers.IO)
}
