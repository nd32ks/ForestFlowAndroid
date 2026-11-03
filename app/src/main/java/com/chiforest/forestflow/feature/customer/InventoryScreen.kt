package com.chiforest.forestflow.feature.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.ai.DrinkCatalog
import com.chiforest.forestflow.core.Escrow
import com.chiforest.forestflow.core.RestockEta
import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.Features
import com.chiforest.forestflow.core.MapData
import com.chiforest.forestflow.core.Product
import com.chiforest.forestflow.core.Seed
import com.chiforest.forestflow.core.SheetKind
import com.chiforest.forestflow.feature.ScreenColumn
import com.chiforest.forestflow.feature.mmss
import com.chiforest.forestflow.ui.components.BtnStyle
import com.chiforest.forestflow.ui.components.Eyebrow
import com.chiforest.forestflow.ui.components.ForestButton
import com.chiforest.forestflow.ui.components.ForestCard
import com.chiforest.forestflow.ui.components.Ic
import com.chiforest.forestflow.ui.components.LiveDot
import com.chiforest.forestflow.ui.components.Pill
import com.chiforest.forestflow.ui.components.ProductArt
import com.chiforest.forestflow.ui.components.pressable
import com.chiforest.forestflow.ui.components.softCard
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Type

@Composable
fun InventoryScreen(s: AppState, now: State<Long>, map: MapData, dispatch: (Action) -> Unit) {
    val c = FF.c
    val node = map.node(s.activeNodeId) ?: map.allNodes.first()
    val campus = map.campusOfNode(node.id)
    ScreenColumn {
        // Station card
        ForestCard(Modifier.fillMaxWidth(), radius = 26.dp, padding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("STATION · ${node.shortName.uppercase()} · BLDG ${node.building}", style = Type.eyebrow(10.sp), color = c.red, modifier = Modifier.weight(1f))
                Pill("${node.walkMin} min walk", c.amber, c.amberTint, icon = Ic.Bolt)
            }
            Spacer(Modifier.height(6.dp))
            Text(node.label, style = Type.display(28.sp), color = c.ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot(if (node.isLow) c.amber else c.green, size = 7.dp)
                Spacer(Modifier.width(6.dp))
                Text("${node.name} · ${campus?.subtitle}", style = Type.body(13.sp), color = c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Features.campusMap) {
                    QuickAction(Ic.Route, "Route", Modifier.weight(1f)) { dispatch(Action.Quick("Route", System.currentTimeMillis())) }
                    QuickAction(Ic.MapPin, "I'm Here", Modifier.weight(1f), active = s.youAreHereNodeId == node.id) { dispatch(Action.Quick("I'm Here", System.currentTimeMillis())) }
                }
                if (Features.vipClub) {
                    QuickAction(Ic.Lock, "Lock", Modifier.weight(1f), active = s.customer.escrow != null) { dispatch(Action.Quick("Lock", System.currentTimeMillis())) }
                }
                QuickAction(Ic.Bell, "Alerts", Modifier.weight(1f)) { dispatch(Action.Tab(2)) }
                QuickAction(Ic.Share, "Share", Modifier.weight(1f)) { dispatch(Action.Quick("Share", System.currentTimeMillis())) }
            }
        }

        AskCard(DrinkCatalog.options(Seed.products, node.ready), s.customer.osmanthusWatch) { dispatch(Action.Notify) }

        Column {
            Eyebrow("Tray Coil Inventory") {
                Pill("4 Slots", c.ink, c.surface)
                Spacer(Modifier.width(6.dp))
                Pill("${node.tempC}°C ${if (node.tempC < 4f) "Hyper-Cool" else "Chilled"}", if (node.tempC < 4f) c.blue else c.amber, if (node.tempC < 4f) c.blueTint else c.amberTint, icon = Ic.Snow)
            }
            Spacer(Modifier.height(10.dp))
            val products = Seed.products
            for (row in products.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { p ->
                        val count = node.ready[p.key] ?: 0
                        val eta = if (count <= 0) RestockEta.forNode(node.id, s.supplier)?.label else null
                        CoilCard(p, count, s.customer.osmanthusWatch, Modifier.weight(1f), dispatch, eta)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        AnimatedVisibility(
            Features.vipClub && s.customer.escrow != null,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut(),
        ) {
            val e = s.customer.escrow
            if (e != null) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(c.navy2, c.navy)))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Ic.Lock, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("1 Lock in Escrow", style = Type.title(15.sp, 800), color = Color.White)
                        Text("${map.node(e.nodeId)?.shortName} Coil #${e.coil} · ${e.product}", style = Type.body(12.sp), color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        HoldLeft(e, now)
                    }
                    ForestButton("Release", { dispatch(Action.Release) }, small = true, style = BtnStyle.Primary)
                }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, active: Boolean = false, onClick: () -> Unit) {
    val c = FF.c
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) c.redTint else c.surface2)
            .semantics { contentDescription = label }
            .pressable(scaleTo = 0.92f, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (active) c.red else c.ink, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = Type.body(11.sp, 700), color = if (active) c.red else c.ink, maxLines = 1)
    }
}

