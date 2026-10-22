package com.chiforest.forestflow.auth

import android.content.Context
import androidx.core.content.edit
import com.chiforest.forestflow.core.Role
import com.chiforest.forestflow.core.Session
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException

/** The app's saved state as stored in the cloud (`user_state`). */
data class RemoteState(val json: String, val clientTs: Long)

/** Cloud copy of the per-account data. [NoRemote] when the app runs offline-only. */
interface RemoteStore {
    val enabled: Boolean
    /** True when a valid cloud session exists for this account (guest and offline sessions have none). */
    fun hasSession(uid: String): Boolean
    suspend fun pullState(uid: String): RemoteState?
    suspend fun pushState(uid: String, stateJson: String, clientTs: Long): Boolean
    suspend fun logActivity(uid: String, role: String, action: String, detail: String)
}

object NoRemote : RemoteStore {
    override val enabled = false
    override fun hasSession(uid: String) = false
    override suspend fun pullState(uid: String): RemoteState? = null
    override suspend fun pushState(uid: String, stateJson: String, clientTs: Long) = false
    override suspend fun logActivity(uid: String, role: String, action: String, detail: String) = Unit
}

/** Where the cloud session (tokens) is kept between launches. */
interface TokenStore {
    fun load(): SupabaseSession?
    fun save(session: SupabaseSession)
    fun clear()
}

/** App-private SharedPreferences (excluded from backups, see res/xml/backup_rules.xml). */
class PrefsTokenStore(context: Context) : TokenStore {
    private val prefs = context.applicationContext.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): SupabaseSession? = prefs.getString("session", null)
        ?.let { runCatching { json.decodeFromString(SupabaseSession.serializer(), it) }.getOrNull() }

    override fun save(session: SupabaseSession) {
        prefs.edit { putString("session", json.encodeToString(SupabaseSession.serializer(), session)) }
    }

    override fun clear() {
        prefs.edit { remove("session") }
    }
}

/**
 * Accounts and cloud data on Supabase: Auth (e-mail + password) and the Postgres tables in supabase/schema.sql.
 * Role and name travel in the user's `user_metadata` (`role`, `display_name`).
 */
