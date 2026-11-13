package com.chiforest.forestflow.core

import androidx.compose.runtime.Immutable

/** One page of the sign-in guide. [tab] is the tab the "Take me there" button opens, when there is one. */
@Immutable
data class GuideStep(val where: String, val title: String, val body: String, val tab: Int? = null)

/**
 * The guide shown after every sign-in (and from the "?" button in the header). It follows the real flow of the app,
 * so steps for a feature that is switched off are left out rather than pointing at a missing button.
 */
object Guide {
    fun steps(role: Role, map: Boolean = Features.campusMap, vip: Boolean = Features.vipClub): List<GuideStep> =
        if (role == Role.Student) student(map, vip) else supplier()

    private fun student(map: Boolean, vip: Boolean): List<GuideStep> = buildList {
        add(
            GuideStep(
                "Inventory tab · top card", "1. Find a machine",
                if (map) "The top card shows the machine nearest to you, its building and the walk time. Tap Route for walking directions, or open Campus Map to see every machine on both campuses. Machine names say where it is, like “AI Lab 2F”."
                else "The top card shows the machine nearest to you, its building and the walk time. Machine names say where it is, like “AI Lab 2F”.",
                if (map) 1 else 0,
            ),
        )
        add(
            GuideStep(
                "Inventory tab · drink cards", "2. Check what’s in stock",
                "Each card shows how many cans are left at this machine. Tap a card for taste, sugar and calories, and the nearest machine that has it. Not sure what to pick? Type what you feel like in Ask for a drink.",
                0,
            ),
        )
        add(
            GuideStep(
                if (vip) "Hold a can · Notify Me" else "Notify Me · Get drop alerts", "3. Hold one, or get a ping",
                if (vip) "Tap Hold a can to keep one for about 5 minutes. Sold out? Tap Notify Me and your phone buzzes when it’s back. Get drop alerts tells you about every new flavour."
                else "Sold out? Tap Notify Me and your phone buzzes when it’s back. Get drop alerts tells you about every new flavour.",
                0,
            ),
        )
        add(
            GuideStep(
                if (vip) "VIP Pass tab" else "At the machine", "4. Pick it up at the machine",
                if (vip) "Show the QR code on VIP Pass to the machine’s scanner, about 10–15 cm away. The app doesn’t take payment: you pay at the machine."
                else "Walk to the machine and get your drink there. The app doesn’t take payment: you pay at the machine.",
                if (vip) 3 else null,
            ),
        )
    }

    private fun supplier(): List<GuideStep> = listOf(
        GuideStep(
            "Telemetry tab", "1. Watch the stock",
            "The coil grid shows how full each of the 16 coils is. Red means empty. Tap a coil to see it and restock it. The AI card warns you before a drink runs out.",
            0,
        ),
        GuideStep(
            "Telemetry, then Dispatch", "2. Send a restock",
            "On Telemetry, tap Dispatch Runner to raise a ticket. On Dispatch, tap Ping to send the runner, then Confirm Restock Loaded when the coils are full.",
            1,
        ),
        GuideStep(
            "Dispatch tab · clearance card", "3. Mark down slow drinks",
            "The clearance card suggests a price cut for the slowest-selling drink. Approve Price Cut applies it, End puts the price back, Dismiss moves to the next one.",
            1,
        ),
        GuideStep(
            "Alerts and Stock tabs", "4. Keep an eye on it all",
            "Alerts lists every empty coil, dispatch and price change. Stock shows every machine on both campuses, emptiest first.",
            3,
        ),
    )
}

/** Finds the closest other machine (by walking time) that has a drink in stock. */
object Nearby {
    fun nearest(map: MapData, fromNodeId: String, key: String): MapNode? =
        map.allNodes.filter { it.id != fromNodeId && (it.ready[key] ?: 0) > 0 }.minByOrNull { it.walkMin }
}
