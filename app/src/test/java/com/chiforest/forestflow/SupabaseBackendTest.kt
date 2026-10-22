package com.chiforest.forestflow

import com.chiforest.forestflow.auth.AuthResult
import com.chiforest.forestflow.auth.LocalAuthRepository
import com.chiforest.forestflow.auth.SupabaseApi
import com.chiforest.forestflow.auth.SupabaseBackend
import com.chiforest.forestflow.auth.SupabaseSession
import com.chiforest.forestflow.auth.TokenStore
import com.chiforest.forestflow.core.Role
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The Supabase client against an in-process fake that speaks the real GoTrue / PostgREST protocol. */
class SupabaseBackendTest {
    private class MemoryTokens : TokenStore {
        var s: SupabaseSession? = null
        override fun load() = s
        override fun save(session: SupabaseSession) { s = session }
        override fun clear() { s = null }
    }

    private lateinit var server: FakeSupabase
    private var now = 1_000_000L
    private lateinit var tokens: MemoryTokens
    private lateinit var backend: SupabaseBackend

    private fun newBackend(t: MemoryTokens = MemoryTokens(), key: String = server.key) =
        SupabaseBackend(SupabaseApi(server.url, key) { now }, t) { now }.also { if (t === tokens) backend = it }

    @Before fun start() {
        server = FakeSupabase()
        tokens = MemoryTokens()
        backend = newBackend(tokens)
    }

    @After fun stop() = server.stop()

    private fun ok(r: AuthResult) = (r as? AuthResult.Ok ?: throw AssertionError("expected Ok, got $r")).session
    private fun err(r: AuthResult) = (r as? AuthResult.Err ?: throw AssertionError("expected Err, got $r")).message

    @Test fun signUp_then_signIn_on_another_phone_keeps_role_and_name() = runBlocking {
        val a = ok(backend.signUp("Lin Wei", "Lin@XJTLU.test", "campus2026", Role.Supplier))
        assertEquals(Role.Supplier, a.role)
        assertEquals("Lin Wei", a.displayName)
        assertFalse(a.guest)
        // a different phone: its own token store, same cloud project
        val phoneB = newBackend(MemoryTokens())
        val b = ok(phoneB.signIn("lin@xjtlu.test", "campus2026"))
        assertEquals(a.uid, b.uid)
        assertEquals(Role.Supplier, b.role)
        assertEquals("Lin Wei", b.displayName)
    }

    @Test fun rejected_credentials_have_friendly_messages() = runBlocking {
        ok(backend.signUp("Ann", "ann@xjtlu.test", "secret77", Role.Student))
        assertEquals("Wrong email or password", err(backend.signIn("ann@xjtlu.test", "nope-nope")))
        assertEquals("Wrong email or password", err(backend.signIn("ghost@xjtlu.test", "secret77")))
        assertEquals("An account already exists for this email", err(backend.signUp("Ann 2", "ANN@xjtlu.test", "secret77", Role.Student)))
        assertEquals("Password is too weak", err(backend.signUp("Weak", "weak@xjtlu.test", "123456", Role.Student)))
        assertTrue(err(backend.signIn("not-an-email", "secret77")).contains("valid email"))
        assertTrue(err(backend.signUp(" ", "x@y.cn", "secret77", Role.Student)).contains("name"))
    }

    @Test fun email_confirmation_switch_is_explained_not_crashed() = runBlocking {
        server.confirmEmail = true
        assertEquals("Check your e-mail to confirm the account, then sign in",
            err(backend.signUp("Cy", "cy@xjtlu.test", "secret77", Role.Student)))
        assertEquals("Confirm your e-mail first, then sign in", err(backend.signIn("cy@xjtlu.test", "secret77")))
    }

    @Test fun demo_accounts_are_created_in_the_cloud_on_first_use() = runBlocking {
        val s = ok(backend.signIn(LocalAuthRepository.DEMO_SUPPLIER, LocalAuthRepository.DEMO_PASSWORD))
        assertEquals(Role.Supplier, s.role)
        assertEquals("Zhang (Ops)", s.displayName)
        assertEquals(1, server.users.size)
        // second time it is a plain sign-in, not another sign-up
        val again = ok(newBackend(MemoryTokens()).signIn(LocalAuthRepository.DEMO_SUPPLIER, LocalAuthRepository.DEMO_PASSWORD))
        assertEquals(s.uid, again.uid)
        assertEquals(1, server.users.size)
        // other e-mails never auto-create
        assertEquals("Wrong email or password", err(backend.signIn("someone@xjtlu.test", LocalAuthRepository.DEMO_PASSWORD)))
    }