class SupabaseBackend(
    private val api: SupabaseApi,
    private val tokens: TokenStore,
    private val clock: () -> Long = System::currentTimeMillis,
) : AuthRepository, RemoteStore {
    override val backendName = "Supabase"
    override val enabled = true
    private val current = MutableStateFlow<Session?>(null)
    override val session: Flow<Session?> = current

    // ---------------------------------------------------------------- AuthRepository

    override suspend fun signIn(email: String, password: String): AuthResult {
        AuthRules.validate(email, password)?.let { return AuthResult.Err(it) }
        val s = try {
            retrying { api.signIn(email.trim(), password) }
        } catch (e: Exception) {
            // The two published demo accounts are created in the cloud on first use, so they work on every phone.
            val demo = demoRole(email, password) ?: return AuthResult.Err(friendly(e))
            if (e is SupabaseException && e.code != "invalid_credentials") return AuthResult.Err(friendly(e))
            return signUp(demoName(demo), email, password, demo)
        }
        return accept(s, fallbackRole = demoRole(email, password))
    }

    override suspend fun signUp(displayName: String, email: String, password: String, role: Role): AuthResult {
        AuthRules.validate(email, password)?.let { return AuthResult.Err(it) }
        if (displayName.isBlank()) return AuthResult.Err("Enter your name")
        val meta = buildJsonObject {
            put("role", role.name)
            put("display_name", displayName.trim())
        }
        return try {
            when (val r = retrying { api.signUp(email.trim(), password, meta) }) {
                is SignUpResult.Signed -> accept(r.session, fallbackRole = role)
                SignUpResult.ConfirmationRequired ->
                    AuthResult.Err("Check your e-mail to confirm the account, then sign in")
            }
        } catch (e: Exception) {
            AuthResult.Err(friendly(e))
        }
    }

    override suspend fun signOut() {
        val s = tokens.load()
        tokens.clear()
        current.value = null
        if (s != null) runCatching { api.signOut(s.accessToken) }
    }

    /** Deletes the signed-in account and its cloud data (server function `delete_my_account`). */
    suspend fun deleteAccount() {
        val token = validToken() ?: return
        api.deleteMyAccount(token)
        tokens.clear()
        current.value = null
    }

    private fun accept(s: SupabaseSession, fallbackRole: Role?): AuthResult {
        val role = s.metadata["role"]?.jsonPrimitive?.contentOrNull?.let { r -> runCatching { Role.valueOf(r) }.getOrNull() }
            ?: fallbackRole
            ?: return AuthResult.Err("This account has no Forest Flow role")
        val name = s.metadata["display_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: s.email ?: "User"
        tokens.save(s)
        val session = Session(s.userId, name, role, "cas", s.email, guest = false)
        current.value = session
        return AuthResult.Ok(session)
    }

    // ---------------------------------------------------------------- RemoteStore

    override fun hasSession(uid: String): Boolean = tokens.load()?.userId == uid

    override suspend fun pullState(uid: String): RemoteState? {
        val token = validToken(uid) ?: return null
        return api.fetchUserState(token, uid)?.let { (json, ts) -> RemoteState(json, ts) }
    }

    override suspend fun pushState(uid: String, stateJson: String, clientTs: Long): Boolean {
        val token = validToken(uid) ?: return false
        val element = Json.parseToJsonElement(stateJson)
        retrying { api.upsertUserState(token, uid, element, clientTs) }
        return true
    }

    override suspend fun logActivity(uid: String, role: String, action: String, detail: String) {
        val token = validToken(uid) ?: return
        api.insertActivity(token, role, action, detail)
    }

    /** The access token for [uid] (or the signed-in user), refreshed when it is about to expire. Null if none. */
    private suspend fun validToken(uid: String? = null): String? {
        val s = tokens.load() ?: return null
        if (uid != null && s.userId != uid) return null
        if (s.expiresAtMs - clock() > 60_000) return s.accessToken
        return try {
            val fresh = api.refresh(s.refreshToken)
            tokens.save(fresh)
            fresh.accessToken
        } catch (e: SupabaseException) {
            if (e.status in 400..401) tokens.clear() // the refresh token was revoked or already used
            null
        } catch (e: IOException) {
            null // offline: keep the session, try again later
        }
    }

    // ---------------------------------------------------------------- helpers

    /** One retry after a short pause for transient failures (network blips, 5xx). */
    private suspend fun <T> retrying(block: suspend () -> T): T = try {
        block()
    } catch (e: IOException) {
        delay(1_200)
        block()
    } catch (e: SupabaseException) {
        if (e.status >= 500) {
            delay(1_200)
            block()
        } else {
            throw e
        }
    }

    private fun demoRole(email: String, password: String): Role? = when {
        password != LocalAuthRepository.DEMO_PASSWORD -> null
        email.trim().equals(LocalAuthRepository.DEMO_STUDENT, true) -> Role.Student
        email.trim().equals(LocalAuthRepository.DEMO_SUPPLIER, true) -> Role.Supplier
        else -> null
    }

    private fun demoName(role: Role) = if (role == Role.Student) "Demo Student" else "Zhang (Ops)"

    private fun friendly(e: Exception): String = when (e) {
        is SupabaseException -> when {
            e.code == "invalid_credentials" || "invalid login credentials" in e.message.lowercase() -> "Wrong email or password"
            e.code == "user_already_exists" || "already registered" in e.message.lowercase() -> "An account already exists for this email"
            e.code == "email_not_confirmed" -> "Confirm your e-mail first, then sign in"
            e.code == "weak_password" -> "Password is too weak"
            e.code?.contains("rate_limit") == true || e.status == 429 -> "Too many attempts, try again in a minute"
            e.code == "signup_disabled" || e.code == "email_provider_disabled" -> "Email sign-up is switched off in Supabase"
            e.status == 401 || e.status == 403 -> "The Supabase key is not accepted (check supabase.anonKey)"
            else -> e.message.ifBlank { "Sign-in failed" }
        }
        is IOException -> "No connection to the account server"
        else -> e.message?.ifBlank { null } ?: "Sign-in failed"
    }
}
