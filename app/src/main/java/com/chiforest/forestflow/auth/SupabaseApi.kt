package com.chiforest.forestflow.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

/** An error answered by Supabase (GoTrue or PostgREST). [code] is GoTrue's `error_code` / PostgREST's `code`. */
class SupabaseException(val status: Int, val code: String?, override val message: String) : Exception(message)

@Serializable
data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMs: Long,
    val userId: String,
    val email: String? = null,
    val metadata: JsonObject = JsonObject(emptyMap()),
)

sealed interface SignUpResult {
    data class Signed(val session: SupabaseSession) : SignUpResult
    /** The project requires e-mail confirmation: the account exists but there is no session yet. */
    data object ConfirmationRequired : SignUpResult
}

/**
 * Minimal Supabase client over plain HTTPS (no SDK): Auth (GoTrue) and the Data API (PostgREST).
 * Every call sends the project's anon / publishable key as `apikey`, plus the user's access token as `Authorization`
 * when there is one, so Postgres Row Level Security sees the signed-in user.
 */
class SupabaseApi(baseUrl: String, private val apiKey: String, private val clock: () -> Long = System::currentTimeMillis) {
    // Accept the project URL as pasted from the dashboard, with or without a trailing /rest/v1 or /auth/v1.
    private val base = baseUrl.trim().trimEnd('/').removeSuffix("/rest/v1").removeSuffix("/auth/v1").trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }

    // ---------------------------------------------------------------- Auth

    suspend fun signUp(email: String, password: String, data: JsonObject): SignUpResult {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
            put("data", data)
        }
        val res = call("POST", "/auth/v1/signup", body)
        val obj = parse(res.body) as? JsonObject ?: JsonObject(emptyMap())
        return parseSession(obj)?.let { SignUpResult.Signed(it) } ?: SignUpResult.ConfirmationRequired
    }

    suspend fun signIn(email: String, password: String): SupabaseSession {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
        }
        return sessionOrThrow(call("POST", "/auth/v1/token?grant_type=password", body))
    }

    suspend fun refresh(refreshToken: String): SupabaseSession =
        sessionOrThrow(call("POST", "/auth/v1/token?grant_type=refresh_token", buildJsonObject { put("refresh_token", refreshToken) }))

    suspend fun signOut(accessToken: String) {
        call("POST", "/auth/v1/logout", null, accessToken)
    }

    // ---------------------------------------------------------------- Data API

    /** Insert-or-update the caller's one `user_state` row (the table's primary key is `user_id`). */
    suspend fun upsertUserState(accessToken: String, userId: String, state: JsonElement, clientTs: Long) {
        val row = buildJsonObject {
            put("user_id", userId)
            put("state", state)
            put("client_ts", clientTs)
        }
        call(
            "POST", "/rest/v1/user_state?on_conflict=user_id", row, accessToken,
            mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        )
    }

    /** The caller's saved state, or null if none. Returns (state JSON, client timestamp). */
    suspend fun fetchUserState(accessToken: String, userId: String): Pair<String, Long>? {
        val res = call("GET", "/rest/v1/user_state?user_id=eq.$userId&select=state,client_ts", null, accessToken)
        val row = (parse(res.body) as? JsonArray)?.firstOrNull()?.jsonObject ?: return null
        val state = row["state"] ?: return null
        return state.toString() to (row["client_ts"]?.jsonPrimitive?.longOrNull ?: 0L)
    }

    suspend fun insertActivity(accessToken: String, role: String, action: String, detail: String) {
        val row = buildJsonObject {
            put("role", role)
            put("action", action)
            put("detail", detail)
        }
        call("POST", "/rest/v1/activity_log", row, accessToken, mapOf("Prefer" to "return=minimal"))
    }

    /** Server-side function `delete_my_account()` (see supabase/schema.sql): removes the caller and all their rows. */
    suspend fun deleteMyAccount(accessToken: String) {
        call("POST", "/rest/v1/rpc/delete_my_account", JsonObject(emptyMap()), accessToken)
    }

    // ---------------------------------------------------------------- plumbing

    private class Response(val status: Int, val body: String)

    private suspend fun call(
        method: String,
        path: String,
        body: JsonElement?,
        bearer: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): Response = withContext(Dispatchers.IO) {
        val conn = (URL(base + path).openConnection() as HttpURLConnection)
        try {
            conn.requestMethod = method
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("apikey", apiKey)
            conn.setRequestProperty("Authorization", "Bearer ${bearer ?: apiKey}")
            conn.setRequestProperty("Accept", "application/json")
            extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = conn.responseCode
            val text = (if (status in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw toException(status, text)
            Response(status, text)
        } finally {
            conn.disconnect()
        }
    }

    private fun toException(status: Int, text: String): SupabaseException {
        val o = parse(text) as? JsonObject
        fun s(k: String) = o?.get(k)?.takeIf { it !is JsonNull }?.jsonPrimitive?.contentOrNull
        val msg = s("msg") ?: s("error_description") ?: s("message") ?: s("error") ?: "HTTP $status"
        return SupabaseException(status, s("error_code") ?: s("code") ?: s("error"), msg)
    }

    private fun parse(text: String): JsonElement? = runCatching { json.parseToJsonElement(text) }.getOrNull()

    private fun sessionOrThrow(res: Response): SupabaseSession =
        parseSession(parse(res.body) as? JsonObject ?: JsonObject(emptyMap()))
            ?: throw SupabaseException(res.status, "no_session", "The server did not return a session")

    private fun parseSession(o: JsonObject): SupabaseSession? {
        val access = o["access_token"]?.jsonPrimitive?.contentOrNull ?: return null
        val refresh = o["refresh_token"]?.jsonPrimitive?.contentOrNull ?: return null
        val user = o["user"] as? JsonObject ?: return null
        val id = user["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val expiresIn = o["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L
        return SupabaseSession(
            accessToken = access,
            refreshToken = refresh,
            expiresAtMs = clock() + expiresIn * 1000,
            userId = id,
            email = user["email"]?.jsonPrimitive?.contentOrNull,
            metadata = (user["user_metadata"] as? JsonObject) ?: JsonObject(emptyMap()),
        )
    }
}

