package com.chiforest.forestflow.auth

import com.chiforest.forestflow.core.Role
import com.chiforest.forestflow.core.Session
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Email/password auth on Firebase (Spark plan). Role lives in Firestore `users/{uid}`.
 * Only constructed when app/google-services.json was present at build time.
 */
class FirebaseAuthRepository : AuthRepository {
    override val backendName = "Firebase"
    private val auth = FirebaseAuth.getInstance()
    private val store = FirebaseFirestore.getInstance()
    private val current = MutableStateFlow<Session?>(null)
    override val session: Flow<Session?> = current

    override suspend fun signIn(email: String, password: String): AuthResult = runCatching {
        AuthRules.validate(email, password)?.let { return AuthResult.Err(it) }
        val user = auth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: return AuthResult.Err("Sign-in failed")
        val doc = store.collection("users").document(user.uid).get().await()
        val role = runCatching { Role.valueOf(doc.getString("role") ?: "Student") }.getOrDefault(Role.Student)
        val s = Session(user.uid, doc.getString("displayName") ?: user.email ?: "User", role, "cas", user.email, guest = false)
        current.value = s
        AuthResult.Ok(s)
    }.getOrElse { AuthResult.Err(it.message ?: "Sign-in failed") }

    override suspend fun signUp(displayName: String, email: String, password: String, role: Role): AuthResult = runCatching {
        AuthRules.validate(email, password)?.let { return AuthResult.Err(it) }
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: return AuthResult.Err("Could not create account")
        store.collection("users").document(user.uid)
            .set(mapOf("displayName" to displayName.trim(), "email" to email.trim(), "role" to role.name)).await()
        val s = Session(user.uid, displayName.trim(), role, "cas", user.email, guest = false)
        current.value = s
        AuthResult.Ok(s)
    }.getOrElse { AuthResult.Err(it.message ?: "Could not create account") }

    override suspend fun signOut() {
        auth.signOut()
        current.value = null
    }
}
