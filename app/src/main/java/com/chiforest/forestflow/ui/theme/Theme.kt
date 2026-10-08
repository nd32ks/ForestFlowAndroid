package com.chiforest.forestflow.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class ForestColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val red: Color,
    val redBtn: Color,
    val red2: Color,
    val redTint: Color,
    val redLine: Color,
    val green: Color,
    val greenTint: Color,
    val wechat: Color,
    val blue: Color,
    val blueTint: Color,
    val amber: Color,
    val amberTint: Color,
    val glass: Color,
    val glassLine: Color,
    val navy: Color,
    val navy2: Color,
    val bar: Color,
    val switchOn: Color,
    val isDark: Boolean,
)

val LightColors = ForestColors(
    bg = Color(0xFFF2F4F6),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF2F4F6),
    ink = Color(0xFF191C1E),
    muted = Color(0xFF5B403D),
    faint = Color(0xFF8F7774),
    line = Color(0xFFE8EAEC),
    red = Color(0xFFB7131A),
    redBtn = Color(0xFFB7131A),
    red2 = Color(0xFFDB322F),
    redTint = Color(0xFFB7131A).copy(alpha = 0.09f),
    redLine = Color(0xFFB7131A).copy(alpha = 0.22f),
    green = Color(0xFF006C49),
    greenTint = Color(0xFF006C49).copy(alpha = 0.11f),
    wechat = Color(0xFF07C160),
    blue = Color(0xFF0B63E5),
    blueTint = Color(0xFF0B63E5).copy(alpha = 0.10f),
    amber = Color(0xFFB86A00),
    amberTint = Color(0xFFE08A00).copy(alpha = 0.14f),
    glass = Color.White.copy(alpha = 0.66f),
    glassLine = Color.White.copy(alpha = 0.75f),
    navy = Color(0xFF101828),
    navy2 = Color(0xFF1C2539),
    bar = Color(0xFFDFE2E6),
    switchOn = Color(0xFF34C759),
    isDark = false,
)

val DarkColors = ForestColors(
    bg = Color(0xFF0E1113),
    surface = Color(0xFF181B1E),
    surface2 = Color(0xFF222629),
    ink = Color(0xFFEEF0F2),
    muted = Color(0xFFBFAEAC),
    faint = Color(0xFF8D807E),
    line = Color(0xFF2A2F33),
    red = Color(0xFFFF6A62),
    redBtn = Color(0xFFCF242B),
    red2 = Color(0xFFE5484D),
    redTint = Color(0xFFFF6A62).copy(alpha = 0.14f),
    redLine = Color(0xFFFF6A62).copy(alpha = 0.30f),
    green = Color(0xFF3DDC97),
    greenTint = Color(0xFF3DDC97).copy(alpha = 0.13f),
    wechat = Color(0xFF07C160),
    blue = Color(0xFF6EA8FF),
    blueTint = Color(0xFF6EA8FF).copy(alpha = 0.15f),
    amber = Color(0xFFFFB454),
    amberTint = Color(0xFFFFB454).copy(alpha = 0.15f),
    glass = Color(0xFF1E2226).copy(alpha = 0.62f),
    glassLine = Color.White.copy(alpha = 0.10f),
    navy = Color(0xFF0A0D12),
    navy2 = Color(0xFF151B27),
    bar = Color(0xFF2C3135),
    switchOn = Color(0xFF34C759),
    isDark = true,
)

val LocalForestColors = staticCompositionLocalOf { LightColors }
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Shorthand accessor: `FF.c.red`. */
object FF {
    val c: ForestColors
        @Composable get() = LocalForestColors.current
    val reduceMotion: Boolean
        @Composable get() = LocalReduceMotion.current
}

@Composable
fun ForestTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reduce = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    CompositionLocalProvider(
        LocalForestColors provides if (dark) DarkColors else LightColors,
        LocalReduceMotion provides reduce,
        content = content,
    )
}
