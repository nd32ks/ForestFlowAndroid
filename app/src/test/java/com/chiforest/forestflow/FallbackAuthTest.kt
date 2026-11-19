package com.chiforest.forestflow

import com.chiforest.forestflow.auth.AuthRepository
import com.chiforest.forestflow.auth.AuthResult
import com.chiforest.forestflow.auth.FallbackAuthRepository
import com.chiforest.forestflow.auth.SupabaseBackend
import com.chiforest.forestflow.core.Role
import com.chiforest.forestflow.core.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackAuthTest {
    /** Answers every call with [answer]; counts calls. */
    private class Fake(private val answer: (String) -> AuthResult) : AuthRepository {
        var calls = 0
        override val backendName = "fake"
        override val session: Flow<Session?> = MutableStateFlow(null)
        override suspend fun signIn(email: String, password: String) = answer(email).also { calls++ }
        override suspend fun signUp(displayName: String, email: String, password: String, role: Role) = answer(email).also { calls++ }
        override suspend fun signOut() = Unit
    }

    private fun ok(email: String) = AuthResult.Ok(Session("u1", "Ann", Role.Student, "cas", email, guest = false))
    private val offline = Fake { AuthResult.Err(SupabaseBackend.OFFLINE) }

    @Test fun sign_up_falls_back_to_this_phone_when_the_server_cannot_be_reached() = runBlocking {
        val local = Fake(::ok)
        val r = FallbackAuthRepository(offline, local).signUp("Ann", "ann@xjtlu.edu.cn", "secret1", Role.Student)
        assertTrue(r is AuthResult.Ok)
        assertEquals(1, local.calls)
    }

    @Test fun real_server_answers_are_not_hidden() = runBlocking {
        val cloud = Fake { AuthResult.Err("An account already exists for this email") }
        val local = Fake(::ok)
        val r = FallbackAuthRepository(cloud, local).signUp("Ann", "ann@xjtlu.edu.cn", "secret1", Role.Student)
        assertEquals("An account already exists for this email", (r as AuthResult.Err).message)
        assertEquals(0, local.calls)
    }

    @Test fun cloud_is_used_when_it_works() = runBlocking {
        val cloud = Fake(::ok)
        val local = Fake(::ok)
        FallbackAuthRepository(cloud, local).signIn("ann@xjtlu.edu.cn", "secret1")
        assertEquals(1, cloud.calls)
        assertEquals(0, local.calls)
    }

    @Test fun a_server_that_hangs_falls_back_after_the_wait() = runBlocking {
        val hanging = object : AuthRepository {
            override val backendName = "hang"
            override val session: Flow<Session?> = MutableStateFlow(null)
            override suspend fun signIn(email: String, password: String): AuthResult { kotlinx.coroutines.delay(60_000); return ok(email) }
            override suspend fun signUp(displayName: String, email: String, password: String, role: Role): AuthResult { kotlinx.coroutines.delay(60_000); return ok(email) }
            override suspend fun signOut() = Unit
        }
        val local = Fake(::ok)
        val start = System.currentTimeMillis()
        val r = FallbackAuthRepository(hanging, local, cloudWaitMs = 200).signUp("Ann", "ann@xjtlu.edu.cn", "secret1", Role.Student)
        assertTrue(r is AuthResult.Ok)
        assertTrue(System.currentTimeMillis() - start < 5_000)
    }

    @Test fun offline_sign_in_with_no_account_on_this_phone_explains_both() = runBlocking {
        val local = Fake { AuthResult.Err("No account for $it") }
        val r = FallbackAuthRepository(offline, local).signIn("ann@xjtlu.edu.cn", "secret1") as AuthResult.Err
        assertTrue(r.message.startsWith(SupabaseBackend.OFFLINE))
        assertTrue("No account for ann@xjtlu.edu.cn" in r.message)
    }
}
