package com.nexusai.app.core.network

import com.nexusai.app.data.remote.dto.ChatCompletionChunkDto
import com.nexusai.app.data.remote.dto.ChatCompletionRequestDto
import kotlinx.coroutines.CancellationException
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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // Long timeout for continuous token streaming
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val sseFactory = EventSources.createFactory(okHttpClient)

    fun streamChatCompletion(
        baseUrl: String,
        request: ChatCompletionRequestDto
    ): Flow<StreamEvent> = callbackFlow {
        val url = if (baseUrl.endsWith("/")) "${baseUrl}v1/chat/completions" else "$baseUrl/v1/chat/completions"
        val requestBodyString = json.encodeToString(ChatCompletionRequestDto.serializer(), request)
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val httpRequest = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .post(requestBodyString.toRequestBody(mediaType))
            .build()

        var eventSource: EventSource? = null

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
                    val chunk = json.decodeFromString(ChatCompletionChunkDto.serializer(), trimmed)
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
                val error = t ?: IOException("SSE connection failed with response code: ${response?.code}")
                trySend(StreamEvent.Error(error))
                close(error)
            }
        }

        try {
            eventSource = sseFactory.newEventSource(httpRequest, listener)
        } catch (e: Exception) {
            trySend(StreamEvent.Error(e))
            close(e)
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
