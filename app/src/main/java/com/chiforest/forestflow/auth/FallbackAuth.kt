package com.chiforest.forestflow.auth

import com.chiforest.forestflow.core.Role
import com.chiforest.forestflow.core.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Cloud accounts first, the phone's own accounts when the cloud cannot be reached.
 *
 * Testers saw "No connection to the account server" when creating an account: the Supabase project was
 * unreachable, so nobody could sign up. Now sign-up and sign-in fall back to the on-device database
 * whenever the server cannot be reached at all. Real answers from the server (wrong password,
 * e-mail already used) are shown as before. Accounts made offline live only on that phone.
 */
class FallbackAuthRepository(
    private val cloud: AuthRepository,
    private val local: AuthRepository,
    /** How long to wait for the server before using this phone's accounts. With its retries the
     *  server call alone could take about 40 s when unreachable. */
    private val cloudWaitMs: Long = 6_000,
) : AuthRepository {
    override val backendName: String get() = cloud.backendName
    override val session: Flow<Session?> = merge(cloud.session, local.session)

    override suspend fun signIn(email: String, password: String): AuthResult =
        tryCloud { cloud.signIn(email, password) }.orLocal {
            when (val r = local.signIn(email, password)) {
                is AuthResult.Err -> AuthResult.Err("${SupabaseBackend.OFFLINE}. ${r.message} on this phone.")
                is AuthResult.Ok -> r
            }
        }

    override suspend fun signUp(displayName: String, email: String, password: String, role: Role): AuthResult =
        tryCloud { cloud.signUp(displayName, email, password, role) }.orLocal { local.signUp(displayName, email, password, role) }

    override suspend fun signOut() {
        runCatching { cloud.signOut() }
        local.signOut()
    }

    private suspend fun tryCloud(call: suspend () -> AuthResult): AuthResult =
        withTimeoutOrNull(cloudWaitMs) { call() } ?: AuthResult.Err(SupabaseBackend.OFFLINE)

    private inline fun AuthResult.orLocal(fallback: () -> AuthResult): AuthResult =
        if (this is AuthResult.Err && message == SupabaseBackend.OFFLINE) fallback() else this
}
