package com.chiforest.forestflow.feature.customer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.Escrow
import com.chiforest.forestflow.core.MapData
import com.chiforest.forestflow.core.Seed
import com.chiforest.forestflow.feature.ScreenColumn
import com.chiforest.forestflow.feature.mmss
import com.chiforest.forestflow.ui.components.BtnStyle
import com.chiforest.forestflow.ui.components.EmptyState
import com.chiforest.forestflow.ui.components.ForestButton
import com.chiforest.forestflow.ui.components.ForestCard
import com.chiforest.forestflow.ui.components.ForestSheet
import com.chiforest.forestflow.ui.components.Hairline
import com.chiforest.forestflow.ui.components.Ic
import com.chiforest.forestflow.ui.components.LiveDot
import com.chiforest.forestflow.ui.components.PageTitle
import com.chiforest.forestflow.ui.components.Pill
import com.chiforest.forestflow.ui.components.ProductArt
import com.chiforest.forestflow.ui.components.QrCode
import com.chiforest.forestflow.ui.components.SheetTitle
import com.chiforest.forestflow.ui.components.StatTile
import com.chiforest.forestflow.ui.components.pressable
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Type

fun escrowPayload(e: Escrow) = "chiforest://escrow/${e.nodeId}-${e.coil}-${Escrow.BACKUP_CODE.removePrefix("CF-").let { "CF$it" }}"

@Composable
fun VipScreen(s: AppState, now: State<Long>, map: MapData, dispatch: (Action) -> Unit) {
    val c = FF.c
    val e = s.customer.escrow
    ScreenColumn {
        PageTitle("Tier 2 · Campus Pass", "VIP Pass")

        if (e != null) {
            val node = map.node(e.nodeId)
            ForestCard(Modifier.fillMaxWidth(), radius = 28.dp, padding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("Active", c.green, c.greenTint, dot = true)
                    Spacer(Modifier.width(6.dp))
                    Pill("Slot ${e.coil}", c.ink, c.surface2)
                    Spacer(Modifier.weight(1f))
                    Pill("On hold", c.red, c.redTint, icon = Ic.Lock)
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductArt(e.product, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${e.product} Sparkling", style = Type.title(16.sp, 800), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("330ml can, chilled", style = Type.body(12.sp), color = c.faint)
                    }
                    Text("Qty 1", style = Type.num(13.sp, 700), color = c.muted)
                }
                Spacer(Modifier.height(16.dp))
                Hairline()
                Spacer(Modifier.height(16.dp))
                Text("TIME REMAINING TO CLAIM", style = Type.eyebrow(10.sp), color = c.faint, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                ClaimCountdown(e, now)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    LiveDot(c.green, size = 7.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Held for you at ${node?.shortName}", style = Type.body(12.sp, 600), color = c.muted)
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    QrCode(
                        escrowPayload(e),
                        Modifier.size(210.dp).pressable(scaleTo = 0.97f, label = "Enlarge QR code") { dispatch(Action.Qr) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Hold this to the scanner on ${node?.shortName} · tap to enlarge", style = Type.body(11.sp), color = c.faint, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface2).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Backup Code", style = Type.body(11.sp, 600), color = c.faint)
                        Text(Escrow.BACKUP_CODE, style = Type.mono(20.sp, 800).copy(letterSpacing = 2.sp), color = c.ink)
                    }
                    ForestButton("COPY", { dispatch(Action.CopyCode) }, small = true, style = BtnStyle.Dark, icon = Ic.Copy)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Hold expires automatically in 10 mins. Scan at physical machine scanner or punch the tap code above.",
                    style = Type.body(12.sp), color = c.faint,
                )
            }
        } else {
            EmptyState(Ic.Qr, "No can on hold", "Tap Hold on the Inventory tab to keep a can for 5 minutes. Your pickup QR code appears here.", "Browse Inventory") { dispatch(Action.Tab(0)) }
        }

        VipCard()

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Zero Sugar", "${s.customer.sugarKg} kg", Modifier.weight(1f), sub = "Sugar Avoided · Metabolic meter", icon = Ic.Leaf, accent = c.green)
            StatTile("Wallet", "${s.customer.points} pts", Modifier.weight(1f), sub = "Point Balance · Lab credits", icon = Ic.Star, accent = c.red)
        }
        ForestButton("Redeem Points for Pilot Drops", { dispatch(Action.Redeem) }, Modifier.fillMaxWidth(), icon = Ic.Gift)
    }
}

