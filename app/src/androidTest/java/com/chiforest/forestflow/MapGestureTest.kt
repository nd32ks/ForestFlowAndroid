package com.chiforest.forestflow

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.ForestDatabase
import com.chiforest.forestflow.core.Role
import com.chiforest.forestflow.core.Session
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real multi-touch on the campus map: double-tap, pinch in/out, pan, and that drags never escape the image. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MapGestureTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before fun launch() {
        org.junit.Assume.assumeTrue("map is switched off in the demo scope", BuildConfig.FEATURE_CAMPUS_MAP)
        MainActivity.resetContainer()
        ctx.deleteDatabase(ForestDatabase.NAME)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        rule.waitUntil(8_000) { state().let { true } }
        scenario.onActivity {
            it.vm.dispatch(Action.LoginSucceeded(Session("t", "Gesture Test", Role.Student, "cas", guest = false)))
            it.vm.dispatch(Action.Tab(1))
        }
        rule.waitUntil(10_000) { rule.onAllNodes(androidx.compose.ui.test.hasTestTag("campusMap")).fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
    }

    @After fun close() {
        if (::scenario.isInitialized) scenario.close()
        MainActivity.resetContainer()
        ctx.deleteDatabase(ForestDatabase.NAME)
    }

    private fun state(): AppState {
        lateinit var s: AppState
        scenario.onActivity { s = it.vm.state.value }
        return s
    }

    private fun camZoom(): Float? = state().cameras["tc"]?.zoom

    private fun settle() {
        rule.mainClock.advanceTimeBy(1500)
        rule.waitForIdle()
        Thread.sleep(800)
        rule.waitForIdle()
    }

    @Test fun double_tap_zooms_in_then_back_to_fit() {
        rule.onNodeWithTag("campusMap").performTouchInput { doubleClick(center) }
        rule.waitUntil(5_000) { (camZoom() ?: 0f) > 2.4f }
        assertEquals(2.5f, camZoom()!!, 0.05f)
        rule.onNodeWithTag("campusMap").performTouchInput { doubleClick(center) }
        rule.waitUntil(5_000) { (camZoom() ?: 9f) < 1.05f }
    }

    @Test fun pinch_out_zooms_in_and_pinch_in_zooms_out_clamped() {
        rule.onNodeWithTag("campusMap").performTouchInput {
            pinch(center + Offset(-20f, 0f), center + Offset(-260f, 0f), center + Offset(20f, 0f), center + Offset(260f, 0f), durationMillis = 400)
        }
        rule.waitUntil(5_000) { (camZoom() ?: 0f) > 2.5f }
        val zoomedIn = camZoom()!!
        assertTrue(zoomedIn <= 4.0f + 1e-3f) // clamped to maxZoom after the rubber-band settles
        rule.onNodeWithTag("campusMap").performTouchInput {
            pinch(center + Offset(-300f, 0f), center + Offset(-10f, 0f), center + Offset(300f, 0f), center + Offset(10f, 0f), durationMillis = 400)
        }
        rule.waitUntil(5_000) { (camZoom() ?: 9f) < zoomedIn }
        assertTrue(camZoom()!! >= 1f - 1e-3f) // never smaller than cover-fit
    }

    @Test fun pan_moves_camera_and_clamps_to_image() {
        rule.onNodeWithTag("campusMap").performTouchInput { doubleClick(center) }
        rule.waitUntil(5_000) { (camZoom() ?: 0f) > 2.4f }
        val before = state().cameras["tc"]!!
        rule.onNodeWithTag("campusMap").performTouchInput { swipe(center, center + Offset(-250f, 0f), 300) }
        rule.waitUntil(5_000) { state().cameras["tc"]!!.cx > before.cx + 0.01f }
        // a huge fling far past the edge must settle inside the image
        rule.onNodeWithTag("campusMap").performTouchInput { swipe(centerLeft, centerRight + Offset(4000f, 0f), 60) }
        settle()
        val cam = state().cameras["tc"]!!
        assertTrue("cx ${cam.cx} out of range", cam.cx in 0f..1f)
        assertTrue(cam.cy in 0f..1f)
        // the page did not scroll away: the search field is still on screen
        rule.onAllNodes(hasText("Drink, machine or building…", substring = true), useUnmergedTree = true).onFirst().assertExists()
    }
}
