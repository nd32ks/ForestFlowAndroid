package com.chiforest.forestflow.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.chiforest.forestflow.R

private fun vf(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val weights = listOf(400, 500, 600, 700, 800)

val Jakarta = FontFamily(weights.map { vf(R.font.plus_jakarta_sans, it) })
val Inter = FontFamily(weights.map { vf(R.font.inter, it) })
val Mono = FontFamily(listOf(400, 500, 700, 800).map { vf(R.font.jetbrains_mono, it) })

/** Type ramp: tracking tightens as size grows, leading loosens as size shrinks. */
object Type {
    fun display(size: TextUnit = 30.sp, weight: Int = 800) = TextStyle(
        fontFamily = Jakarta,
        fontWeight = FontWeight(weight),
        fontSize = size,
        lineHeight = size * 1.12f,
        letterSpacing = if (size.value >= 26) (-0.035).em else (-0.02).em,
    )

    fun title(size: TextUnit = 17.sp, weight: Int = 700) = TextStyle(
        fontFamily = Jakarta,
        fontWeight = FontWeight(weight),
        fontSize = size,
        lineHeight = size * 1.25f,
        letterSpacing = (-0.015).em,
    )

    fun body(size: TextUnit = 14.sp, weight: Int = 400) = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight(weight),
        fontSize = size,
        lineHeight = size * 1.45f,
        letterSpacing = if (size.value <= 12) 0.01.em else 0.em,
    )

    fun eyebrow(size: TextUnit = 11.sp) = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = size,
        lineHeight = size * 1.3f,
        letterSpacing = 0.08.em,
    )

    fun mono(size: TextUnit = 14.sp, weight: Int = 700) = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight(weight),
        fontSize = size,
        lineHeight = size * 1.2f,
        fontFeatureSettings = "tnum",
        letterSpacing = if (size.value >= 28) (-0.02).em else 0.em,
    )

    /** Body numerals that change in place (counts, prices). */
    fun num(size: TextUnit = 14.sp, weight: Int = 700) =
        title(size, weight).copy(fontFeatureSettings = "tnum")
}
