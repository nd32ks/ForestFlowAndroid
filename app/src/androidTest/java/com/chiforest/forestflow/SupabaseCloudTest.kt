package com.chiforest.forestflow

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chiforest.forestflow.auth.AuthResult
import com.chiforest.forestflow.auth.SupabaseApi
import com.chiforest.forestflow.auth.SupabaseBackend
import com.chiforest.forestflow.auth.SupabaseSession
import com.chiforest.forestflow.auth.TokenStore
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.AppContainer
import com.chiforest.forestflow.core.AppViewModel
import com.chiforest.forestflow.core.ForestDatabase
import com.chiforest.forestflow.core.Role
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The cloud backend, against whatever Supabase project the build points at (BuildConfig.SUPABASE_URL):
 * the real project, or `tools/fake_supabase.py` on the host (http://10.0.2.2:54321). Skipped when the build has none.
 */
@RunWith(AndroidJUnit4::class)
class SupabaseCloudTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val inst = InstrumentationRegistry.getInstrumentation()

    private class MemoryTokens : TokenStore {
        var s: SupabaseSession? = null
        override fun load() = s
        override fun save(session: SupabaseSession) { s = session }
        override fun clear() { s = null }
    }

    private fun phone() = SupabaseBackend(SupabaseApi(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY), MemoryTokens())

    @Before fun configured() {
        assumeTrue("Build has no Supabase project", BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank())
        ctx.deleteDatabase(ForestDatabase.NAME)
        ctx.getSharedPreferences("supabase_session", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun the_app_is_wired_to_supabase() {
        assertEquals("Supabase", AppContainer(ctx).auth.backendName)
    }

    @Test fun account_made_on_one_phone_signs_in_on_another() = runBlocking {
        val email = "cloud${System.currentTimeMillis()}@xjtlu.test"
        val a = phone()
        val made = a.signUp("Lin Wei", email, "campus2026", Role.Supplier)
        assertTrue("signUp: $made", made is AuthResult.Ok)
        val b = phone()
        val again = b.signIn(email.uppercase(), "campus2026")
        assertTrue("signIn: $again", again is AuthResult.Ok)
        again as AuthResult.Ok
        assertEquals(Role.Supplier, again.session.role)
        assertEquals("Lin Wei", again.session.displayName)
        assertTrue(b.signIn(email, "wrong-pass") is AuthResult.Err)
        assertTrue(a.signUp("Dup", email, "campus2026", Role.Student) is AuthResult.Err)
        b.deleteAccount()
    }

    @Test fun app_data_follows_the_account_to_a_new_phone() {
        val email = "move${System.currentTimeMillis()}@xjtlu.test"
        val prefs = ctx.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)

        // ---- phone 1: sign up, change some data, sign out
        val c1 = AppContainer(ctx)
        val vm1 = newVm(c1)
        waitUntil { vm1.ready.value }
        inst.runOnMainSync {
            vm1.dispatch(Action.Agree)
            vm1.dispatch(Action.SetRole(Role.Student))
            vm1.casSubmit(email, "restock99", true, "Cloud Ann")
        }
        waitUntil { vm1.state.value.session != null || vm1.state.value.casError != null }
        assertEquals(null, vm1.state.value.casError)
        val uid = vm1.state.value.session!!.uid
        inst.runOnMainSync {
            vm1.dispatch(Action.Redeem)
            vm1.dispatch(Action.DoRedeem("d3"))
            vm1.dispatch(Action.Save("N06"))
        }
        waitUntil(20_000) { runBlocking { c1.supabase!!.pullState(uid) }?.json?.contains("N06") == true }
        inst.runOnMainSync { vm1.dispatch(Action.Logout) }
        Thread.sleep(1_500)

        // ---- a brand-new phone: no local database, no saved session
        c1.db.close()
        ctx.deleteDatabase(ForestDatabase.NAME)
        prefs.edit().clear().commit()
        val c2 = AppContainer(ctx)
        val vm2 = newVm(c2)
        waitUntil { vm2.ready.value }
        inst.runOnMainSync {
            vm2.dispatch(Action.Agree)
            vm2.dispatch(Action.SetRole(Role.Student))
            vm2.casSubmit(email, "restock99", false, "")
        }
        waitUntil { vm2.state.value.session != null || vm2.state.value.casError != null }
        assertEquals(null, vm2.state.value.casError)
        val s = vm2.state.value
        assertEquals("Cloud Ann", s.session!!.displayName)
        assertEquals(330, s.customer.points)                 // 420 − 90 spent on phone 1
        assertTrue("N06" in s.savedNodes)
        assertNotNull(s.ownerUid)

        runBlocking { c2.supabase!!.deleteAccount() }          // leave the project clean
    }

    private fun newVm(c: AppContainer): AppViewModel {
        var vm: AppViewModel? = null
        inst.runOnMainSync { vm = AppViewModel(c) }
        return vm!!
    }

    private fun waitUntil(timeoutMs: Long = 15_000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            var ok = false
            inst.runOnMainSync { ok = runCatching(cond).getOrDefault(false) }
            if (ok) return
            Thread.sleep(100)
        }
        throw AssertionError("Condition not met within ${timeoutMs}ms")
    }
}