    @Test fun each_account_reads_only_its_own_cloud_state() = runBlocking {
        val ann = ok(backend.signUp("Ann", "ann@xjtlu.test", "secret77", Role.Student))
        val annState = """{"points":330,"saved":["N06"]}"""
        assertTrue(backend.pushState(ann.uid, annState, 111))
        assertTrue(backend.pushState(ann.uid, """{"points":300}""", 222))   // upsert, not a second row
        assertEquals(1, server.states.size)

        val pulled = backend.pullState(ann.uid)
        assertNotNull(pulled)
        assertEquals("""{"points":300}""", pulled!!.json)
        assertEquals(222L, pulled.clientTs)

        val phoneB = newBackend(MemoryTokens())
        val bo = ok(phoneB.signUp("Bo", "bo@xjtlu.test", "secret77", Role.Student))
        assertNull(phoneB.pullState(bo.uid))                 // Bo has nothing yet
        assertNull(phoneB.pullState(ann.uid))                // and cannot read Ann's row (token/uid mismatch)
        assertFalse(phoneB.pushState(ann.uid, """{"hack":1}""", 1))   // nor write it
        assertEquals("""{"points":300}""", backend.pullState(ann.uid)!!.json)
    }

    @Test fun expired_access_token_is_refreshed_before_use() = runBlocking {
        val ann = ok(backend.signUp("Ann", "ann@xjtlu.test", "secret77", Role.Student))
        val firstToken = tokens.s!!.accessToken
        now += 3_599_000                    // 1 s before expiry: inside the 60 s safety margin
        assertTrue(backend.pushState(ann.uid, """{"a":1}""", 5))
        assertTrue(tokens.s!!.accessToken != firstToken)
        assertTrue(server.requests.any { it.path.startsWith("/auth/v1/token?grant_type=refresh_token") })
        assertEquals(server.requests.last().bearer, tokens.s!!.accessToken)
    }

    @Test fun revoked_refresh_token_ends_the_cloud_session_quietly() = runBlocking {
        val ann = ok(backend.signUp("Ann", "ann@xjtlu.test", "secret77", Role.Student))
        server.revokeAllRefreshTokens()
        now += 7_200_000
        assertFalse(backend.pushState(ann.uid, """{"a":1}""", 5))
        assertNull(tokens.s)
        assertFalse(backend.hasSession(ann.uid))
    }

    @Test fun project_url_pasted_with_a_rest_suffix_still_works() = runBlocking {
        val pasted = SupabaseBackend(SupabaseApi(server.url + "/rest/v1/", server.key) { now }, MemoryTokens()) { now }
        assertEquals("Pasty", ok(pasted.signUp("Pasty", "pasty@xjtlu.test", "secret77", Role.Student)).displayName)
    }

    @Test fun wrong_project_key_is_reported() = runBlocking {
        val bad = newBackend(MemoryTokens(), key = "not-the-key")
        assertTrue(err(bad.signIn("ann@xjtlu.test", "secret77")).contains("Supabase key"))
    }

    @Test fun a_transient_server_error_is_retried_once() = runBlocking {
        server.failWith500 = 1
        val s = ok(backend.signUp("Dee", "dee@xjtlu.test", "secret77", Role.Student))
        assertEquals("Dee", s.displayName)
    }

    @Test fun unreachable_server_gives_a_connection_message() = runBlocking {
        server.stop()
        assertEquals("No connection to the account server", err(backend.signIn("ann@xjtlu.test", "secret77")))
    }

    @Test fun activity_sign_out_and_account_deletion() = runBlocking {
        val ann = ok(backend.signUp("Ann", "ann@xjtlu.test", "secret77", Role.Student))
        backend.logActivity(ann.uid, "Student", "lock", "N01 coil 3")
        assertEquals(1, server.activity.size)
        assertEquals("lock", server.activity[0]["action"]!!.toString().trim('"'))

        backend.pushState(ann.uid, """{"a":1}""", 1)
        backend.signOut()
        assertNull(tokens.s)
        assertFalse(backend.hasSession(ann.uid))
        assertNull(backend.pullState(ann.uid))               // signed out: no cloud calls

        ok(backend.signIn("ann@xjtlu.test", "secret77"))
        backend.deleteAccount()
        assertTrue(server.users.isEmpty())
        assertTrue(server.states.isEmpty())
        assertTrue(server.activity.isEmpty())
        assertEquals("Wrong email or password", err(backend.signIn("ann@xjtlu.test", "secret77")))
    }
}
