package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.concurrent.TimeUnit

object GeminiServiceClient {
    private const val TAG = "GeminiServiceClient"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    data class TextPart(val text: String)
    data class Content(val parts: List<TextPart>)
    data class GeminiRequest(val contents: List<Content>, val systemInstruction: Content? = null)

    data class ResponsePart(val text: String?)
    data class ResponseContent(val parts: List<ResponsePart>?)
    data class Candidate(val content: ResponseContent?)
    data class GeminiResponse(val candidates: List<Candidate>?)

    suspend fun getDailyAiSuggestions(
        userName: String,
        tasksSummary: String,
        completedCount: Int,
        totalCount: Int,
        streakCount: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Please configure your GEMINI_API_KEY in the AI Studio Secrets panel to unlock the AI coach's personalized advice!"
        }

        val prompt = """
            Hello AI productivity coach! Please analyze my productivity stats for today and give me actionable suggestions.
            - User Name: $userName
            - Today's completion is $completedCount/$totalCount tasks.
            - Current Daily Streak: $streakCount days.
            - List of tasks with details (Title, Priority, Status, Category, Recurrence):
            $tasksSummary
            
            Provide a short, direct, highly motivating and personalized 2-paragraph analysis:
            1. An executive coaching insight (evaluate priorities, notice if study/work is balanced, suggest optimal timings or suggest which specific High-Priority task should be tackled first).
            2. A highly positive booster/productivity hack tailored to this profile. Keep it friendly, concise, and focused.
        """.trimIndent()

        val systemPrompt = "You are an elite, friendly, extremely motivating personal productivity assistant and time-management coach. Keep responses concise, structured, and practical. Speak directly to the user by name."

        try {
            val requestBodyJson = moshi.adapter(GeminiRequest::class.java).toJson(
                GeminiRequest(
                    contents = listOf(Content(parts = listOf(TextPart(prompt)))),
                    systemInstruction = Content(parts = listOf(TextPart(systemPrompt)))
                )
            )

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()
                    Log.e(TAG, "API call failed: Code ${response.code}, Body: $errBody")
                    return@withContext "The AI coach is currently busy compiling suggestions. (API code: ${response.code})"
                }

                val responseString = response.body?.string() ?: ""
                val geminiResponse = moshi.adapter(GeminiResponse::class.java).fromJson(responseString)
                val responseText = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                responseText ?: "The AI coach analyzed your day and left you high praise, but forgot to write it down! Try again shortly."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            "Could not connect to the AI productivity coach. Please check your network connection and API key configuration."
        }
    }
}
