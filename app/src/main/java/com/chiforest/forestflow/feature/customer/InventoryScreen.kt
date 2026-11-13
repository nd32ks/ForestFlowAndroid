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
import com.chiforest.forestflow.core.Clearance
import com.chiforest.forestflow.core.Nearby
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.chiforest.forestflow.ui.components.ForestSheet
import com.chiforest.forestflow.ui.components.SheetTitle
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
                Text("Nearest machine · ${node.shortName} · Building ${node.building}", style = Type.body(12.sp, 700), color = c.red, modifier = Modifier.weight(1f))
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
                    QuickAction(Ic.Route, "Route", "Walk there", Modifier.weight(1f)) { dispatch(Action.Quick("Route", System.currentTimeMillis())) }
                    QuickAction(Ic.MapPin, "I'm Here", "Check in", Modifier.weight(1f), active = s.youAreHereNodeId == node.id) { dispatch(Action.Quick("I'm Here", System.currentTimeMillis())) }
                }
                if (Features.vipClub) {
                    QuickAction(Ic.Lock, "Hold", "5 min", Modifier.weight(1f), active = s.customer.escrow != null) { dispatch(Action.Quick("Lock", System.currentTimeMillis())) }
                }
                QuickAction(Ic.Bell, "Alerts", "Ping me", Modifier.weight(1f)) { dispatch(Action.Tab(2)) }
                QuickAction(Ic.Share, "Share", "Send", Modifier.weight(1f)) { dispatch(Action.Quick("Share", System.currentTimeMillis())) }
            }
        }

        DropAlertsBar(s.customer.dropAlerts) { dispatch(Action.DropAlerts) }

        AskCard(DrinkCatalog.options(Seed.products, node.ready), s.customer.osmanthusWatch, { dispatch(Action.Notify) }, { dispatch(Action.ProductDetails(it)) })

        Column {
            Eyebrow("Drinks in this machine") {
                Pill("${Seed.products.size} Slots", c.ink, c.surface)
                Spacer(Modifier.width(6.dp))
                Pill("${node.tempC}°C ${if (node.tempC < 4f) "Ice cold" else "Chilled"}", if (node.tempC < 4f) c.blue else c.amber, if (node.tempC < 4f) c.blueTint else c.amberTint, icon = Ic.Snow)
            }
            Spacer(Modifier.height(10.dp))
            val products = Seed.products
            for (row in products.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { p ->
                        val count = node.ready[p.key] ?: 0
                        val eta = if (count <= 0) RestockEta.forNode(node.id, s.supplier)?.label else null
                        CoilCard(p, count, isWatching(s, p.key), Modifier.weight(1f), dispatch, eta, if (count <= 0) Nearby.nearest(map, node.id, p.key) else null)
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
                        Text("1 can on hold for you", style = Type.title(15.sp, 800), color = Color.White)
                        Text("${e.product} · ${map.node(e.nodeId)?.shortName}, slot ${e.coil}", style = Type.body(12.sp), color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        HoldLeft(e, now)
                    }
                    ForestButton("Release", { dispatch(Action.Release) }, small = true, style = BtnStyle.Primary)
                }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, hint: String, modifier: Modifier, active: Boolean = false, onClick: () -> Unit) {
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
        Text(hint, style = Type.body(9.sp, 500), color = c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

@Composable
private fun CoilCard(p: Product, count: Int, watching: Boolean, modifier: Modifier, dispatch: (Action) -> Unit, eta: String? = null, nearest: com.chiforest.forestflow.core.MapNode? = null) {
    val c = FF.c
    val out = count <= 0
    Column(modifier.softCard(RoundedCornerShape(22.dp)).pressable(scaleTo = 0.97f, label = "${p.name} details") { dispatch(Action.ProductDetails(p.key)) }) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.25f).clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))) {
            ProductArt(p.key, Modifier.matchParentSize(), outOfStock = out)
            Text(
                "Slot ${p.coil}", style = Type.eyebrow(9.sp), color = c.ink,
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
            if (out) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (nearest != null) "Sold out here · nearest: ${nearest.shortName}, ${nearest.walkMin} min walk" else "Sold out at every machine · tap Notify Me",
                    style = Type.body(10.sp, 600), color = c.muted,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (out) {
                ForestButton(
                    if (watching) "Subscribed" else "Notify Me", { dispatch(Action.Watch(p.key)) }, Modifier.fillMaxWidth(),
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

/** Opened by tapping a drink card: what the drink is, what it tastes like, and where to find it. */
@Composable
fun ProductSheet(s: AppState, map: MapData, dispatch: (Action) -> Unit) {
    val c = FF.c
    val p = Seed.product(s.productKey) ?: return
    val node = map.node(s.activeNodeId) ?: map.allNodes.first()
    val here = node.ready[p.key] ?: 0
    val elsewhere = map.allNodes.filter { it.id != node.id && (it.ready[p.key] ?: 0) > 0 }.sortedBy { it.walkMin }
    ForestSheet(onDismiss = { dispatch(Action.CloseSheet) }) {
        SheetTitle(p.name, "${p.info.brand} · ${p.nameZh} · ${p.tags.first()}") { dispatch(Action.CloseSheet) }
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductArt(p.key, Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)), outOfStock = here <= 0)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(Clearance.yuan(Clearance.LIST_PRICE), style = Type.num(22.sp, 800), color = c.ink)
                    Spacer(Modifier.height(4.dp))
                    if (here > 0) Pill("$here ready at ${node.shortName}", c.green, c.greenTint, dot = true)
                    else Pill("Out of stock at ${node.shortName}", c.faint, c.surface2)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(p.info.description, style = Type.body(14.sp), color = c.ink)
            p.info.labelNote?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = Type.body(12.sp, 600), color = c.muted)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Fact("Sugar", p.info.sugarG?.let { if (it == 0.0) "0 g" else "$it g" } ?: "See label", Modifier.weight(1f))
                Fact("Energy", p.info.kcal?.let { "$it kcal" } ?: "See label", Modifier.weight(1f))
                Fact("Caffeine", p.info.caffeine, Modifier.weight(1.2f))
            }
            Spacer(Modifier.height(14.dp))
            Text("TASTE", style = Type.eyebrow(10.sp), color = c.faint)
            Spacer(Modifier.height(4.dp))
            Text(p.info.notes, style = Type.body(13.sp, 600), color = c.muted)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { p.tags.forEach { Pill(it, c.muted, c.surface2) } }
            Spacer(Modifier.height(16.dp))
            Text("ALSO IN STOCK AT", style = Type.eyebrow(10.sp), color = c.faint)
            Spacer(Modifier.height(4.dp))
            if (elsewhere.isEmpty()) Text("No other station has it right now.", style = Type.body(13.sp), color = c.faint)
            elsewhere.take(4).forEach { n ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${n.shortName} · ${n.label}", style = Type.body(13.sp, 600), color = c.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${n.ready[p.key]} · ${n.walkMin} min", style = Type.mono(12.sp, 600), color = c.faint)
                }
            }
            Spacer(Modifier.height(14.dp))
            ForestButton(
                if (here > 0) "Where is it? Right here at ${node.shortName}" else "Take me to the nearest one",
                { dispatch(Action.FindDrink(p.key)) }, Modifier.fillMaxWidth(), style = BtnStyle.Outline, icon = Ic.Navigation,
            )
            Spacer(Modifier.height(10.dp))
            val following = isWatching(s, p.key)
            ForestButton(
                if (following) "Following · tap to stop alerts" else "Notify me when it's restocked", { dispatch(Action.Watch(p.key)) },
                Modifier.fillMaxWidth(), style = if (following) BtnStyle.Green else BtnStyle.Primary, icon = if (following) Ic.Check else Ic.Bell,
            )
            Spacer(Modifier.height(10.dp))
            Text("Per container, from the label or brand. \"See label\" means we could not confirm the figure.", style = Type.body(11.sp), color = c.faint)
        }
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier) {
    val c = FF.c
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(c.surface2).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(label, style = Type.body(11.sp, 600), color = c.faint)
        Text(value, style = Type.title(15.sp, 800), color = c.ink)
    }
}

fun isWatching(s: AppState, key: String): Boolean =
    key in s.customer.watch || (key == "Osmanthus" && s.customer.osmanthusWatch)

/** One tap to get phone notifications for every new product drop. */
@Composable
fun DropAlertsBar(on: Boolean, onToggle: () -> Unit) {
    val c = FF.c
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (on) c.greenTint else c.redTint)
            .semantics { contentDescription = if (on) "Product drop alerts on" else "Turn on product drop alerts" }
            .pressable(scaleTo = 0.98f, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (on) Ic.Check else Ic.Bell, null, tint = if (on) c.green else c.red, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(if (on) "Drop alerts on" else "Get drop alerts", style = Type.title(14.sp, 800), color = c.ink)
            Text(
                if (on) "Your phone will buzz when a new flavour lands" else "One tap · phone notification for every new flavour",
                style = Type.body(11.sp), color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text(if (on) "On" else "Turn on", style = Type.body(12.sp, 800), color = if (on) c.green else c.red)
    }
}
