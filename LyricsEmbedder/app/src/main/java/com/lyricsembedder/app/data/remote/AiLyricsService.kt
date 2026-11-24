package com.lyricsembedder.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

enum class AiProvider {
    GEMINI,
    OPEN_ROUTER
}

class AiLyricsService(
    private val client: OkHttpClient,
    private val json: Json
) {
    /**
     * Uses user-provided Gemini or OpenRouter API key to clean up or format lyrics while
     * strictly preserving existing LRC timestamps.
     */
    suspend fun assistLyrics(
        provider: AiProvider,
        apiKey: String,
        model: String,
        instruction: String,
        currentLyrics: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) { "Vui lòng nhập khóa API trong Cài đặt > Trình chỉnh sửa lời bài hát." }
            val prompt = buildString {
                appendLine(instruction)
                appendLine("Giữ nguyên tất cả các dấu thời gian LRC [mm:ss.xx] nếu có. Chỉ trả về nội dung lời bài hát đã chỉnh sửa.")
                appendLine()
                append(currentLyrics)
            }

            when (provider) {
                AiProvider.GEMINI -> callGemini(apiKey.trim(), model.ifBlank { "gemini-2.5-flash" }, prompt)
                AiProvider.OPEN_ROUTER -> callOpenRouter(apiKey.trim(), model.ifBlank { "openrouter/free" }, prompt)
            }
        }
    }

    private fun callGemini(apiKey: String, model: String, prompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val payload = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", prompt) })
                    })
                })
            })
        }
        val req = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException("Gemini API error ${resp.code}: $body")
            val root = json.parseToJsonElement(body).jsonObject
            return root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content
                ?.trim()
                ?: throw IOException("Không nhận được nội dung từ Gemini API")
        }
    }

    private fun callOpenRouter(apiKey: String, model: String, prompt: String): String {
        val url = "https://openrouter.ai/api/v1/chat/completions"
        val payload = buildJsonObject {
            put("model", model)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://lyricsembedder.local")
            .header("X-Title", "LyricsEmbedder")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException("OpenRouter API error ${resp.code}: $body")
            val root = json.parseToJsonElement(body).jsonObject
            return root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.content
                ?.trim()
                ?: throw IOException("Không nhận được nội dung từ OpenRouter API")
        }
    }
}