@Composable
private fun CoilCard(p: Product, count: Int, watching: Boolean, modifier: Modifier, dispatch: (Action) -> Unit, eta: String? = null) {
    val c = FF.c
    val out = count <= 0
    Column(modifier.softCard(RoundedCornerShape(22.dp))) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.25f).clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))) {
            ProductArt(p.key, Modifier.matchParentSize(), outOfStock = out)
            Text(
                "COIL #0${p.coil}", style = Type.eyebrow(9.sp), color = c.ink,
                modifier = Modifier.padding(10.dp).clip(CircleShape).background(c.surface.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                when {
                    out -> Pill("Out of Stock", c.faint, c.surface.copy(alpha = 0.92f))
                    count == 1 -> Pill("1 left", c.amber, c.surface.copy(alpha = 0.92f), dot = true)
                    else -> Pill("$count left", c.green, c.surface.copy(alpha = 0.92f), dot = true)
                }
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(p.name, style = Type.title(15.sp, 800), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(p.nameZh, style = Type.body(11.sp), color = c.faint)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (out) Pill("RESTOCKING", c.amber, c.amberTint)
                else p.tags.forEach { Pill(it, c.muted, c.surface2) }
            }
            if (eta != null) {
                Spacer(Modifier.height(6.dp))
                Text(eta, style = Type.body(10.sp, 600), color = c.amber)
            }
            Spacer(Modifier.height(10.dp))
            if (out && p.key != "Osmanthus" && !Features.campusMap) {
                // no station list without the map: the RESTOCKING pill above says it all
            } else if (out && p.key != "Osmanthus") {
                ForestButton(
                    "Other Nodes", { dispatch(Action.OpenSheet(SheetKind.Stations)) }, Modifier.fillMaxWidth(),
                    small = true, style = BtnStyle.Outline, icon = Ic.Vending,
                )
            } else if (out) {
                ForestButton(
                    if (watching) "Subscribed" else "Notify Me", { dispatch(Action.Notify) }, Modifier.fillMaxWidth(),
                    small = true, style = if (watching) BtnStyle.Green else BtnStyle.Tonal, icon = if (watching) Ic.Check else Ic.Bell,
                )
            } else if (Features.campusMap) {
                ForestButton("Navigate", { dispatch(Action.CoilNav(p.coil)) }, Modifier.fillMaxWidth(), small = true, style = BtnStyle.Outline, icon = Ic.Navigation)
            } else {
                Pill("Ready to dispense", c.green, c.greenTint, icon = Ic.Check)
            }
        }
    }
}

/** Only this text reads the clock, so the per-second tick recomposes one Text instead of the whole screen. */
@Composable
private fun HoldLeft(e: Escrow, now: State<Long>) {
    Text("Hold ${mmss(e.holdLeftMs(now.value))}", style = Type.mono(13.sp, 700), color = Color(0xFFFFB4AC))
}
