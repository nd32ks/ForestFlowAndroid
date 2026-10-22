package com.chiforest.forestflow

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chiforest.forestflow.auth.AuthResult
import com.chiforest.forestflow.auth.LocalAuthRepository
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.AppContainer
import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.AppViewModel
import com.chiforest.forestflow.core.Escrow
import com.chiforest.forestflow.core.ForestDatabase
import com.chiforest.forestflow.core.Role
import com.chiforest.forestflow.core.Session
import com.chiforest.forestflow.core.UserRow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real on-device backend: SQLite tables, PBKDF2 auth, and state persistence across "restarts". */
@RunWith(AndroidJUnit4::class)
class BackendTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    private val stores = mutableListOf<androidx.lifecycle.ViewModelStore>()

    /** Creates a ViewModel the way an Activity would, so @After can clear (cancel) it. */
    private fun newVm(): AppViewModel {
        var vm: AppViewModel? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val store = androidx.lifecycle.ViewModelStore().also { stores += it }
            vm = androidx.lifecycle.ViewModelProvider.create(store, AppViewModel.Factory(AppContainer(ctx)))[AppViewModel::class.java]
        }
        return vm!!
    }

    @Before fun clean() {
        MainActivity.resetContainer()
        ctx.deleteDatabase(ForestDatabase.NAME)
    }

    @After fun tidy() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { stores.forEach { it.clear() } }
        MainActivity.resetContainer()
        ctx.deleteDatabase(ForestDatabase.NAME)
    }

    @Test fun database_tables_roundtrip() {
        val db = ForestDatabase(ctx)
        assertNull(db.loadState())
        db.saveState("{\"a\":1}", 1L)
        db.saveState("{\"a\":2}", 2L) // single-row upsert
        assertEquals("{\"a\":2}", db.loadState())

        db.log(10L, "Student", "lock", "N01 coil 3")
        db.log(20L, "Student", "release", "Calamansi Lime")
        val logs = db.recentLogs(5)
        assertEquals(2, db.logCount())
        assertEquals("release", logs.first().action) // newest first

        val u = UserRow("uid-1", "Mixed@Case.Test", "Ann", Role.Supplier, "00", "ff", 1, 0L)
        assertTrue(db.insertUser(u))
        assertFalse(db.insertUser(u.copy(uid = "uid-2"))) // unique email
        assertEquals("uid-1", db.findUser("mixed@case.test")?.uid)
        assertEquals(Role.Supplier, db.findUser("MIXED@CASE.TEST")?.role)
        db.close()
    }

    @Test fun local_auth_signin_signup_and_hashing() = runBlocking {
        val db = ForestDatabase(ctx)
        val auth = LocalAuthRepository(db)
        auth.seedDemoAccounts()
        auth.seedDemoAccounts() // idempotent
        assertEquals(2, db.userCount())

        val ok = auth.signIn(LocalAuthRepository.DEMO_STUDENT, LocalAuthRepository.DEMO_PASSWORD)
        assertTrue(ok is AuthResult.Ok)
        assertEquals(Role.Student, (ok as AuthResult.Ok).session.role)
        assertEquals(Role.Supplier, (auth.signIn(LocalAuthRepository.DEMO_SUPPLIER, "forest123") as AuthResult.Ok).session.role)

        assertTrue(auth.signIn(LocalAuthRepository.DEMO_STUDENT, "wrong-pass") is AuthResult.Err)
        assertTrue(auth.signIn("ghost@xjtlu.test", "forest123") is AuthResult.Err)
        assertTrue(auth.signIn("not-an-email", "forest123") is AuthResult.Err)

        val created = auth.signUp("New Runner", "runner@xjtlu.test", "secret77", Role.Supplier)
        assertTrue(created is AuthResult.Ok)
        assertTrue(auth.signUp("Dup", "runner@xjtlu.test", "secret77", Role.Student) is AuthResult.Err)
        assertTrue(auth.signIn("RUNNER@xjtlu.test", "secret77") is AuthResult.Ok)

        // no plain-text passwords anywhere in the users table
        db.readableDatabase.rawQuery("SELECT salt, hash, iterations FROM users", null).use { c ->
            while (c.moveToNext()) {
                assertFalse(c.getString(1).contains("forest123") || c.getString(1).contains("secret77"))
                assertEquals(64, c.getString(1).length) // 256-bit hex
                assertTrue(c.getInt(2) >= 120_000)
            }
        }
        db.close()
    }

    @Test fun state_persists_across_restart_and_escrow_expires_after_reopen() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val session = Session("u", "Persist Test", Role.Student, "cas", guest = false)
        val vm: AppViewModel? = newVm()
        waitUntil { vm!!.ready.value }
        inst.runOnMainSync {
            vm!!.dispatch(Action.LoginSucceeded(session))
            vm!!.dispatch(Action.Save("N05"))
            vm!!.dispatch(Action.Redeem)
            vm!!.dispatch(Action.DoRedeem("d3"))
            vm!!.dispatch(Action.LayerToggle("entrances"))
            vm!!.dispatch(Action.Tab(2))
        }
        // persistence is written on IO right after each change
        waitUntil {
            val raw = ForestDatabase(ctx).use { it.loadState() } ?: return@waitUntil false
            raw.contains("N05") && raw.contains("d3")
        }
        // the activity log is a real table too
        waitUntil { ForestDatabase(ctx).use { it.logCount() } >= 2 }

        // "restart": a brand-new container + ViewModel reading the same database file
        val vm2: AppViewModel? = newVm()
        waitUntil { vm2!!.ready.value }
        val s = vm2!!.state.value
        assertEquals(session, s.session)
        assertTrue("N05" in s.savedNodes)
        assertEquals(420 - 90, s.customer.points)
        assertTrue(s.layers.entrances)
        assertEquals(2, s.customerTab)
        assertNull(s.sheet) // UI-only fields are never persisted

        // escrow with an absolute deadline in the past is cleared on reopen
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
        val past = System.currentTimeMillis() - 10_000
        val expired: AppState = s.copy(customer = s.customer.copy(escrow = Escrow("N01", 3, "Calamansi Lime", past - Escrow.CLAIM_MS, past)))
        ForestDatabase(ctx).use { it.saveState(json.encodeToString(AppState.serializer(), expired), past) }
        val vm3: AppViewModel? = newVm()
        waitUntil { vm3!!.ready.value }
        waitUntil { vm3!!.state.value.customer.escrow == null }
        assertNotNull(vm3!!.state.value.session)
    }

    @Test fun cas_role_mismatch_is_rejected() {
        val opsEmail = "ops${System.currentTimeMillis()}@xjtlu.test"
        val inst = InstrumentationRegistry.getInstrumentation()
        val vm: AppViewModel? = newVm()
        waitUntil { vm!!.ready.value }
        // wait for the demo accounts to be seeded
        waitUntil { ForestDatabase(ctx).use { it.userCount() } >= 2 }
        inst.runOnMainSync {
            vm!!.dispatch(Action.Logout)
            vm!!.dispatch(Action.Agree)
            vm!!.dispatch(Action.SetRole(Role.Student))
            vm!!.casSubmit(LocalAuthRepository.DEMO_SUPPLIER, LocalAuthRepository.DEMO_PASSWORD, false, "")
        }
        waitUntil { vm!!.state.value.casError != null }
        assertTrue(vm!!.state.value.casError!!.contains("Supplier account"))
        assertNull(vm!!.state.value.session)

        inst.runOnMainSync { vm!!.casSubmit(LocalAuthRepository.DEMO_STUDENT, LocalAuthRepository.DEMO_PASSWORD, false, "") }
        waitUntil { vm!!.state.value.session != null }
        assertEquals(Role.Student, vm!!.state.value.session!!.role)

        // create account through the same path
        inst.runOnMainSync {
            vm!!.dispatch(Action.Logout)
            vm!!.dispatch(Action.Agree)
            vm!!.dispatch(Action.SetRole(Role.Supplier))
            vm!!.casSubmit(opsEmail, "restock99", true, "Ops Two")
        }
        waitUntil { vm!!.state.value.session != null }
        assertEquals("Ops Two", vm!!.state.value.session!!.displayName)
        assertEquals(Role.Supplier, vm!!.state.value.session!!.role)
        // when the build talks to Supabase, remove the throw-away cloud account again
        if (vm!!.authBackend == "Supabase") runBlocking { AppContainer(ctx).supabase?.deleteAccount() }
    }

    @Test fun each_account_keeps_its_own_data_across_restarts() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val ann = Session("acct-ann", "Ann", Role.Student, "cas", guest = false)
        val bo = Session("acct-bo", "Bo", Role.Student, "cas", guest = false)
        val vm = newVm()
        waitUntil { vm.ready.value }
        inst.runOnMainSync { vm.signedIn(ann) }
        waitUntil { vm.state.value.session == ann }
        inst.runOnMainSync {
            vm.dispatch(Action.Redeem)
            vm.dispatch(Action.DoRedeem("d3"))
            vm.dispatch(Action.Save("N06"))
        }
        waitUntil { ForestDatabase(ctx).use { db -> db.loadUserState("acct-ann")?.contains("N06") == true } }
        inst.runOnMainSync { vm.dispatch(Action.Logout) }

        inst.runOnMainSync { vm.signedIn(bo) }
        waitUntil { vm.state.value.session == bo }
        assertEquals(420, vm.state.value.customer.points)       // Bo does not see Ann's points
        assertFalse("N06" in vm.state.value.savedNodes)
        inst.runOnMainSync { vm.dispatch(Action.Logout) }

        // "restart" the app, then Ann signs in again and gets her own data back
        val vm2 = newVm()
        waitUntil { vm2.ready.value }
        inst.runOnMainSync { vm2.signedIn(ann) }
        waitUntil { vm2.state.value.session == ann }
        assertEquals(330, vm2.state.value.customer.points)
        assertTrue("N06" in vm2.state.value.savedNodes)
    }

    private fun waitUntil(timeoutMs: Long = 8_000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            var ok = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync { ok = runCatching(cond).getOrDefault(false) }
            if (ok) return
            Thread.sleep(50)
        }
        throw AssertionError("Condition not met within ${timeoutMs}ms")
    }
}
