package com.chiforest.forestflow.core

/**
 * When will an out-of-stock drink be back? Reads the supplier's restock pipeline, so what the student sees changes
 * as the supplier dispatches, pings the runner and confirms the restock. Only Node 01 has live supplier data.
 */
object RestockEta {
    data class Eta(val minutes: Int, val label: String)

    const val LIVE_NODE = "N01"

    fun forNode(nodeId: String, sp: SupplierState): Eta? {
        if (nodeId != LIVE_NODE) return null
        return when {
            sp.restockConfirmed || sp.stage >= 4 -> Eta(0, "Restock delivered, check the machine")
            sp.stage == 3 -> Eta(10, "Runner on the way · back in about 10 min")
            sp.stage == 2 -> Eta(25, "Runner assigned · back in about 25 min")
            else -> Eta(40, "Restock queued · back in about 40 min")
        }
    }
}
