package com.example.gemini

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ChatHistoryItem(
    val role: String, // "user" or "model"
    val text: String
)

class GeminiApiService {
    private val TAG = "GeminiApiService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun streamGenerateContent(
        apiKey: String,
        model: String = "gemini-3.8-flash",
        systemInstruction: String,
        history: List<ChatHistoryItem>,
        newPrompt: String
    ): Flow<String> = flow {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API Key is not configured. Please open Settings and enter your Google Gemini API key.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey"

        val rootJson = JSONObject()

        // System Instruction
        if (systemInstruction.isNotBlank()) {
            val systemObj = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                }
                put("parts", parts)
            }
            rootJson.put("systemInstruction", systemObj)
        }

        // Contents
        val contentsArray = JSONArray()
        // Include recent history (last 10 turns for context and speed)
        val recentHistory = history.takeLast(10)
        for (item in recentHistory) {
            val role = if (item.role.lowercase() == "user") "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", item.text) })
                })
            })
        }

        // Current prompt
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", newPrompt) })
            })
        })

        rootJson.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 2048)
        }
        rootJson.put("generationConfig", genConfig)

        val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string().orEmpty()
            Log.e(TAG, "Gemini API error ($response.code): $errBody")
            var userMsg = "Gemini API Error (${response.code})"
            try {
                val errJson = JSONObject(errBody)
                val msg = errJson.optJSONObject("error")?.optString("message")
                if (!msg.isNullOrBlank()) {
                    userMsg = msg
                }
            } catch (e: Throwable) {
                // Ignore json parse error
            }
            throw IllegalStateException(userMsg)
        }

        val source = response.body?.byteStream()
            ?: throw IllegalStateException("Empty response body from Gemini.")

        val reader = BufferedReader(InputStreamReader(source))
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            val currentLine = line?.trim().orEmpty()
            if (currentLine.startsWith("data:")) {
                val jsonPayload = currentLine.removePrefix("data:").trim()
                if (jsonPayload.isNotEmpty()) {
                    try {
                        val chunkObj = JSONObject(jsonPayload)
                        val candidates = chunkObj.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text")
                                if (text.isNotEmpty()) {
                                    emit(text)
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error parsing SSE chunk: $jsonPayload", e)
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
