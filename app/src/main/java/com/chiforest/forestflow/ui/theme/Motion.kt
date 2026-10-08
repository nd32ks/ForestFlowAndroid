package com.chiforest.forestflow.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/** Motion tokens ported from the prototype's CSS curves. */
object Motion {
    val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val EaseOut = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val EaseInOut = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    /** Segmented thumb and nav indicator: slight overshoot. */
    fun <T> indicator() = spring<T>(dampingRatio = 0.68f, stiffness = 380f)

    /** Critically damped default for UI that was not thrown. */
    fun <T> settle() = spring<T>(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)

    /** Momentum release: a little bounce, only after a gesture. */
    fun <T> bouncy() = spring<T>(dampingRatio = 0.62f, stiffness = 420f)

    val popOffset = spring<IntOffset>(dampingRatio = 0.8f, stiffness = 420f)

    val TabSlide: Dp = 26.dp
    const val TAB_MS = 420
    const val ENTER_MS = 550
}
