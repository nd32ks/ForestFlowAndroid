package com.chiforest.forestflow

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chiforest.forestflow.core.ForestDatabase
import com.chiforest.forestflow.BuildConfig
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end: drives the real app (real SQLite, real reducer, real Compose UI) through every
 * student and supplier feature, asserting what appears on screen after each tap.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun launch() {
        MainActivity.resetContainer()
        ctx.deleteDatabase(ForestDatabase.NAME)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun close() {
        scenario?.close()
        MainActivity.resetContainer()
        ctx.deleteDatabase(ForestDatabase.NAME)
    }

    // ------------------------------------------------------------ helpers
    private fun text(t: String, substring: Boolean = false): SemanticsNodeInteraction {
        rule.waitUntil(8_000) { rule.onAllNodes(hasText(t, substring = substring), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        return rule.onAllNodes(hasText(t, substring = substring), useUnmergedTree = true).onFirst()
    }

    private fun see(t: String, substring: Boolean = false) {
        text(t, substring)
    }

    private fun gone(t: String, substring: Boolean = false) {
        rule.waitUntil(8_000) { rule.onAllNodes(hasText(t, substring = substring), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
    }

    /** Click the nearest clickable ancestor via its semantics action (immune to overlays like the nav pill). */
    private fun tap(t: String, substring: Boolean = false) {
        rule.waitUntil(8_000) { clickableWith(t, substring) != null }
        val node = clickableWith(t, substring)!!
        runCatching { node.performScrollTo() }
        node.performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
    }

    private fun clickableWith(t: String, substring: Boolean): SemanticsNodeInteraction? {
        val m = hasText(t, substring = substring) or hasContentDescription(t, substring = substring)
        val direct = rule.onAllNodes(m.and(androidx.compose.ui.test.hasClickAction()))
        if (direct.fetchSemanticsNodes().isNotEmpty()) return direct.onFirst()
        val parent = rule.onAllNodes(androidx.compose.ui.test.hasClickAction().and(androidx.compose.ui.test.hasAnyDescendant(m)))
        val nodes = parent.fetchSemanticsNodes()
        if (nodes.isEmpty()) return null
        // the innermost clickable that contains the text: the one with the fewest descendants
        val idx = nodes.indices.minBy { i -> countDesc(nodes[i]) }
        return parent[idx]
    }

    private fun countDesc(n: androidx.compose.ui.semantics.SemanticsNode): Int = n.children.sumOf { 1 + countDesc(it) }

    private fun nav(label: String) {
        rule.waitUntil(8_000) {
            rule.onAllNodes(hasContentDescription(label, substring = true).and(androidx.compose.ui.test.hasClickAction())).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodes(hasContentDescription(label, substring = true).and(androidx.compose.ui.test.hasClickAction())).onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
    }

    private fun agreeAndLogin(supplier: Boolean) {
        see("元气森林 Chi Forest")
        if (supplier) tap("补货供货商 Supplier")
        // blocked without agreement
        tap(if (supplier) "微信登录 · Supplier WeChat" else "微信一键登录 · WeChat Login")
        see("Please accept the user agreement first")
        tap("我已阅读", substring = true)
        tap(if (supplier) "微信登录 · Supplier WeChat" else "微信一键登录 · WeChat Login")
    }

    // ------------------------------------------------------------ tests

    @Test fun sms_login_validation_and_code_flow() {
        see("元气森林 Chi Forest")
        tap("我已阅读", substring = true)
        tap("登录并解锁冷柜 / Sign In")
        see("Enter a valid 11-digit mobile number")
        rule.onAllNodes(hasContentDescription("Mobile number")).onFirst().performTextInput("13800138000")
        tap("获取验证码")
        see("Code sent", substring = true)
        // demo code auto-fills after 0.9s, countdown shows seconds
        see("482915")
        tap("登录并解锁冷柜 / Sign In")
        see("Welcome", substring = true)
        see("TRAY COIL INVENTORY")
    }

    @Test fun cas_login_with_database_account() {
        tap("我已阅读", substring = true)
        tap("XJTLU CAS")
        see("Demo account · tap to fill")
        tap("Demo account · tap to fill")
        tap("Sign in")
        see("Welcome, Demo Student", substring = true)
        see("TRAY COIL INVENTORY")
    }

    @Test fun student_full_flow() {
        assumeTrue("needs every feature switched on", BuildConfig.FEATURE_VIP_CLUB && BuildConfig.FEATURE_CAMPUS_MAP)
        agreeAndLogin(supplier = false)
        see("TRAY COIL INVENTORY")

        // ---- Inventory
        see("1 Lock in Escrow")
        tap("Release")
        gone("1 Lock in Escrow")
        tap("Lock")
        see("Coil #03 locked", substring = true)
        see("1 Lock in Escrow")
        tap("Notify Me")
        see("Subscribed")
        tap("I'm Here")
        see("Checked in at Node 01", substring = true)

        // ---- Map: search, select, campus switch, directions, layers, bookmark, view stock
        nav("Campus Map")
        see("Search nodes or buildings…")
        rule.onAllNodes(hasContentDescription("Search nodes or buildings")).onFirst().performTextInput("library")
        see("Library 1F Hub", substring = true)
        tap("Node 03")
        see("Chi Forest Smart Node 03")
        tap("Directions")
        see("Directions started", substring = true)
        see("Clear Route")
        see("5 min walk") // chip drawn on the map above the pins
        tap("Bookmark node")
        see("Node 03 saved")
        tap("Remove bookmark")
        see("Node 03 removed from saved")
        tap("Map layers")
        see("Map Layers")
        tap("Entrances")
        tap("Close")
        tap("Reset map view")
        tap("Locate me")
        see("Located you near Bldg G", substring = true)
        tap("High Stock")
        tap("SIP 园区")
        see("Chi Forest Smart Node 07")
        tap("View Stock")
        see("STATION · NODE 07", substring = true)

        // ---- Notifications
        nav("Notifications")
        see("Restock & Drops")
        see("Osmanthus Plum Sparkling")
        tap("Mark All Read")
        see("All caught up")
        tap("Check Live Status")
        see("restocking today 14:30", substring = true)
        tap("Escrow & Drops")
        see("URGENT DROP ALERT")
        tap("Dismiss")
        gone("URGENT DROP ALERT")

        // ---- VIP
        nav("VIP Pass")
        see("TIME REMAINING TO CLAIM")
        tap("COPY")
        see("CF-9204", substring = true)
        tap("Redeem Points for Pilot Drops")
        see("Pilot Drops")
        tap("90 pts")
        see("Redeemed Oolong Zero Trial", substring = true)
        tap("Close")
        see("330 pts")

        // ---- Profile: activity log comes from SQLite, then sign out
        tap("Profile")
        see("RECENT ACTIVITY · activity_log")
        see("redeem", substring = true)
        tap("Sign out")
        see("元气森林 Chi Forest")
    }

    @Test fun supplier_full_flow() {
        agreeAndLogin(supplier = true)
        see("Coil Tray Matrix")

        // ---- Telemetry
        see("42%")
        tap("C3 Osmanthus Plum, 0 of 15")
        see("0 cans left · 18 queued · Depleted at 13:41")
        tap("Restock")
        see("C3 restocked · 15/15")
        see("Full")
        tap("14:00: 120 cans")
        see("120 PEAK")
        tap("Dispatch Runner")
        see("Dispatch ticket sent to Zhang")

        // ---- Dispatch
        see("Node 01 Restock Routine")
        tap("Call")
        see("Calling Zhang", substring = true)
        tap("Approve Price Cut")
        see("Price cut live · Barley ¥4.40")
        tap("Simulate Foot-Traffic Surge")
        see("Surge simulated", substring = true)
        tap("Confirm Restock Loaded")
        see("Restock Confirmed")
        see("Node 01 restocked to 100%")

        // ---- Alerts (badge cleared on open)
        nav("Alerts")
        see("Live Alert Gateway")
        see("No urgent alerts · all coils stocked")
        tap("Restocks", substring = true)
        see("Coil C3 Restocked")
        tap("Alert gateway live")
        see("Alert gateway muted")

        // ---- Reset demo data keeps session
        tap("Profile")
        tap("Reset demo data")
        see("Demo data reset")
        nav("Telemetry")
        see("0 cans left · 18 queued · Depleted at 13:41", substring = true)
    }

    @Test fun student_secondary_actions() {
        assumeTrue("needs every feature switched on", BuildConfig.FEATURE_VIP_CLUB && BuildConfig.FEATURE_CAMPUS_MAP)
        agreeAndLogin(supplier = false)
        see("TRAY COIL INVENTORY")

        // Inventory: Route and coil Navigate both open the map on the active node
        tap("Route")
        see("You're already at Node 01")
        see("Chi Forest Smart Node 01")
        nav("Inventory")
        tap("Navigate")
        see("Coil #01 · Node 01 highlighted on map")

        // Map: pin tap, building filter sheet, layers with labels, stations list
        tap("Node 05,", substring = true)
        see("Chi Forest Smart Node 05")
        tap("Bldg …")
        see("Buildings")
        tap("Bldg A · A栋")
        see("Bldg A")
        tap("All Stations (6)", substring = true)
        tap("Map layers")
        tap("Entrances")
        tap("Points of interest")
        tap("Close")
        see("East Gate 东门")
        see("Mini Hub · 西浦小屋")
        rule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Search nodes or buildings")).onFirst().performTextInput("node")
        tap("All stations list")
        see("All Stations")
        tap("Node 09 · FB Atrium")
        see("Chi Forest Smart Node 09")

        // Notifications: switches, preferences, unsubscribe → empty state, hold & re-order
        nav("Notifications")
        tap("Push App Alert")
        see("App alerts off")
        tap("SMS Drop")
        see("SMS drop alerts on")
        tap("Quiet Campus Hours")
        tap("Unsubscribe")
        see("No restock alerts yet")
        tap("Navigate & Hold")
        see("Routing to Node 02")
        see("Chi Forest Smart Node 02")
        nav("Notifications")
        tap("Re-order Now")
        see("You already hold a coil")
        see("TIME REMAINING TO CLAIM")

        // VIP: enlarge QR, then release the escrow and see the empty state
        tap("QR code for chiforest://escrow", substring = true)
        see("Scan at the machine")
        tap("Close")
        nav("Inventory")
        tap("Release")
        nav("VIP Pass")
        see("No active escrow")
        nav("Notifications")
        tap("Re-order Now")
        see("Re-ordered · Coil #03 locked for you")
        see("TIME REMAINING TO CLAIM")

        // Sign out, then the Visitor path
        tap("Profile")
        tap("Sign out")
        tap("我已阅读", substring = true)
        tap("访客 / 教职工")
        see("Welcome, Campus Visitor", substring = true)
    }

    @Test fun supplier_secondary_actions() {
        agreeAndLogin(supplier = true)
        see("Coil Tray Matrix")
        nav("Dispatch")
        tap("Ping")
        see("Zhang pinged", substring = true)
        see("En Route")
        tap("Dismiss")
        gone("Barley")
        see("Electro")
        nav("Alerts")
        tap("Ping Runner")
        see("Runner Zhang pinged")
        see("Runner Pinged")
        tap("Stockouts", substring = true)
        see("Coil C3 Empty")
        gone("VIP Escrow")
        tap("Clearance", substring = true)
        see("Clearance −20% Approved")
    }

    /** Share leaves the app (system share sheet), so the outgoing intent itself is captured and checked. */
    @Test fun share_sends_station_text_and_link() {
        androidx.test.espresso.intent.Intents.init()
        try {
            androidx.test.espresso.intent.Intents.intending(
                androidx.test.espresso.intent.matcher.IntentMatchers.hasAction(android.content.Intent.ACTION_CHOOSER),
            ).respondWith(android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_OK, null))
            agreeAndLogin(supplier = false)
            see("TRAY COIL INVENTORY")
            tap("Share")
            androidx.test.espresso.intent.Intents.intended(
                org.hamcrest.Matchers.allOf(
                    androidx.test.espresso.intent.matcher.IntentMatchers.hasAction(android.content.Intent.ACTION_CHOOSER),
                    androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra(
                        org.hamcrest.Matchers.equalTo(android.content.Intent.EXTRA_INTENT),
                        org.hamcrest.Matchers.allOf(
                            androidx.test.espresso.intent.matcher.IntentMatchers.hasAction(android.content.Intent.ACTION_SEND),
                            androidx.test.espresso.intent.matcher.IntentMatchers.hasType("text/plain"),
                            androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra(
                                org.hamcrest.Matchers.equalTo(android.content.Intent.EXTRA_TEXT),
                                org.hamcrest.Matchers.allOf(
                                    org.hamcrest.Matchers.containsString("Chi Forest Smart Node 01"),
                                    org.hamcrest.Matchers.containsString("chiforest://node/N01"),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        } finally {
            androidx.test.espresso.intent.Intents.release()
        }
    }

    /** The demo build: login page + Feature 1 (supplier) + Feature 2 (student restock radar); the rest is hidden. */
    @Test fun demo_scope_shows_login_and_two_features_only() {
        assumeTrue("demo scope only", !BuildConfig.FEATURE_VIP_CLUB && !BuildConfig.FEATURE_CAMPUS_MAP)
        agreeAndLogin(supplier = false)
        see("TRAY COIL INVENTORY")
        // student navigation: Inventory + Notifications only
        rule.onAllNodes(hasContentDescription("Inventory", substring = true).and(androidx.compose.ui.test.hasClickAction())).assertCountEquals(1)
        rule.onAllNodes(hasContentDescription("Notifications", substring = true).and(androidx.compose.ui.test.hasClickAction())).assertCountEquals(1)
        rule.onAllNodes(hasContentDescription("VIP Pass")).assertCountEquals(0)
        rule.onAllNodes(hasContentDescription("Campus Map")).assertCountEquals(0)
        // no map, escrow or VIP controls on the inventory
        gone("Route"); gone("I'm Here"); gone("Lock"); gone("Navigate"); gone("Other Nodes"); gone("1 Lock in Escrow")
        // Feature 2: Notify Me works
        tap("Notify Me")
        see("Subscribed")
        nav("Notifications")
        see("Restock & Drops")
        see("URGENT DROP ALERT")
        see("Osmanthus Plum Sparkling")
        gone("Escrow & Drops"); gone("VIP Privileges"); gone("ESCROW TIMEOUT"); gone("CAMPUS PASS PERKS"); gone("Navigate & Hold")
        tap("Unsubscribe")
        tap("Mark All Read")
        see("All caught up")
        // Feature 1: the supplier still has all three tabs
        tap("Profile")
        tap("Sign out")
        tap("补货供货商 Supplier")
        tap("我已阅读", substring = true)
        tap("微信登录 · Supplier WeChat")
        see("Coil Tray Matrix")
        nav("Dispatch")
        see("Node 01 Restock Routine")
        nav("Alerts")
        see("Live Alert Gateway")
    }
}
