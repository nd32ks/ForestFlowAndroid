package com.chiforest.forestflow.core

import kotlin.math.roundToInt

/**
 * Dynamic clearance, same rule as the Softr Dispatch page: group the coils by product, work out how many hours
 * each product needs to sell out at the current dispense rate (surge included), and suggest a markdown on the
 * slowest one that needs longer than [THRESHOLD_HOURS].
 */
object Clearance {
    const val LIST_PRICE = 5.50
    const val THRESHOLD_HOURS = 2.0
    const val DISCOUNT_PCT = 20
    const val SURGE_FACTOR = 2.5

    data class Line(
        val name: String,
        val coils: List<String>,
        val stock: Int,
        val perHour: Double,
        val hours: Double,
        val price: Double,
    ) {
        val onSale: Boolean get() = price < LIST_PRICE - 0.001
    }

    fun surge(sp: SupplierState): Double = if (sp.surges > 0) SURGE_FACTOR else 1.0

    fun lines(sp: SupplierState): List<Line> {
        val s = surge(sp)
        return sp.coils.groupBy { it.name }.map { (name, cs) ->
            val stock = cs.sumOf { it.level.coerceAtLeast(0) }
            val perHour = cs.sumOf { it.cph } * s
            val hours = when {
                stock <= 0 -> 0.0
                perHour > 0 -> stock / perHour
                else -> NO_SALES
            }
            Line(name, cs.map { it.id }, stock, perHour, hours, sp.prices[name] ?: LIST_PRICE)
        }
    }

    /** The product to mark down next, or null when nothing is slow enough (or it was dismissed). */
    fun candidate(sp: SupplierState): Line? = lines(sp)
        .filter { !it.onSale && it.stock > 0 && it.hours > THRESHOLD_HOURS && it.name != sp.clearanceDismissed }
        .maxByOrNull { it.hours }

    fun live(sp: SupplierState): List<Line> = lines(sp).filter { it.onSale }

    /** List price less [DISCOUNT_PCT], rounded to 10 fen (¥5.50 → ¥4.40). */
    fun salePrice(price: Double): Double = ((price * (100 - DISCOUNT_PCT) / 100) * 10).roundToInt() / 10.0

    fun yuan(v: Double): String = "¥" + String.format(java.util.Locale.US, "%.2f", v)

    fun hoursLabel(h: Double): String =
        if (h >= NO_SALES) "no recent sales" else String.format(java.util.Locale.US, "%.1f h to sell out", h)

    private const val NO_SALES = 999.0
}
