package com.example.gibdd_ochevidec.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    suspend fun allMessages(observerDeviceId: String, authorization: String,
        clientApp: String = "employee", afterMessageId: String? = null, limit: Int = 100): MessageListResponse {
        val all = mutableListOf<MessageResponse>()
        var cursor = afterMessageId
        while (true) {
            val page = apiService.messages(observerDeviceId = observerDeviceId, authorization = authorization,
                clientApp = clientApp, afterMessageId = cursor, limit = limit).messages
            all.addAll(page)
            if (page.size < limit || page.lastOrNull()?.messageId == cursor) break
            cursor = page.last().messageId
        }
        return MessageListResponse(all.distinctBy { it.messageId })
    }
    suspend fun allChats(authorization: String): ChatListResponse {
        val all = mutableListOf<ChatSummaryResponse>()
        var offset = 0
        while (true) {
            val page = apiService.chats(authorization, offset = offset).chats
            all.addAll(page)
            if (page.size < 200) break
            offset += page.size
        }
        return ChatListResponse(all.distinctBy { it.observerDeviceId })
    }

    const val BASE_URL = com.example.gibdd_ochevidec.BuildConfig.API_BASE_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
