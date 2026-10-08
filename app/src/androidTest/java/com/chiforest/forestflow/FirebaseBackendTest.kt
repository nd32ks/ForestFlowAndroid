package com.chiforest.forestflow

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chiforest.forestflow.auth.AuthResult
import com.chiforest.forestflow.auth.FirebaseAuthRepository
import com.chiforest.forestflow.auth.LocalAuthRepository
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.AppContainer
import com.chiforest.forestflow.core.AppViewModel
import com.chiforest.forestflow.core.Role
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The shared (cross-device) backend. Runs only when the app was built with a google-services.json.
 * Locally it is exercised against the Firebase Emulator Suite (project "demo-forestflow"): two
 * repository instances stand in for two different phones talking to the same cloud.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseBackendTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Before fun connect() {
        val app = runCatching { FirebaseApp.getApps(ctx).firstOrNull() ?: FirebaseApp.initializeApp(ctx) }.getOrNull()
        assumeTrue("Built without google-services.json", app != null)
        val host = InstrumentationArgs.emulatorHost
        if (host != null) {
            runCatching { FirebaseAuth.getInstance().useEmulator(host, 9099) }
            runCatching { FirebaseFirestore.getInstance().useEmulator(host, 8080) }
        }
    }

    @Test fun app_uses_firebase_when_configured() {
        assertEquals("Firebase", AppContainer(ctx).auth.backendName)
    }

    @Test fun account_created_on_one_phone_signs_in_on_another() = runBlocking {
        val email = "student${System.currentTimeMillis()}@xjtlu.test"
        val phoneA = FirebaseAuthRepository()
        val created = phoneA.signUp("Lin Wei", email, "campus2026", Role.Supplier)
        assertTrue("signUp: $created", created is AuthResult.Ok)
        phoneA.signOut()

        val phoneB = FirebaseAuthRepository()
        val again = phoneB.signIn(email.uppercase(), "campus2026")
        assertTrue("signIn: $again", again is AuthResult.Ok)
        again as AuthResult.Ok
        assertEquals(Role.Supplier, again.session.role)      // role read back from Firestore users/{uid}
        assertEquals("Lin Wei", again.session.displayName)
        phoneB.signOut()

        assertTrue(phoneB.signIn(email, "wrong-pass") is AuthResult.Err)
        assertTrue(phoneB.signUp("Dup", email, "campus2026", Role.Student) is AuthResult.Err)
        assertTrue(phoneB.signIn("nobody${System.nanoTime()}@xjtlu.test", "campus2026") is AuthResult.Err)
    }

    @Test fun app_cas_sheet_creates_and_signs_in_cloud_account() {
        val inst = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val email = "ops${System.currentTimeMillis()}@xjtlu.test"
        fun newVm(): AppViewModel {
            var vm: AppViewModel? = null
            inst.runOnMainSync {
                vm = androidx.lifecycle.ViewModelProvider.create(androidx.lifecycle.ViewModelStore(), AppViewModel.Factory(AppContainer(ctx)))[AppViewModel::class.java]
            }
            return vm!!
        }
        fun waitUntil(cond: () -> Boolean) {
            val end = System.currentTimeMillis() + 15_000
            while (System.currentTimeMillis() < end) {
                var ok = false
                inst.runOnMainSync { ok = cond() }
                if (ok) return
                Thread.sleep(100)
            }
            throw AssertionError("timeout")
        }
        val a = newVm()
        waitUntil { a.ready.value }
        inst.runOnMainSync {
            a.dispatch(Action.Logout)
            a.dispatch(Action.Agree)
            a.dispatch(Action.SetRole(Role.Supplier))
            a.casSubmit(email, "restock99", true, "Cloud Ops")
        }
        waitUntil { a.state.value.session != null || a.state.value.casError != null }
        assertEquals(null, a.state.value.casError)
        assertEquals("Cloud Ops", a.state.value.session!!.displayName)
        inst.runOnMainSync { a.dispatch(Action.Logout) }

        // a second app instance (fresh container) signs in with the same cloud account
        val b = newVm()
        waitUntil { b.ready.value }
        inst.runOnMainSync {
            b.dispatch(Action.Agree)
            b.dispatch(Action.SetRole(Role.Supplier))
            b.casSubmit(email, "restock99", false, "")
        }
        waitUntil { b.state.value.session != null || b.state.value.casError != null }
        val st = b.state.value
        assertEquals("casError=${st.casError} busy=${st.casBusy} session=${st.session}", null, st.casError)
        assertEquals(Role.Supplier, st.session!!.role)
        assertEquals("Cloud Ops", b.state.value.session!!.displayName)
        inst.runOnMainSync { b.dispatch(Action.Logout) }
    }

    @Test fun demo_accounts_work_on_every_phone() = runBlocking {
        val first = FirebaseAuthRepository().signIn(LocalAuthRepository.DEMO_SUPPLIER, LocalAuthRepository.DEMO_PASSWORD)
        assertTrue("$first", first is AuthResult.Ok)
        assertEquals(Role.Supplier, (first as AuthResult.Ok).session.role)
        FirebaseAuth.getInstance().signOut()
        val second = FirebaseAuthRepository().signIn(LocalAuthRepository.DEMO_SUPPLIER, LocalAuthRepository.DEMO_PASSWORD)
        assertEquals(Role.Supplier, (second as AuthResult.Ok).session.role)
        FirebaseAuth.getInstance().signOut()
    }
}

/** `-Pandroid.testInstrumentationRunnerArguments.firebaseEmulator=10.0.2.2` points the test at a local emulator. */
object InstrumentationArgs {
    val emulatorHost: String?
        get() = androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("firebaseEmulator")
}
