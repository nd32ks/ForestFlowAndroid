package com.chiforest.forestflow

import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.Features
import com.chiforest.forestflow.core.Reducer
import com.chiforest.forestflow.core.Role
import com.chiforest.forestflow.core.Seed
import com.chiforest.forestflow.core.Session
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.MapData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/** The demo build hides later features; they must be ready to be brought out with one switch. */
class FeaturesTest {
    @Test fun student_tabs_follow_the_switches() {
        assertEquals(listOf(0, 2), Features.studentTabs(vip = false, map = false))        // demo scope
        assertEquals(listOf(0, 2, 3), Features.studentTabs(vip = true, map = false))      // + VIP club
        assertEquals(listOf(0, 1, 2), Features.studentTabs(vip = false, map = true))
        assertEquals(listOf(0, 1, 2, 3), Features.studentTabs(vip = true, map = true))    // everything
    }

    @Test fun a_saved_tab_that_is_now_hidden_falls_back_to_the_first_visible_one() {
        assertEquals(0, Features.effectiveStudentTab(3, vip = false, map = false))
        assertEquals(0, Features.effectiveStudentTab(1, vip = false, map = false))
        assertEquals(2, Features.effectiveStudentTab(2, vip = false, map = false))
        assertEquals(3, Features.effectiveStudentTab(3, vip = true, map = false))
    }

    @Test fun unread_only_counts_cards_that_are_shown() {
        val s = Seed.fresh(0L)
        assertEquals(3, s.unread(vip = true))
        assertEquals(1, s.unread(vip = false))                       // only the restock drop alert is shown in the demo
        assertEquals(0, s.copy(customer = s.customer.copy(dismissed = listOf("urgent"))).unread(vip = false))
    }

    @Test fun demo_seed_has_no_hold_but_the_full_seed_does() {
        assertNull(Seed.fresh(0L, escrow = false).customer.escrow)
        assertNotNull(Seed.fresh(0L).customer.escrow)
        val map = MapData.parse(File("src/main/assets/maps/map_data.json").readText())
        val session = Session("u", "Test", Role.Student, "cas", guest = false)
        val demo = Reducer(map, seedEscrow = false)
        val reset: AppState = demo.reduce(Seed.fresh(0L, session), Action.Reset(5L)).state
        assertNull(reset.customer.escrow)                             // Reset demo data keeps the demo scope
        val full = Reducer(map, seedEscrow = true).reduce(Seed.fresh(0L, session, escrow = false), Action.Reset(5L)).state
        assertNotNull(full.customer.escrow)                           // ...and restores the hold once VIP is on
    }
}
