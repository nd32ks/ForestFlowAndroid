package com.chiforest.forestflow.core

import com.chiforest.forestflow.BuildConfig

/**
 * Which parts of the app are shown. The demo build shows the login page and two features only:
 *  - Feature 1: AI stock prediction and dispatch (supplier: Telemetry, Dispatch, Alerts)
 *  - Feature 2: restock radar and "Notify Me" alerts (student: Inventory, Notifications)
 * The rest is built and tested but hidden until switched on (see `features` in app/build.gradle.kts / README).
 */
object Features {
    /** Feature 3: VIP pass, 10-minute escrow (Lock / Release / QR) and points. */
    val vipClub: Boolean get() = BuildConfig.FEATURE_VIP_CLUB

    /** Campus map, routes, directions, "I'm here" and the station list. */
    val campusMap: Boolean get() = BuildConfig.FEATURE_CAMPUS_MAP

    /** Canonical student tab ids (the numbers the reducer uses): 0 Inventory, 1 Map, 2 Notifications, 3 VIP. */
    fun studentTabs(vip: Boolean = vipClub, map: Boolean = campusMap): List<Int> =
        listOf(0) + (if (map) listOf(1) else emptyList()) + listOf(2) + (if (vip) listOf(3) else emptyList())

    /** A tab id saved earlier may point at a hidden tab; fall back to the first visible one. */
    fun effectiveStudentTab(tab: Int, vip: Boolean = vipClub, map: Boolean = campusMap): Int =
        studentTabs(vip, map).let { if (tab in it) tab else it.first() }
}
