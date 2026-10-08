package com.chiforest.forestflow.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Uniform stroke icon set (24dp viewport, 1.8 stroke, round caps/joins).
 * Circles and rects are expressed as path commands so every glyph is one stroked path.
 */
object Ic {
    private fun c(cx: Float, cy: Float, r: Float) =
        " M${cx - r} $cy a$r $r 0 1 0 ${2 * r} 0 a$r $r 0 1 0 ${-2 * r} 0"

    private fun r(x: Float, y: Float, w: Float, h: Float, rx: Float) =
        " M${x + rx} $y h${w - 2 * rx} a$rx $rx 0 0 1 $rx $rx v${h - 2 * rx} a$rx $rx 0 0 1 ${-rx} $rx" +
            " h${-(w - 2 * rx)} a$rx $rx 0 0 1 ${-rx} ${-rx} v${-(h - 2 * rx)} a$rx $rx 0 0 1 $rx ${-rx} z"

    private fun icon(name: String, d: String, filled: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
            pathData = addPathNodes(d),
            fill = if (filled) SolidColor(Color.Black) else null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()

    val Box = icon("box", "M21 8l-9 -5 -9 5v8l9 5 9 -5z M3 8l9 5 9 -5 M12 13v8 M7.5 5.5l9 5")
    val Navigation = icon("navigation", "M3 11l19 -9 -9 19 -2 -8z")
    val NavigationFilled = icon("navigationFilled", "M3 11l19 -9 -9 19 -2 -8z", filled = true)
    val Bell = icon("bell", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3 -2 3 -9 M10.3 21a1.94 1.94 0 0 0 3.4 0")
    val BellOff = icon("bellOff", "M13.7 21a2 2 0 0 1 -3.4 0 M18.6 13A17.9 17.9 0 0 1 18 8 M6.3 6.3A6 6 0 0 0 6 8c0 7 -3 9 -3 9h14 M18 8a6 6 0 0 0 -9.3 -5 M2 2l20 20")
    val IdCard = icon("idCard", r(2f, 5f, 20f, 14f, 2f) + c(8f, 10.5f, 2f) + " M4.8 16c0.6 -1.6 1.9 -2.4 3.2 -2.4s2.6 0.8 3.2 2.4 M14 10h4 M14 13.5h3")
    val BarChart = icon("barChart", "M3 3v18h18 M18 17V9 M13 17V5 M8 17v-3")
    val Truck = icon("truck", "M1 4h14v12H1z M15 8h4l4 4v4h-8z" + c(5.5f, 18.5f, 2f) + c(18.5f, 18.5f, 2f))
    val Search = icon("search", c(11f, 11f, 7f) + " M21 21l-4.3 -4.3")
    val Locate = icon("locate", c(12f, 12f, 7f) + c(12f, 12f, 2.5f) + " M12 2v3 M12 19v3 M2 12h3 M19 12h3")
    val Compass = icon("compass", c(12f, 12f, 9f) + " M16 8l-2.5 5.5L8 16l2.5 -5.5z")
    val Layers = icon("layers", "M12 2l10 5 -10 5L2 7z M2 12l10 5 10 -5 M2 17l10 5 10 -5")
    val Bookmark = icon("bookmark", "M19 21l-7 -5 -7 5V5a2 2 0 0 1 2 -2h10a2 2 0 0 1 2 2z")
    val BookmarkFilled = icon("bookmarkFilled", "M19 21l-7 -5 -7 5V5a2 2 0 0 1 2 -2h10a2 2 0 0 1 2 2z", filled = true)
    val Route = icon("route", c(6f, 19f, 3f) + " M9 19h8.5a3.5 3.5 0 0 0 0 -7h-11a3.5 3.5 0 0 1 0 -7H15" + c(18f, 5f, 3f))
    val MapPin = icon("mapPin", "M20 10c0 6 -8 12 -8 12s-8 -6 -8 -12a8 8 0 0 1 16 0z" + c(12f, 10f, 3f))
    val Lock = icon("lock", r(3f, 11f, 18f, 11f, 2f) + " M7 11V7a5 5 0 0 1 10 0v4")
    val Unlock = icon("unlock", r(3f, 11f, 18f, 11f, 2f) + " M7 11V7a5 5 0 0 1 9.9 -1")
    val Share = icon("share", c(18f, 5f, 3f) + c(6f, 12f, 3f) + c(18f, 19f, 3f) + " M8.6 13.5l6.8 4 M15.4 6.5l-6.8 4")
    val Check = icon("check", "M20 6L9 17l-5 -5")
    val Close = icon("close", "M18 6L6 18 M6 6l12 12")
    val ChevronRight = icon("chevronRight", "M9 18l6 -6 -6 -6")
    val ChevronDown = icon("chevronDown", "M6 9l6 6 6 -6")
    val Bolt = icon("bolt", "M13 2L3 14h9l-1 8 10 -12h-9z")
    val Thermo = icon("thermo", "M14 14.76V3.5a2.5 2.5 0 0 0 -5 0v11.26a4.5 4.5 0 1 0 5 0z")
    val User = icon("user", c(12f, 8f, 4f) + " M4 21c0 -4 4 -6 8 -6s8 2 8 6")
    val Users = icon("users", c(9f, 7f, 4f) + " M3 21v-2a4 4 0 0 1 4 -4h4a4 4 0 0 1 4 4v2 M16 3.1a4 4 0 0 1 0 7.8 M21 21v-2a4 4 0 0 0 -3 -3.9")
    val Logout = icon("logout", "M9 21H5a2 2 0 0 1 -2 -2V5a2 2 0 0 1 2 -2h4 M16 17l5 -5 -5 -5 M21 12H9")
    val Refresh = icon("refresh", "M21 12a9 9 0 1 1 -3 -6.7L21 8 M21 3v5h-5")
    val Copy = icon("copy", r(9f, 9f, 13f, 13f, 2f) + " M5 15H4a2 2 0 0 1 -2 -2V4a2 2 0 0 1 2 -2h9a2 2 0 0 1 2 2v1")
    val Phone = icon("phone", "M22 16.9v3a2 2 0 0 1 -2.2 2 19.8 19.8 0 0 1 -8.6 -3.1 19.5 19.5 0 0 1 -6 -6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c0.1 0.9 0.4 1.8 0.7 2.7a2 2 0 0 1 -0.5 2.1L8 9.8a16 16 0 0 0 6 6l1.3 -1.3a2 2 0 0 1 2.1 -0.4c0.9 0.3 1.8 0.6 2.7 0.7a2 2 0 0 1 1.7 2z")
    val Chat = icon("chat", "M21 11.5a8.4 8.4 0 0 1 -9 8.4 9 9 0 0 1 -3.8 -0.9L3 20l1.3 -4A8.4 8.4 0 0 1 12 3a8.4 8.4 0 0 1 9 8.5z M8.5 11h0.01 M12 11h0.01 M15.5 11h0.01")
    val Shield = icon("shield", "M12 22s8 -4 8 -10V5l-8 -3 -8 3v7c0 6 8 10 8 10z M9 12l2 2 4 -4")
    val Building = icon("building", r(4f, 2f, 16f, 20f, 2f) + " M9 22v-4h6v4 M8 6h0.01 M16 6h0.01 M12 6h0.01 M8 10h0.01 M16 10h0.01 M12 10h0.01 M8 14h0.01 M16 14h0.01 M12 14h0.01")
    val Gift = icon("gift", r(3f, 8f, 18f, 4f, 1f) + " M12 8v13 M19 12v7a2 2 0 0 1 -2 2H7a2 2 0 0 1 -2 -2v-7 M7.5 8a2.5 2.5 0 0 1 0 -5C11 3 12 8 12 8s1 -5 4.5 -5a2.5 2.5 0 0 1 0 5")
    val Clock = icon("clock", c(12f, 12f, 9f) + " M12 7v5l3 2")
    val Sun = icon("sun", c(12f, 12f, 4f) + " M12 2v2 M12 20v2 M4.9 4.9l1.4 1.4 M17.7 17.7l1.4 1.4 M2 12h2 M20 12h2 M6.3 17.7l-1.4 1.4 M19.1 4.9l-1.4 1.4")
    val Sparkles = icon("sparkles", "M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9 -5.1L5 10l5.1 -1.9z M19 15l0.8 2.2L22 18l-2.2 0.8L19 21l-0.8 -2.2L16 18l2.2 -0.8z")
    val Alert = icon("alert", "M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7 -3L13.7 3.9a2 2 0 0 0 -3.4 0z M12 9v4 M12 17h0.01")
    val Volume = icon("volume", "M11 5L6 9H2v6h4l5 4z M15.5 8.5a5 5 0 0 1 0 7 M19 5a10 10 0 0 1 0 14")
    val Moon = icon("moon", "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z")
    val Drop = icon("drop", "M12 2.7l5.7 5.6a8 8 0 1 1 -11.4 0z")
    val Leaf = icon("leaf", "M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.5 19 2c1 2 2 4.2 2 8 0 5.5 -4.8 10 -10 10z M2 21c0 -3 1.9 -5.4 5.1 -6")
    val TrendUp = icon("trendUp", "M22 7l-8.5 8.5 -5 -5L2 17 M16 7h6v6")
    val Qr = icon("qr", r(3f, 3f, 7f, 7f, 1f) + r(14f, 3f, 7f, 7f, 1f) + r(3f, 14f, 7f, 7f, 1f) + " M14 14h3v3h-3z M20 14v0.01 M14 20h0.01 M17 17h4v4")
    val Mail = icon("mail", r(2f, 4f, 20f, 16f, 2f) + " M22 6l-10 7L2 6")
    val Key = icon("key", c(7.5f, 15.5f, 5.5f) + " M11.5 11.5L21 2 M18 5l3 3 M15 8l2 2")
    val Plus = icon("plus", "M12 5v14 M5 12h14")
    val ArrowRight = icon("arrowRight", "M5 12h14 M12 5l7 7 -7 7")
    val Star = icon("star", "M12 2l3.1 6.3 6.9 1 -5 4.9 1.2 6.8L12 17.8 5.8 21l1.2 -6.8 -5 -4.9 6.9 -1z")
    val Snow = icon("snow", "M12 2v20 M4.9 7l14.2 10 M4.9 17L19.1 7 M9 4l3 2 3 -2 M9 20l3 -2 3 2")
    val Signal = icon("signal", "M5 12.5a10 10 0 0 1 14 0 M8.5 16a5 5 0 0 1 7 0 M12 19.5h0.01 M2 9a15 15 0 0 1 20 0")
    val Vending = icon("vending", r(5f, 2f, 14f, 20f, 2f) + " M9 6h6 M9 10h6 M9 14h2 M15 14h0.01 M9 18h6")
    val Activity = icon("activity", "M22 12h-4l-3 9L9 3l-3 9H2")
    val Tag = icon("tag", "M12.6 2.6A2 2 0 0 0 11.2 2H4a2 2 0 0 0 -2 2v7.2a2 2 0 0 0 0.6 1.4l8.7 8.7a2.4 2.4 0 0 0 3.4 0l6.6 -6.6a2.4 2.4 0 0 0 0 -3.4z" + c(7.5f, 7.5f, 1.5f))
    val CheckCircle = icon("checkCircle", c(12f, 12f, 10f) + " M8 12l3 3 5 -6")
    val Ping = icon("ping", c(12f, 12f, 2f) + " M16.2 7.8a6 6 0 0 1 0 8.4 M7.8 16.2a6 6 0 0 1 0 -8.4 M19.1 4.9a10 10 0 0 1 0 14.2 M4.9 19.1a10 10 0 0 1 0 -14.2")
    val Cloud = icon("cloud", "M12 2v2 M4.9 4.9l1.4 1.4 M20 12h2 M19.1 4.9l-1.4 1.4 M15.9 13a4 4 0 0 0 -7.5 -2.6 M13 22H7a5 5 0 1 1 4.9 -6H13a3 3 0 0 1 0 6z")
    val Eye = icon("eye", "M2 12s3.5 -7 10 -7 10 7 10 7 -3.5 7 -10 7S2 12 2 12z" + c(12f, 12f, 3f))
    val Database = icon("database", "M3 5c0 -1.7 4 -3 9 -3s9 1.3 9 3 -4 3 -9 3 -9 -1.3 -9 -3z M3 5v14c0 1.7 4 3 9 3s9 -1.3 9 -3V5 M3 12c0 1.7 4 3 9 3s9 -1.3 9 -3")
    val Minus = icon("minus", "M5 12h14")
}
