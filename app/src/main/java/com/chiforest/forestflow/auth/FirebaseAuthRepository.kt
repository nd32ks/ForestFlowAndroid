package com.chiforest.forestflow.auth

import com.chiforest.forestflow.core.Role
import com.chiforest.forestflow.core.Session
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Email/password accounts shared by every phone (Firebase Authentication, Spark plan).
 *
 * Role and name live in two places:
 *  - the Auth profile (`displayName = "<Role>|<Name>"`): needs no database rules, so accounts work on a
 *    brand-new project where only Email/Password sign-in is enabled;
 *  - Firestore `users/{uid}`, also written when the project's security rules allow it (best effort).
 */
class FirebaseAuthRepository : AuthRepository {
    override val backendName = "Firebase"
    private val auth = FirebaseAuth.getInstance()
    private val store = FirebaseFirestore.getInstance()
    private val current = MutableStateFlow<Session?>(null)
    override val session: Flow<Session?> = current

    override suspend fun signIn(email: String, password: String): AuthResult {
        AuthRules.validate(email, password)?.let { return AuthResult.Err(it) }
        val user = try {
            retrying { auth.signInWithEmailAndPassword(email.trim(), password).await().user }
        } catch (e: Exception) {
            // The two published demo accounts are created in the cloud on first use, so they work on every phone.
            val demo = demoRole(email, password) ?: return AuthResult.Err(friendly(e))
            return signUp(demoName(demo), email, password, demo)
        } ?: return AuthResult.Err("Sign-in failed")

        val (role, name) = profileOf(user, email, password)
            ?: return AuthResult.Err("This account has no Forest Flow role")
        val s = Session(user.uid, name, role, "cas", user.email, guest = false)
        current.value = s
        return AuthResult.Ok(s)
    }

    override suspend fun signUp(displayName: String, email: String, password: String, role: Role): AuthResult {
        AuthRules.validate(email, password)?.let { return AuthResult.Err(it) }
        if (displayName.isBlank()) return AuthResult.Err("Enter your name")
        val user = try {
            retrying { auth.createUserWithEmailAndPassword(email.trim(), password).await().user }
        } catch (e: Exception) {
            return AuthResult.Err(friendly(e))
        } ?: return AuthResult.Err("Could not create account")
        try {
            retrying { user.updateProfile(userProfileChangeRequest { this.displayName = "${role.name}|${displayName.trim()}" }).await() }
        } catch (e: Exception) {
            // roll back: an account without a role would be unusable
            runCatching { user.delete().await() }
            return AuthResult.Err(friendly(e))
        }
        runCatching {
            withTimeoutOrNull(4_000) {
                store.collection("users").document(user.uid)
                    .set(mapOf("displayName" to displayName.trim(), "email" to email.trim(), "role" to role.name)).await()
            }
        }
        val s = Session(user.uid, displayName.trim(), role, "cas", user.email, guest = false)
        current.value = s
        return AuthResult.Ok(s)
    }

    override suspend fun signOut() {
        auth.signOut()
        current.value = null
    }

    /** Firestore first (if rules allow), then the Auth profile, then the published demo accounts. */
    private suspend fun profileOf(user: FirebaseUser, email: String, password: String): Pair<Role, String>? {
        val doc = runCatching { withTimeoutOrNull(4_000) { store.collection("users").document(user.uid).get().await() } }.getOrNull()
        val docRole = doc?.getString("role")?.let { r -> runCatching { Role.valueOf(r) }.getOrNull() }
        if (docRole != null) return docRole to (doc.getString("displayName") ?: user.email ?: "User")
        user.displayName?.split("|", limit = 2)?.takeIf { it.size == 2 }?.let { (r, n) ->
            runCatching { Role.valueOf(r) }.getOrNull()?.let { return it to n }
        }
        return demoRole(email, password)?.let { it to demoName(it) }
    }

    /** One retry, after a short pause, for transient server errors ("service is currently unavailable"). */
    private suspend fun <T> retrying(block: suspend () -> T): T = try {
        block()
    } catch (e: Exception) {
        val m = e.message.orEmpty()
        if ("unavailable" in m.lowercase() || "internal error" in m.lowercase() || "network" in m.lowercase()) {
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

    private fun friendly(e: Exception): String {
        val m = e.message.orEmpty()
        return when {
            "no user record" in m || "INVALID_LOGIN_CREDENTIALS" in m || "credential is incorrect" in m ||
                "password is invalid" in m -> "Wrong email or password"
            "already in use" in m -> "An account already exists for this email"
            "network" in m.lowercase() -> "No connection to the account server"
            "CONFIGURATION_NOT_FOUND" in m || "OPERATION_NOT_ALLOWED" in m -> "Email sign-in is not enabled in Firebase"
            else -> m.ifBlank { "Sign-in failed" }
        }
    }
}