@Composable
private fun VipCard() {
    val reduce = FF.reduceMotion
    val t = rememberInfiniteTransition(label = "vip")
    val sheen by t.animateFloat(-0.6f, 1.6f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "sheen")
    val blink by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "nfc")
    Box(
        Modifier.fillMaxWidth().aspectRatio(1.58f).clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1C2539), Color(0xFF101828), Color(0xFF3A0D10)))),
    ) {
        // sheen sweep (one pass per 5s)
        if (!reduce) Canvas(Modifier.fillMaxSize()) {
            val x = size.width * sheen
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.13f), Color.Transparent),
                    start = Offset(x - 120f, 0f), end = Offset(x + 120f, size.height),
                ),
            )
        }
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFB7131A), Color(0xFFDB322F)))), contentAlignment = Alignment.Center) {
                    Text("VIP", style = Type.title(10.sp, 800), color = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Chi Forest VIP Club", style = Type.title(16.sp, 800), color = Color.White)
                    Text("TIER 2 STUDENT PASS", style = Type.eyebrow(9.sp), color = Color(0xFFFFB4AC))
                }
                // NFC bars blink in sequence
                Canvas(Modifier.size(28.dp)) {
                    for (i in 0..2) {
                        val on = reduce || ((blink * 3).toInt() >= i)
                        drawRoundRect(
                            Color.White.copy(alpha = if (on) 0.9f else 0.25f),
                            Offset(i * size.width / 3f + 3f, size.height * (0.55f - i * 0.18f)),
                            Size(4f * density / 2.5f + 3f, size.height * (0.3f + i * 0.18f)),
                            CornerRadius(4f),
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("AFFILIATION", style = Type.eyebrow(9.sp), color = Color.White.copy(alpha = 0.5f))
                    Text("XJTLU Taicang", style = Type.title(13.sp, 700), color = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("STUDENT ID", style = Type.eyebrow(9.sp), color = Color.White.copy(alpha = 0.5f))
                    Text("2241 8821", style = Type.mono(13.sp, 700), color = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("ACTIVE PRIVILEGES", style = Type.eyebrow(9.sp), color = Color.White.copy(alpha = 0.5f))
            Spacer(Modifier.height(4.dp))
            for (p in listOf("First pick of new drops", "Hold a can for up to 10 minutes", "10% off at every campus machine")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Ic.Check, null, tint = Color(0xFF3DDC97), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(p, style = Type.body(11.sp, 500), color = Color.White.copy(alpha = 0.85f), maxLines = 1)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("NFC TOUCH-READY", style = Type.eyebrow(9.sp), color = Color.White.copy(alpha = 0.45f), modifier = Modifier.graphicsLayer { alpha = 0.9f })
        }
    }
}

@Composable
fun RedeemSheet(s: AppState, dispatch: (Action) -> Unit) {
    val c = FF.c
    ForestSheet(onDismiss = { dispatch(Action.CloseSheet) }) {
        SheetTitle("Pilot Drops", "Balance · ${s.customer.points} pts") { dispatch(Action.CloseSheet) }
        Seed.drops.forEach { d ->
            val done = d.id in s.customer.redeemed
            val affordable = s.customer.points >= d.pts
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(18.dp)).background(c.surface2).padding(14.dp)
                    .graphicsLayer { alpha = if (done) 0.55f else 1f },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(c.redTint), contentAlignment = Alignment.Center) {
                    Icon(Ic.Gift, null, tint = c.red, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.name, style = Type.title(14.sp, 800), color = c.ink)
                    Text("${d.nameZh} · ${d.note}", style = Type.body(11.sp), color = c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                ForestButton(
                    if (done) "Redeemed" else "${d.pts} pts",
                    { dispatch(Action.DoRedeem(d.id)) }, small = true,
                    style = if (done) BtnStyle.Green else if (affordable) BtnStyle.Primary else BtnStyle.Outline,
                    enabled = !done, icon = if (done) Ic.Check else null,
                )
            }
        }
    }
}

@Composable
fun QrSheet(s: AppState, map: MapData, dispatch: (Action) -> Unit) {
    val c = FF.c
    val e = s.customer.escrow
    ForestSheet(onDismiss = { dispatch(Action.CloseSheet) }) {
        SheetTitle("Scan at the machine", "Hold the phone 10–15 cm from the optical port") { dispatch(Action.CloseSheet) }
        if (e == null) {
            Text("No can on hold.", style = Type.body(14.sp), color = c.faint)
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { QrCode(escrowPayload(e), Modifier.fillMaxWidth(0.86f).aspectRatio(1f)) }
            Spacer(Modifier.height(12.dp))
            Text("${map.node(e.nodeId)?.name} · slot ${e.coil} · code ${Escrow.BACKUP_CODE}", style = Type.mono(13.sp, 600), color = c.muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

/** Only the countdown reads the clock, so the rest of the VIP pass (QR code included) is not redrawn every second. */
@Composable
private fun ClaimCountdown(e: Escrow, now: State<Long>) {
    val c = FF.c
    val left = e.claimLeftMs(now.value)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
        Text(mmss(left), style = Type.mono(52.sp, 800), color = if (left < 60_000) c.red else c.ink)
        Spacer(Modifier.width(6.dp))
        Text("MIN", style = Type.eyebrow(11.sp), color = c.faint, modifier = Modifier.padding(bottom = 12.dp))
    }
}
