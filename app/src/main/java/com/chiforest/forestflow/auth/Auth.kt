package com.chiforest.forestflow.auth

import com.chiforest.forestflow.core.ForestDatabase
import com.chiforest.forestflow.core.Role
import com.chiforest.forestflow.core.Session
import com.chiforest.forestflow.core.UserRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface AuthResult {
    data class Ok(val session: Session) : AuthResult
    data class Err(val message: String) : AuthResult
}

interface AuthRepository {
    val backendName: String
    val session: Flow<Session?>
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(displayName: String, email: String, password: String, role: Role): AuthResult
    suspend fun signOut()
}

/** PBKDF2WithHmacSHA256 with a random 16-byte salt. Plain-text passwords are never stored or logged. */
object Passwords {
    const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private val rng = SecureRandom()

    fun newSalt(): String = ByteArray(16).also(rng::nextBytes).toHex()

    fun hash(password: String, saltHex: String, iterations: Int = ITERATIONS): String {
        val spec = PBEKeySpec(password.toCharArray(), saltHex.hexToBytes(), iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.toHex()
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(password: String, saltHex: String, expectedHex: String, iterations: Int): Boolean {
        val actual = hash(password, saltHex, iterations)
        if (actual.length != expectedHex.length) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].code xor expectedHex[i].code)
        return diff == 0
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes() = ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

object AuthRules {
    val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    fun validate(email: String, password: String): String? = when {
        !emailRegex.matches(email.trim()) -> "Enter a valid email address"
        password.length < 6 -> "Password must be at least 6 characters"
        else -> null
    }
}

/** Offline accounts in the on-device SQLite `users` table. Default backend. */
class LocalAuthRepository(private val db: ForestDatabase) : AuthRepository {
    override val backendName = "On-device SQLite"
    private val current = MutableStateFlow<Session?>(null)
    override val session: Flow<Session?> = current

    suspend fun seedDemoAccounts() = withContext(Dispatchers.IO) {
        if (db.findUser(DEMO_STUDENT) == null) create("Demo Student", DEMO_STUDENT, DEMO_PASSWORD, Role.Student)
        if (db.findUser(DEMO_SUPPLIER) == null) create("Zhang (Ops)", DEMO_SUPPLIER, DEMO_PASSWORD, Role.Supplier)
    }

    private fun create(name: String, email: String, password: String, role: Role): UserRow {
        val salt = Passwords.newSalt()
        val row = UserRow(
            uid = UUID.randomUUID().toString(), email = email.trim().lowercase(), displayName = name, role = role,
            salt = salt, hash = Passwords.hash(password, salt), iterations = Passwords.ITERATIONS,
            createdAt = System.currentTimeMillis(),
        )
        db.insertUser(row)
        return row
    }

    override suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        AuthRules.validate(email, password)?.let { return@withContext AuthResult.Err(it) }
        val u = db.findUser(email) ?: return@withContext AuthResult.Err("No account for ${email.trim()}")
        if (!Passwords.verify(password, u.salt, u.hash, u.iterations)) return@withContext AuthResult.Err("Wrong password")
        val s = Session(u.uid, u.displayName, u.role, "cas", u.email, guest = false)
        current.value = s
        AuthResult.Ok(s)
    }

    override suspend fun signUp(displayName: String, email: String, password: String, role: Role): AuthResult =
        withContext(Dispatchers.IO) {
            AuthRules.validate(email, password)?.let { return@withContext AuthResult.Err(it) }
            if (displayName.isBlank()) return@withContext AuthResult.Err("Enter your name")
            if (db.findUser(email) != null) return@withContext AuthResult.Err("An account already exists for ${email.trim()}")
            val u = create(displayName.trim(), email, password, role)
            val s = Session(u.uid, u.displayName, u.role, "cas", u.email, guest = false)
            current.value = s
            AuthResult.Ok(s)
        }

    override suspend fun signOut() {
        current.value = null
    }

    companion object {
        const val DEMO_STUDENT = "student@xjtlu.test"
        const val DEMO_SUPPLIER = "supplier@xjtlu.test"
        const val DEMO_PASSWORD = "forest123"
    }
}
