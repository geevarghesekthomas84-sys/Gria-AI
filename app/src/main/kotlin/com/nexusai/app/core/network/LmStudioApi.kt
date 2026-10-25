package com.nexusai.app.core.network

import com.nexusai.app.data.remote.dto.ChatCompletionRequestDto
import com.nexusai.app.data.remote.dto.ModelsResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface LmStudioApi {
    @GET("v1/models")
    suspend fun getModels(): ModelsResponseDto

    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatCompletionRequestDto
    ): retrofit2.Response<okhttp3.ResponseBody>
}
