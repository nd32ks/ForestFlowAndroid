package com.chiforest.forestflow

import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.Effect
import com.chiforest.forestflow.core.Escrow
import com.chiforest.forestflow.core.MapData
import com.chiforest.forestflow.core.MapFocus
import com.chiforest.forestflow.core.Reducer
import com.chiforest.forestflow.core.Role
import com.chiforest.forestflow.core.Seed
import com.chiforest.forestflow.core.Session
import com.chiforest.forestflow.core.SheetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReducerTest {
    private val map = MapData.parse(File("src/main/assets/maps/map_data.json").readText())
    private val r = Reducer(map)
    private val t0 = 1_000_000L
    private val student = Session("u1", "Test Student", Role.Student, "cas", guest = false)
    private val supplier = Session("u2", "Zhang", Role.Supplier, "cas", guest = false)
    private fun fresh(s: Session? = student) = Seed.fresh(t0, s)
    private fun AppState.go(vararg a: Action): AppState = a.fold(this) { st, act -> r.reduce(st, act).state }
    private fun effects(s: AppState, a: Action) = r.reduce(s, a).effects

    // ---------------- login
    @Test fun role_switches_login_role() = assertEquals(Role.Supplier, fresh(null).go(Action.SetRole(Role.Supplier)).loginRole)

    @Test fun agree_toggles() {
        val s = fresh(null).copy(agreed = false)
        assertTrue(s.go(Action.Agree).agreed)
        assertFalse(s.go(Action.Agree, Action.Agree).agreed)
    }

    @Test fun login_blocked_without_agreement_shakes_agree() {
        val s = fresh(null).copy(agreed = false).go(Action.Login("wechat"))
        assertEquals("agree", s.shakeField)
        assertNull(s.loggingIn)
    }

    @Test fun visitor_requires_agreement() {
        assertEquals("agree", fresh(null).copy(agreed = false).go(Action.Visitor).shakeField)
        assertEquals("visitor", fresh(null).copy(agreed = true).go(Action.Visitor).loggingIn)
    }

    @Test fun sms_validates_phone_then_code() {
        val base = fresh(null).copy(agreed = true)
        assertEquals("phone", base.go(Action.Phone("123"), Action.Login("sms")).shakeField)
        assertEquals("code", base.go(Action.Phone("13800138000"), Action.Code("12"), Action.Login("sms")).shakeField)
        assertEquals("code", base.go(Action.Phone("13800138000"), Action.Code("111111"), Action.Login("sms")).shakeField)
        val ok = base.go(Action.Phone("13800138000"), Action.Code(Reducer.DEMO_CODE), Action.Login("sms"))
        assertEquals("sms", ok.loggingIn)
    }

    @Test fun getCode_starts_countdown_and_autofill() {
        val s = fresh(null).go(Action.Phone("13800138000"))
        val n = r.reduce(s, Action.GetCode(t0))
        assertEquals(60, n.state.codeLeftSec(t0))
        assertTrue(n.effects.contains(Effect.ScheduleAutofill))
        assertEquals(Reducer.DEMO_CODE, n.state.go(Action.CodeAutofill).smsCode)
        // second tap during countdown is ignored
        assertEquals(n.state, n.state.go(Action.GetCode(t0 + 5_000)))
    }

    @Test fun cas_opens_sheet() = assertEquals(SheetKind.Cas, fresh(null).copy(agreed = true).go(Action.Login("cas")).sheet)

    @Test fun loginSucceeded_enters_role_and_logs() {
        val n = r.reduce(fresh(null), Action.LoginSucceeded(supplier))
        assertEquals(Role.Supplier, n.state.session?.role)
        assertTrue(n.effects.any { it is Effect.Log })
    }

    @Test fun each_account_sees_only_its_own_data() {
        val other = Session("u9", "Other Student", Role.Student, "cas", guest = false)
        // student redeems, signs out, another account signs in → fresh data, not the first student's
        val a = fresh().go(Action.Redeem, Action.DoRedeem("d3"), Action.Logout)
        assertEquals(330, a.customer.points)
        val b = a.go(Action.LoginSucceeded(other, null, t0))
        assertEquals(420, b.customer.points)
        assertEquals("u9", b.ownerUid)
        // the same account signing back in (no saved copy passed) keeps what is on screen
        val again = b.go(Action.Logout, Action.LoginSucceeded(other, null, t0))
        assertEquals(420, again.customer.points)
        // a saved copy for the account wins
        val back = again.go(Action.Logout, Action.LoginSucceeded(student, a, t0))
        assertEquals(330, back.customer.points)
        assertEquals(student, back.session)
    }

    @Test fun logout_clears_session() = assertNull(fresh().go(Action.Logout).session)

    @Test fun reset_restores_seed_but_keeps_session() {
        val s = fresh().go(Action.Release, Action.Redeem, Action.DoRedeem("d1")).go(Action.Reset(t0))
        assertEquals(student, s.session)
        assertEquals(420, s.customer.points)
        assertNotNull(s.customer.escrow)
    }

    // ---------------- shell
    @Test fun tab_changes_and_alerts_tab_clears_badge() {
        assertEquals(2, fresh().go(Action.Tab(2)).customerTab)
        val sup = fresh(supplier)
        assertEquals(2, sup.aBadge)
        assertEquals(0, sup.go(Action.Tab(2)).aBadge)
    }

    @Test fun back_closes_sheet_then_returns_to_first_tab() {
        val s = fresh().go(Action.Tab(3), Action.Profile)
        val a = s.go(Action.Back)
        assertNull(a.sheet)
        assertEquals(3, a.customerTab)
        assertEquals(0, a.go(Action.Back).customerTab)
    }

    @Test fun profile_opens_sheet() = assertEquals(SheetKind.Profile, fresh().go(Action.Profile).sheet)

    // ---------------- inventory
    @Test fun lock_creates_escrow_on_coil_3_or_toasts_if_exists() {
        val s = fresh().go(Action.Release)
        val locked = s.go(Action.Quick("Lock", t0))
        assertEquals(3, locked.customer.escrow?.coil)
        assertEquals(t0 + Escrow.CLAIM_MS, locked.customer.escrow?.expiresAt)
        assertTrue(effects(locked, Action.Quick("Lock", t0)).any { it is Effect.Toast })
    }

    @Test fun release_clears_escrow() = assertNull(fresh().go(Action.Release).customer.escrow)

    @Test fun imHere_sets_you_are_here() {
        val s = fresh().copy(activeNodeId = "N04").go(Action.Quick("I'm Here", t0))
        assertEquals("N04", s.youAreHereNodeId)
    }

    @Test fun route_opens_map_with_directions() {
        val s = fresh().copy(activeNodeId = "N05").go(Action.Quick("Route", t0))
        assertEquals(1, s.customerTab)
        assertEquals("N05", s.selectedNodeId)
        assertEquals("N05", s.directionsTo)
        assertTrue(s.mapFocus is MapFocus.Node)
    }

    @Test fun share_emits_share_effect() = assertTrue(effects(fresh(), Action.Quick("Share", t0)).any { it is Effect.ShareText })

    @Test fun notify_toggles_osmanthus_watch() {
        assertTrue(fresh().go(Action.Notify).customer.osmanthusWatch)
        assertFalse(fresh().go(Action.Notify, Action.Notify).customer.osmanthusWatch)
    }

    @Test fun coilNav_opens_map() = assertEquals(1, fresh().go(Action.CoilNav(1)).customerTab)

    @Test fun escrow_expires_on_tick() {
        val s = fresh()
        assertNotNull(s.go(Action.Tick(t0 + 1000)).customer.escrow)
        assertNull(s.go(Action.Tick(t0 + Escrow.CLAIM_MS)).customer.escrow)
    }

    // ---------------- map
    @Test fun pin_on_other_campus_switches_campus() {
        val s = fresh().go(Action.Pin("N08"))
        assertEquals("sip", s.campusId)
        assertEquals("N08", s.selectedNodeId)
    }

    @Test fun campus_switch_selects_a_node_on_that_campus() {
        val s = fresh().go(Action.Campus("sip"))
        assertEquals("sip", s.campusId)
        assertTrue(map.campus("sip").nodes.any { it.id == s.selectedNodeId })
    }

    @Test fun filters_and_search_combine() {
        val n01 = map.node("N01")!!
        val n03 = map.node("N03")!!
        assertTrue(Reducer.matches(n01, "chilled", null, "", map))
        assertFalse(Reducer.matches(n03, "chilled", null, "", map))
        assertTrue(Reducer.matches(n01, "high", null, "", map))
        assertFalse(Reducer.matches(n01, "all", "D", "", map))
        assertTrue(Reducer.matches(n03, "all", null, "library", map))
        assertFalse(Reducer.matches(n01, "all", null, "library", map))
        assertEquals("high", fresh().go(Action.MapFilter("high")).mapFilter)
        assertEquals("G", fresh().go(Action.BuildingFilter("G")).buildingFilter)
        assertEquals("cb", fresh().go(Action.Search("cb")).search)
    }

    @Test fun searchBuilding_pulses_and_focuses() {
        val s = fresh().go(Action.SearchBuilding("tc", "H"))
        assertEquals("H", s.pulseBuilding)
        assertTrue(s.mapFocus is MapFocus.Building)
    }

    @Test fun locate_and_compass_emit_focus() {
        assertTrue(fresh().go(Action.Locate).mapFocus is MapFocus.Locate)
        assertTrue(fresh().go(Action.Compass).mapFocus is MapFocus.Compass)
        assertNull(fresh().go(Action.Compass, Action.FocusConsumed).mapFocus)
    }

    @Test fun layers_toggle() {
        val s = fresh().go(Action.LayerToggle("entrances"), Action.LayerToggle("poi"), Action.LayerToggle("dim"), Action.LayerToggle("nodes"))
        assertTrue(s.layers.entrances)
        assertTrue(s.layers.poi)
        assertFalse(s.layers.dimDark)
        assertFalse(s.layers.nodes)
    }

    @Test fun save_toggles_bookmark() {
        assertTrue("N02" in fresh().go(Action.Save("N02")).savedNodes)
        assertFalse("N02" in fresh().go(Action.Save("N02"), Action.Save("N02")).savedNodes)
    }

    @Test fun directions_toggle_and_replace() {
        val s = fresh().go(Action.Pin("N05"), Action.Directions)
        assertEquals("N05", s.directionsTo)
        assertEquals("N04", s.go(Action.Pin("N04")).directionsTo)
        assertNull(s.go(Action.Directions).directionsTo)
    }

    @Test fun directions_refused_across_campuses() = assertNull(fresh().go(Action.Pin("N08"), Action.Directions).directionsTo)

    @Test fun viewStock_sets_active_node_and_inventory_tab() {
        val s = fresh().go(Action.Tab(1), Action.Pin("N02"), Action.ViewStock)
        assertEquals("N02", s.activeNodeId)
        assertEquals(0, s.customerTab)
    }

    @Test fun saveCamera_persists_per_campus() {
        val cam = com.chiforest.forestflow.core.Camera(2f, 0.4f, 0.5f)
        assertEquals(cam, fresh().go(Action.SaveCamera("tc", cam)).cameras["tc"])
    }

    // ---------------- notifications
    @Test fun nf_readAll_dismiss_unread() {
        val s = fresh()
        assertEquals(3, s.unread)
        assertEquals("vip", s.go(Action.Nf("vip")).customer.nf)
        assertEquals(0, s.go(Action.ReadAll).unread)
        assertEquals(2, s.go(Action.Dismiss("perks")).unread)
    }

    @Test fun unsub_and_alert_switches() {
        val s = fresh().go(Action.Notify, Action.Unsub("osmanthus"), Action.Unsub("peach"))
        assertFalse(s.customer.osmanthusWatch)
        assertFalse(s.customer.peachWatch)
        assertFalse(fresh().go(Action.PushAlert).customer.peachPush)
        assertTrue(fresh().go(Action.SmsAlert).customer.peachSms)
    }

    @Test fun liveStatus_toasts() = assertTrue(effects(fresh(), Action.LiveStatus).any { it is Effect.Toast })

    @Test fun holdNav_opens_map_on_node_02() {
        val s = fresh().go(Action.Release, Action.HoldNav(t0))
        assertEquals(1, s.customerTab)
        assertEquals("N02", s.selectedNodeId)
        assertEquals("N02", s.customer.escrow?.nodeId)
    }

    @Test fun reorder_creates_escrow_and_opens_vip() {
        val s = fresh().go(Action.Release, Action.Reorder(t0))
        assertEquals(3, s.customerTab)
        assertNotNull(s.customer.escrow)
    }

    @Test fun prefs_toggle() {
        val s = fresh().go(Action.Pref("push"), Action.Pref("sound"), Action.Pref("quiet"))
        assertFalse(s.customer.prefPush)
        assertFalse(s.customer.prefSound)
        assertTrue(s.customer.prefQuiet)
    }

    // ---------------- vip
    @Test fun copyCode_and_qr() {
        assertTrue(effects(fresh(), Action.CopyCode).contains(Effect.CopyText(Escrow.BACKUP_CODE)))
        assertEquals(SheetKind.Qr, fresh().go(Action.Qr).sheet)
    }

    @Test fun redeem_deducts_points_once() {
        val s = fresh().go(Action.Redeem, Action.DoRedeem("d2"))
        assertEquals(200, s.customer.points)
        assertEquals(200, s.go(Action.DoRedeem("d2")).customer.points)
        // 200 - 150 = 50, then 90 is unaffordable
        assertEquals(50, s.go(Action.DoRedeem("d1"), Action.DoRedeem("d3")).customer.points)
    }

    // ---------------- supplier
    @Test fun network_fill_formula() {
        val s = fresh(supplier)
        assertEquals(132, s.supplier.coilSum)
        assertEquals(42, s.supplier.networkFill)
    }

    @Test fun coil_select_and_restock_c3_clears_queue() {
        val s = fresh(supplier).go(Action.SelectCoil("C3"), Action.RestockCoil("14:00"))
        assertEquals(15, s.supplier.coil("C3")?.level)
        assertEquals(0, s.supplier.queue)
        assertTrue(s.supplier.urgentResolved)
        assertEquals(3, s.aBadge)
        assertEquals(6, s.supplier.feed.size)
    }

    @Test fun velocity_select() = assertEquals(2, fresh(supplier).go(Action.Velocity(2)).supplier.velocitySel)

    @Test fun aiDispatch_advances_and_opens_dispatch() {
        val s = fresh(supplier).go(Action.AiDispatch("13:30"))
        assertEquals(3, s.supplier.stage)
        assertEquals(1, s.supplierTab)
    }

    @Test fun ping_and_call_runner() {
        val s = fresh(supplier).go(Action.PingRunner)
        assertTrue(s.supplier.runnerPinged)
        assertEquals(3, s.supplier.stage)
        assertTrue(effects(s, Action.CallRunner).any { it is Effect.Toast })
    }

    @Test fun approve_and_decline_clearance() {
        assertEquals("approved", fresh(supplier).go(Action.Approve("13:30")).supplier.clearance)
        assertEquals("dismissed", fresh(supplier).go(Action.Decline).supplier.clearance)
    }

    @Test fun surge_updates_metrics() {
        val s = fresh(supplier).go(Action.Surge("13:50"))
        assertEquals(318 + 64, s.supplier.dispensed)
        assertEquals(18 + 13, s.supplier.queue)
        assertEquals(31, s.supplier.networkFill)
        assertEquals(3, s.aBadge)
    }

    @Test fun confirmRestock_fills_everything_once() {
        val s = fresh(supplier).go(Action.ConfirmRestock("14:05"))
        assertTrue(s.supplier.coils.all { it.level == 15 })
        assertEquals(4, s.supplier.stage)
        assertEquals(0, s.supplier.queue)
        assertTrue(s.supplier.restockConfirmed)
        assertEquals(s, s.go(Action.ConfirmRestock("14:06")))
    }

    @Test fun alerts_ping_filter_mute() {
        val s = fresh(supplier).go(Action.Ping, Action.AlertFilter("stockout"), Action.Mute)
        assertTrue(s.supplier.urgentPinged)
        assertEquals("stockout", s.supplier.alertFilter)
        assertTrue(s.supplier.muted)
    }
}
