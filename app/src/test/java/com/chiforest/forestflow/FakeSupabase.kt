package com.chiforest.forestflow

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-process stand-in for a Supabase project: the parts of GoTrue (`/auth/v1`) and PostgREST (`/rest/v1`)
 * the app uses, with the same JSON shapes and error codes, and Row Level Security emulated by token.
 */
class FakeSupabase(private val apiKey: String = "test-anon-key") {
    class User(val id: String, val email: String, var password: String, val metadata: JsonObject, var confirmed: Boolean)
    class Req(val method: String, val path: String, val bearer: String?)

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val url: String get() = "http://127.0.0.1:${server.address.port}"
    val key: String get() = apiKey

    val users = mutableListOf<User>()
    val states = mutableMapOf<String, Pair<JsonElement, Long>>()
    val activity = mutableListOf<JsonObject>()
    val requests = mutableListOf<Req>()
    private val access = mutableMapOf<String, String>()   // access token -> user id
    private val refresh = mutableMapOf<String, String>()  // refresh token -> user id
    private val seq = AtomicInteger(1)

    var confirmEmail = false        // "Confirm email" switch of the Auth settings
    var expiresInSeconds = 3600L
    var failWith500 = 0             // next N requests answer 500

    private val json = Json { ignoreUnknownKeys = true }

    init {
        server.createContext("/") { ex ->
            synchronized(this) {
                runCatching { handle(ex) }.onFailure { reply(ex, 500, buildJsonObject { put("message", it.toString()) }) }
            }
        }
        server.start()
    }

    fun stop() = server.stop(0)

    fun revokeAllRefreshTokens() = refresh.clear()

    private fun tokensFor(u: User): JsonObject {
        val n = seq.getAndIncrement()
        val at = "at-$n"; val rt = "rt-$n"
        access[at] = u.id; refresh[rt] = u.id
        return buildJsonObject {
            put("access_token", at)
            put("token_type", "bearer")
            put("expires_in", expiresInSeconds)
            put("refresh_token", rt)
            put("user", userJson(u))
        }
    }

    private fun userJson(u: User) = buildJsonObject {
        put("id", u.id)
        put("email", u.email)
        put("user_metadata", u.metadata)
    }

    private fun err(ex: HttpExchange, status: Int, code: String, msg: String) =
        reply(ex, status, buildJsonObject { put("code", status.toLong()); put("error_code", code); put("msg", msg) })

    private fun reply(ex: HttpExchange, status: Int, body: JsonElement?) {
        val bytes = (body?.toString() ?: "").toByteArray()
        ex.responseHeaders.add("Content-Type", "application/json")
        if (bytes.isEmpty()) ex.sendResponseHeaders(status, -1) else ex.sendResponseHeaders(status, bytes.size.toLong())
        if (bytes.isNotEmpty()) ex.responseBody.use { it.write(bytes) }
        ex.close()
    }

    private fun handle(ex: HttpExchange) {
        val path = ex.requestURI.path
        val query = ex.requestURI.rawQuery.orEmpty()
        val bearer = ex.requestHeaders.getFirst("Authorization")?.removePrefix("Bearer ")
        requests += Req(ex.requestMethod, path + if (query.isEmpty()) "" else "?$query", bearer)
        val bodyText = ex.requestBody.readBytes().toString(Charsets.UTF_8)
        val body = if (bodyText.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(bodyText)

        if (ex.requestHeaders.getFirst("apikey") != apiKey) return reply(ex, 401, buildJsonObject { put("message", "Invalid API key") })
        if (failWith500 > 0) { failWith500--; return err(ex, 500, "unexpected_failure", "Database error") }

        val userId = bearer?.let { access[it] }
        fun str(k: String) = (body as? JsonObject)?.get(k)?.jsonPrimitive?.contentOrNull

        when {
            path == "/auth/v1/signup" -> {
                val email = str("email")!!.lowercase()
                val pw = str("password")!!
                if (pw.length < 6 || pw in listOf("123456", "password")) return err(ex, 422, "weak_password", "Password should be at least 6 characters.")
                if (users.any { it.email == email }) return err(ex, 422, "user_already_exists", "User already registered")
                val u = User("uid-${seq.getAndIncrement()}", email, pw, (body as JsonObject)["data"]?.jsonObject ?: JsonObject(emptyMap()), !confirmEmail)
                users += u
                if (confirmEmail) return reply(ex, 200, userJson(u))   // no session until the e-mail is confirmed
                reply(ex, 200, tokensFor(u))
            }
            path == "/auth/v1/token" && query.contains("grant_type=password") -> {
                val u = users.firstOrNull { it.email == str("email")!!.lowercase() && it.password == str("password") }
                    ?: return err(ex, 400, "invalid_credentials", "Invalid login credentials")
                if (!u.confirmed) return err(ex, 400, "email_not_confirmed", "Email not confirmed")
                reply(ex, 200, tokensFor(u))
            }
            path == "/auth/v1/token" && query.contains("grant_type=refresh_token") -> {
                val id = refresh.remove(str("refresh_token"))
                    ?: return err(ex, 400, "refresh_token_not_found", "Invalid Refresh Token: Refresh Token Not Found")
                reply(ex, 200, tokensFor(users.first { it.id == id }))
            }
            path == "/auth/v1/logout" -> { access.remove(bearer); reply(ex, 204, null) }
            path.startsWith("/rest/v1/") -> {
                if (userId == null) return reply(ex, 401, buildJsonObject { put("code", "PGRST301"); put("message", "JWT expired or invalid") })
                when {
                    path == "/rest/v1/user_state" && ex.requestMethod == "POST" -> {
                        val row = body as JsonObject
                        if (row["user_id"]!!.jsonPrimitive.content != userId) return reply(ex, 403, buildJsonObject { put("code", "42501"); put("message", "new row violates row-level security policy") })
                        states[userId] = row["state"]!! to (row["client_ts"]?.jsonPrimitive?.longOrNull ?: 0L)
                        reply(ex, 201, null)
                    }
                    path == "/rest/v1/user_state" && ex.requestMethod == "GET" -> {
                        val wanted = Regex("user_id=eq\\.([^&]+)").find(query)?.groupValues?.get(1)?.let { URLDecoder.decode(it, "UTF-8") }
                        val row = states[userId]?.takeIf { wanted == userId }
                        reply(ex, 200, JsonArray(listOfNotNull(row?.let { (s, ts) -> buildJsonObject { put("state", s); put("client_ts", ts) } })))
                    }
                    path == "/rest/v1/activity_log" && ex.requestMethod == "POST" -> {
                        activity += buildJsonObject { put("user_id", userId); (body as JsonObject).forEach { (k, v) -> put(k, v) } }
                        reply(ex, 201, null)
                    }
                    path == "/rest/v1/rpc/delete_my_account" -> {
                        users.removeAll { it.id == userId }; states.remove(userId); activity.removeAll { it["user_id"]!!.jsonPrimitive.content == userId }
                        access.entries.removeAll { it.value == userId }; refresh.entries.removeAll { it.value == userId }
                        reply(ex, 204, null)
                    }
                    else -> reply(ex, 404, buildJsonObject { put("message", "no such route $path") })
                }
            }
            else -> reply(ex, 404, buildJsonObject { put("message", "no such route $path") })
        }
    }
}
