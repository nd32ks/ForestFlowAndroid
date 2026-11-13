package com.chiforest.forestflow.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to any OpenAI-style `/chat/completions` endpoint (DeepSeek, Kimi, Qwen and others use this shape).
 * One request, no streaming, no retries, short timeouts: a failure returns null and the app uses the offline matcher.
 *
 * The key is built into the app from `local.properties`, so it can be extracted from the APK. Use this for a demo
 * build only, and for anything public put the key behind your own server and point [endpoint] at it.
 */
class ChatCompletionsTransport(
    private val endpoint: String,
    private val apiKey: String,
    private val model: String,
    private val timeoutMs: Int = 8_000,
    private val jsonMode: Boolean = true,
    /** "Authorization" sends `Bearer <key>`; any other name (e.g. Azure's "api-key") sends the raw key in that header. */
    private val authHeader: String = "Authorization",
) : AiTransport {
    override suspend fun complete(system: String, user: String): String? = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("model", model)
            put("temperature", 0.2)
            put("max_tokens", 60)
            if (jsonMode) put("response_format", buildJsonObject { put("type", "json_object") })
            put("messages", buildJsonArray {
                add(buildJsonObject { put("role", "system"); put("content", system) })
                add(buildJsonObject { put("role", "user"); put("content", user) })
            })
        }.toString()
        val conn = (URL(endpoint).openConnection() as HttpURLConnection)
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            if (authHeader.equals("Authorization", ignoreCase = true)) conn.setRequestProperty("Authorization", "Bearer $apiKey")
            else conn.setRequestProperty(authHeader, apiKey)
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode !in 200..299) return@withContext null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            extractContent(text)
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        /** choices[0].message.content from a chat-completions response, or null. */
        fun extractContent(responseJson: String): String? = runCatching {
            val root = Json.parseToJsonElement(responseJson) as JsonObject
            val first = (root["choices"] as JsonArray).first() as JsonObject
            ((first["message"] as JsonObject)["content"] as JsonPrimitive).content
        }.getOrNull()
    }
}
