package com.chiforest.forestflow.feature.supplier

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiforest.forestflow.R
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.Coil
import com.chiforest.forestflow.core.FeedEvent
import com.chiforest.forestflow.core.FeedType
import com.chiforest.forestflow.core.Seed
import com.chiforest.forestflow.feature.ScreenColumn
import com.chiforest.forestflow.ui.components.BarChart
import com.chiforest.forestflow.ui.components.BtnStyle
import com.chiforest.forestflow.ui.components.Chip
import com.chiforest.forestflow.ui.components.Eyebrow
import com.chiforest.forestflow.ui.components.ForestButton
import com.chiforest.forestflow.ui.components.ForestCard
import com.chiforest.forestflow.ui.components.ForestSwitch
import com.chiforest.forestflow.ui.components.Hairline
import com.chiforest.forestflow.ui.components.Ic
import com.chiforest.forestflow.ui.components.LiveDot
import com.chiforest.forestflow.ui.components.PageTitle
import com.chiforest.forestflow.ui.components.Pill
import com.chiforest.forestflow.ui.components.ProductArt
import com.chiforest.forestflow.ui.components.StatTile
import com.chiforest.forestflow.ui.components.canColor
import com.chiforest.forestflow.ui.components.pressable
import com.chiforest.forestflow.ui.components.softCard
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Motion
import com.chiforest.forestflow.ui.theme.Type

// ===================================================================== Telemetry

@Composable
fun TelemetryScreen(s: AppState, clock: () -> String, dispatch: (Action) -> Unit) {
    val c = FF.c
    val sp = s.supplier
    ScreenColumn {
        PageTitle("Node 01 Admin · Taicang", "Telemetry")

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(c.blueTint, c.amberTint)))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Ic.Sun, null, tint = c.amber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("28°C · Sunny · Taicang", style = Type.title(14.sp, 700), color = c.ink)
                Text("Hall G dismissal incoming", style = Type.body(12.sp), color = c.muted)
            }
            Pill("Active Sync", c.green, c.surface, dot = true, pulse = true)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Dispensed", "${sp.dispensed}", Modifier.weight(1f), sub = "+14% vs Tue", subColor = c.green, icon = Ic.Vending)
            StatTile(
                "Fill", "${sp.networkFill}%", Modifier.weight(1f),
                sub = if (sp.networkFill < 50) "Low Stock" else "Healthy", subColor = if (sp.networkFill < 50) c.amber else c.green, icon = Ic.Drop,
                accent = if (sp.networkFill < 50) c.amber else c.ink,
            )
            StatTile("Next Surge", "13:50", Modifier.weight(1f), sub = "Hall G End", subColor = c.red, icon = Ic.Bolt)
        }

        // coil matrix
        ForestCard(Modifier.fillMaxWidth(), radius = 26.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Coil Tray Matrix", style = Type.title(17.sp, 800), color = c.ink)
                    Text("Node 01 · 16 coils · 15 cans each", style = Type.body(12.sp), color = c.faint)
                }
                Legend(c.red2, "Empty")
                Spacer(Modifier.width(8.dp))
                Legend(c.amber, "Low")
                Spacer(Modifier.width(8.dp))
                Legend(c.green, "Optimal")
            }
            Spacer(Modifier.height(14.dp))
            sp.coils.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { coil -> CoilTile(coil, coil.id == sp.selected, Modifier.weight(1f)) { dispatch(Action.SelectCoil(coil.id)) } }
                }
                Spacer(Modifier.height(8.dp))
            }
            val sel = sp.coil(sp.selected)
            if (sel != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface2).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${sel.id} · ${sel.name}", style = Type.title(15.sp, 800), color = c.ink)
                            Spacer(Modifier.width(6.dp))
                            when (sel.state) {
                                "empty" -> Pill("EMPTY", c.red, c.redTint)
                                "low" -> Pill("LOW", c.amber, c.amberTint)
                                else -> Pill("OPTIMAL", c.green, c.greenTint)
                            }
                        }
                        Text(
                            if (sel.state == "empty") "0 cans left · ${sp.queue} queued · Depleted at 13:41"
                            else "${sel.level}/15 cans · ${sel.level * 100 / 15}% fill",
                            style = Type.body(12.sp), color = c.faint,
                        )
                    }
                    ForestButton(
                        if (sel.level >= 15) "Full" else "Restock", { dispatch(Action.RestockCoil(clock())) },
                        small = true, enabled = sel.level < 15, icon = if (sel.level >= 15) Ic.Check else Ic.Refresh,
                    )
                }
            }
        }

        // AI predictive restock
        ForestCard(Modifier.fillMaxWidth(), radius = 26.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(c.blue, Color(0xFF7A5AF8)))), contentAlignment = Alignment.Center) {
                    Icon(Ic.Sparkles, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("AI Predictive Restock Engine", style = Type.title(15.sp, 800), color = c.ink, modifier = Modifier.weight(1f))
                Pill("Live ML", c.blue, c.blueTint, dot = true, pulse = true)
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.redTint).padding(14.dp)) {
                Text("White Peach Sparkling Water", style = Type.title(15.sp, 800), color = c.ink)
                Text(
                    if (sp.surges > 0) "stockout predicted 13:51 (surge ×${sp.surges}, +450 students, outdoor load)" else "stockout predicted 13:55 (+450 students, outdoor load)",
                    style = Type.body(13.sp, 600), color = c.red,
                )
                Text("Telemetry timestamp ${clock()} · temperature delta +2.4°C", style = Type.mono(11.sp, 500), color = c.faint)
            }
            Spacer(Modifier.height(12.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            Text("RECOMMENDED DISPATCH", style = Type.eyebrow(10.sp), color = c.faint)
            Text("3 cases White Peach · 2 cases Lychee Fizzy", style = Type.title(15.sp, 700), color = c.ink)
            Spacer(Modifier.height(12.dp))
            ForestButton(
                when {
                    sp.stage >= 4 -> "Restocked"
                    sp.stage >= 3 -> "Runner En Route · Open Dispatch"
                    else -> "Dispatch Runner"
                },
                { if (sp.stage >= 3) dispatch(Action.Tab(1)) else dispatch(Action.AiDispatch(clock())) },
                Modifier.fillMaxWidth(), style = BtnStyle.Dark, icon = Ic.Truck, enabled = sp.stage < 4,
            )
        }

        // velocity chart
        ForestCard(Modifier.fillMaxWidth(), radius = 26.dp) {
            Text("Dispense Velocity", style = Type.title(17.sp, 800), color = c.ink)
            Text("Cans per hour with upcoming surge forecast", style = Type.body(12.sp), color = c.faint)
            Spacer(Modifier.height(14.dp))
            BarChart(Seed.velocity, sp.velocitySel, { dispatch(Action.Velocity(it)) }, valueSuffix = " cans")
            Spacer(Modifier.height(10.dp))
            val (label, v) = Seed.velocity[sp.velocitySel]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (label == "14:00") "14:00 (Peak Surge 13:50)" else "$label window", style = Type.body(13.sp, 600), color = c.ink, modifier = Modifier.weight(1f))
                Pill(if (label == "14:00") "$v PEAK" else "$v cans", if (label == "14:00") c.red else c.ink, if (label == "14:00") c.redTint else c.surface2, mono = true)
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = Type.body(10.sp, 600), color = FF.c.faint)
    }
}

@Composable
private fun CoilTile(coil: Coil, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = FF.c
    val color = when (coil.state) {
        "empty" -> c.red2
        "low" -> c.amber
        else -> c.green
    }
    val border by animateColorAsState(if (selected) c.ink else Color.Transparent, label = "coilBorder")
    Column(
        modifier
            .aspectRatio(0.92f)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = if (c.isDark) 0.16f else 0.10f))
            .border(2.dp, border, RoundedCornerShape(14.dp))
            .semantics { this.selected = selected; contentDescription = "${coil.id} ${coil.name}, ${coil.level} of 15" }
            .pressable(scaleTo = 0.92f, onClick = onClick)
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(coil.id, style = Type.mono(11.sp, 800), color = c.ink, modifier = Modifier.weight(1f))
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        }
        Spacer(Modifier.weight(1f))
        Text(coil.name, style = Type.body(10.sp, 600), color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${coil.level}/15", style = Type.num(14.sp, 800), color = if (coil.level == 0) c.red2 else c.ink)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(c.bar)) {
            Box(Modifier.fillMaxWidth(coil.level / 15f).height(4.dp).clip(CircleShape).background(color))
        }
    }
}

// ===================================================================== Dispatch

@Composable
fun DispatchScreen(s: AppState, clock: () -> String, dispatch: (Action) -> Unit) {
    val c = FF.c
    val sp = s.supplier
    ScreenColumn {
        PageTitle("Logistics Console", "Dispatch")

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(c.navy2, c.navy))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Node 01 Restock Routine", style = Type.title(16.sp, 800), color = Color.White)
                Text("Batch #88 · ${sp.networkFill}% Remaining Capacity", style = Type.body(12.sp), color = Color.White.copy(alpha = 0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${sp.queue}", style = Type.num(26.sp, 800), color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveDot(Color(0xFFFF6A62), size = 6.dp)
                    Spacer(Modifier.width(5.dp))
                    Text("In Queue", style = Type.body(11.sp, 600), color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        // pipeline
        ForestCard(Modifier.fillMaxWidth(), radius = 26.dp) {
            Eyebrow("Pipeline Status") { Pill("Live Sync", c.green, c.greenTint, dot = true, pulse = true) }
            Spacer(Modifier.height(14.dp))
            val steps = listOf(
                Triple("Depletion Predicted", "13:21", Ic.Sparkles),
                Triple("Ticket Dispatched", "13:28", Ic.Activity),
                Triple("Runner En Route", "ETA 6m", Ic.Truck),
                Triple("Stock Restocked", "Pending", Ic.CheckCircle),
            )
            steps.forEachIndexed { i, (title, time, icon) ->
                val n = i + 1
                val done = sp.stage > n || sp.stage == 4
                val active = sp.stage == n && sp.stage < 4
                PipelineStep(title, if (active) "$time (Active)" else if (done && n == 4) "Done ${clock()}" else if (done) time else if (n == 4) "Pending" else time, icon, done, active, last = i == steps.lastIndex)
            }
        }

        // runner
        ForestCard(Modifier.fillMaxWidth(), radius = 26.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.runner_zhang), "Runner Zhang", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(CircleShape).border(2.dp, c.surface, CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Runner: Zhang", style = Type.title(16.sp, 800), color = c.ink)
                        Spacer(Modifier.width(6.dp))
                        Pill("Logistics 02", c.blue, c.blueTint)
                    }
                    Text("Transport: Smart Cart 04 · 2 Cases Loaded", style = Type.body(12.sp), color = c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (sp.stage >= 3) c.greenTint else c.surface2).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Ic.Route, null, tint = if (sp.stage >= 3) c.green else c.muted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("East Corridor Quad → Node 01", style = Type.body(13.sp, 600), color = c.ink, modifier = Modifier.weight(1f))
                Text(if (sp.stage >= 4) "Arrived" else "ETA 6 min", style = Type.mono(12.sp, 700), color = if (sp.stage >= 3) c.green else c.muted)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ForestButton(
                    if (sp.runnerPinged || sp.stage >= 3) "Pinged · En Route" else "Ping", { dispatch(Action.PingRunner) },
                    Modifier.weight(1f), style = if (sp.runnerPinged || sp.stage >= 3) BtnStyle.Green else BtnStyle.Dark,
                    icon = if (sp.runnerPinged || sp.stage >= 3) Ic.Check else Ic.Ping, enabled = sp.stage < 4,
                )
                ForestButton("Call", { dispatch(Action.CallRunner) }, Modifier.weight(1f), style = BtnStyle.Outline, icon = Ic.Phone)
            }
        }

        // manifest
        ForestCard(Modifier.fillMaxWidth(), radius = 26.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Restock Manifest", style = Type.title(17.sp, 800), color = c.ink)
                    Text("Node 01 Batch #88 · ${sp.dispensed} Cans Dispensed", style = Type.body(12.sp), color = c.faint)
                }
                Pill("6 Cases Total", c.ink, c.surface2)
            }
            Spacer(Modifier.height(8.dp))
            ManifestRow("White Peach", "White Peach · A1", "Coil A1 · 3 Cases (72 cans)", "HIGH", c.red, c.redTint, "Critical ${pct(sp.coil("A1"))}%")
            Hairline()
            ManifestRow("Lychee", "Lychee Fizzy · B2", "Coil B2 · 2 Cases (48 cans)", "MEDIUM", c.amber, c.amberTint, "Low ${pct(sp.coil("B2"))}%")
            Hairline()
            ManifestRow("Green Tea", "Green Tea Zero Sugar · C4", "Coil C4 · 1 Case (24 cans)", "LOW", c.green, c.greenTint, "Steady ${pct(sp.coil("C4"))}%")
        }

        // dynamic clearance
        AnimatedVisibility(sp.clearance != "dismissed", enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            if (sp.clearance == "approved") {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.greenTint).border(1.dp, c.green.copy(alpha = 0.3f), RoundedCornerShape(22.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Ic.CheckCircle, null, tint = c.green, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Price cut live · ¥4.40", style = Type.title(15.sp, 800), color = c.green)
                        Text("E-Ink shelf tags updated on Coil C3 (was ¥5.50)", style = Type.body(12.sp), color = c.muted)
                    }
                }
            } else {
                ForestCard(Modifier.fillMaxWidth().border(1.5.dp, c.amber.copy(alpha = 0.35f), RoundedCornerShape(26.dp)), radius = 26.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Ic.Tag, null, tint = c.amber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("DYNAMIC CLEARANCE TRIGGERED", style = Type.eyebrow(10.sp), color = c.amber, modifier = Modifier.weight(1f))
                        Pill("Coil C3", c.ink, c.surface2)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProductArt("Osmanthus", Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Osmanthus Plum Sparkling", style = Type.title(15.sp, 800), color = c.ink)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("¥5.50", style = Type.num(13.sp, 600).copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough), color = c.faint)
                                Spacer(Modifier.width(6.dp))
                                Text("¥4.40", style = Type.num(17.sp, 800), color = c.red)
                                Spacer(Modifier.width(6.dp))
                                Pill("−20%", c.red, c.redTint)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Slow-moving item detected. Automated price markdown suggested to purge remaining coil C3 inventory prior to incoming tea stock.", style = Type.body(12.sp), color = c.muted)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ForestButton("Approve Price Cut", { dispatch(Action.Approve(clock())) }, Modifier.weight(1f), icon = Ic.Check)
                        ForestButton("Dismiss", { dispatch(Action.Decline) }, style = BtnStyle.Outline)
                    }
                }
            }
        }

        // surge simulation
        ForestCard(Modifier.fillMaxWidth(), radius = 26.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SIMULATION ENGINE · DEMO DAY", style = Type.eyebrow(10.sp), color = c.faint, modifier = Modifier.weight(1f))
                Pill("Predictive AI", c.blue, c.blueTint, icon = Ic.Sparkles)
            }
            Spacer(Modifier.height(10.dp))
            ForestButton("Simulate Foot-Traffic Surge", { dispatch(Action.Surge(clock())) }, Modifier.fillMaxWidth(), style = BtnStyle.Tonal, icon = Ic.Bolt)
            Spacer(Modifier.height(6.dp))
            Text("450 students · Hall G · 13:50 · 28°C", style = Type.mono(11.sp, 500), color = c.faint, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Column {
            ForestButton(
                if (sp.restockConfirmed) "Restock Confirmed" else "Confirm Restock Loaded",
                { dispatch(Action.ConfirmRestock(clock())) }, Modifier.fillMaxWidth().height(58.dp),
                style = if (sp.restockConfirmed) BtnStyle.Green else BtnStyle.Primary,
                icon = if (sp.restockConfirmed) Ic.CheckCircle else Ic.Box, enabled = !sp.restockConfirmed,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (sp.restockConfirmed) "Node 01 at 100% capacity · Campus Log 02 notified" else "Resets Node 01 to 100% capacity and notifies Campus Log 02.",
                style = Type.body(12.sp), color = c.faint, modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

private fun pct(c: Coil?): Int = "${((c?.level ?: 0) * 100) / 15}"

@Composable
private fun PipelineStep(title: String, time: String, icon: ImageVector, done: Boolean, active: Boolean, last: Boolean) {
    val c = FF.c
    val reduce = FF.reduceMotion
    val t = rememberInfiniteTransition(label = "step")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "stepPulse")
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                if (active && !reduce) Box(
                    Modifier.size(34.dp).graphicsLayer { scaleX = 1f + p * 0.5f; scaleY = 1f + p * 0.5f; alpha = 1f - p }
                        .clip(CircleShape).background(c.red2.copy(alpha = 0.35f)),
                )
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(
                        when {
                            done -> c.green
                            active -> c.red2
                            else -> c.surface2
                        },
                    ),
                    contentAlignment = Alignment.Center,
                ) { Icon(if (done) Ic.Check else icon, null, tint = if (done || active) Color.White else c.faint, modifier = Modifier.size(16.dp)) }
            }
            if (!last) Box(Modifier.width(2.dp).height(26.dp).background(if (done) c.green else c.line))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(top = 6.dp)) {
            Text(title, style = Type.title(14.sp, if (active) 800 else 700), color = if (done || active) c.ink else c.faint)
            Text(time, style = Type.mono(11.sp, 500), color = if (active) c.red else c.faint)
        }
    }
}

@Composable
private fun ManifestRow(product: String, title: String, sub: String, priority: String, fg: Color, bg: Color, level: String) {
    val c = FF.c
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        ProductArt(product, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(canColor(product).copy(alpha = 0.12f)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.title(14.sp, 700), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = Type.body(12.sp), color = c.faint)
        }
        Column(horizontalAlignment = Alignment.End) {
            Pill(priority, fg, bg)
            Spacer(Modifier.height(3.dp))
            Text(level, style = Type.mono(10.sp, 600), color = c.faint)
        }
    }
}

// ===================================================================== Alerts

@Composable
fun AlertsScreen(s: AppState, dispatch: (Action) -> Unit) {
    val c = FF.c
    val sp = s.supplier
    val counts = mapOf(
        "stockout" to sp.feed.count { it.type == FeedType.Stockout },
        "restock" to sp.feed.count { it.type == FeedType.Restock },
        "escrow" to sp.feed.count { it.type == FeedType.Escrow },
        "clearance" to sp.feed.count { it.type == FeedType.Clearance },
    )
    val visible = sp.feed.filter {
        when (sp.alertFilter) {
            "stockout" -> it.type == FeedType.Stockout
            "restock" -> it.type == FeedType.Restock
            "escrow" -> it.type == FeedType.Escrow
            "clearance" -> it.type == FeedType.Clearance
            else -> true
        }
    }
    ScreenColumn {
        PageTitle("Admin · Alerts & Activity", "Alerts")

        ForestCard(Modifier.fillMaxWidth(), radius = 24.dp, padding = PaddingValues(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(if (sp.muted) c.surface2 else c.redTint), contentAlignment = Alignment.Center) {
                    Icon(if (sp.muted) Ic.BellOff else Ic.Bell, null, tint = if (sp.muted) c.faint else c.red, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Live Alert Gateway", style = Type.title(15.sp, 800), color = c.ink)
                        Spacer(Modifier.width(6.dp))
                        if (!sp.muted) LiveDot(c.green, size = 6.dp)
                    }
                    Text("Push pipeline latency: 28ms · Node 01 queue auto-managed", style = Type.body(11.sp), color = c.faint)
                }
                ForestSwitch(!sp.muted, { dispatch(Action.Mute) }, "Alert gateway live")
            }
        }

        AnimatedVisibility(!sp.urgentResolved, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFB7131A), Color(0xFF8E0E14))))
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("URGENT", Color(0xFFB7131A), Color.White, icon = Ic.Alert)
                    Spacer(Modifier.weight(1f))
                    Text("1 empty coil · ${sp.queue} students waiting", style = Type.body(12.sp, 700), color = Color.White.copy(alpha = 0.9f))
                }
                Spacer(Modifier.height(12.dp))
                Text("Coil C3 (Osmanthus Plum) depleted.", style = Type.title(18.sp, 800), color = Color.White)
                Text("FIFO auto-notification armed upon runner check-in.", style = Type.body(13.sp), color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painterResource(R.drawable.runner_zhang), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp).clip(CircleShape).border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Assigned Campus Runner", style = Type.body(10.sp, 600), color = Color.White.copy(alpha = 0.7f))
                        Text("Zhang (Zone A3 Hub)", style = Type.title(14.sp, 800), color = Color.White)
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = if (sp.urgentPinged) 0.18f else 1f))
                            .pressable(enabled = !sp.urgentPinged, scaleTo = 0.94f) { dispatch(Action.Ping) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (sp.urgentPinged) Ic.Check else Ic.Ping, null, tint = if (sp.urgentPinged) Color.White else Color(0xFFB7131A), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (sp.urgentPinged) "Runner Pinged" else "Ping Runner", style = Type.title(13.sp, 800), color = if (sp.urgentPinged) Color.White else Color(0xFFB7131A))
                        }
                    }
                }
            }
        }
        if (sp.urgentResolved) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.greenTint).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Ic.CheckCircle, null, tint = c.green, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("No urgent alerts · all coils stocked", style = Type.title(14.sp, 700), color = c.green)
            }
        }

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("All (${sp.feed.size})", sp.alertFilter == "all", { dispatch(Action.AlertFilter("all")) })
            Chip("Stockouts (${counts["stockout"]})", sp.alertFilter == "stockout", { dispatch(Action.AlertFilter("stockout")) })
            Chip("Restocks (${counts["restock"]})", sp.alertFilter == "restock", { dispatch(Action.AlertFilter("restock")) })
            Chip("VIP Escrows (${counts["escrow"]})", sp.alertFilter == "escrow", { dispatch(Action.AlertFilter("escrow")) })
            Chip("Clearance (${counts["clearance"]})", sp.alertFilter == "clearance", { dispatch(Action.AlertFilter("clearance")) })
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (visible.isEmpty()) {
                Text("No events in this filter yet.", style = Type.body(13.sp), color = c.faint, modifier = Modifier.padding(8.dp))
            }
            visible.forEach { ev -> key(ev.id) { RiseIn { EventCard(ev) } } }
        }

        // peak surge mini chart
        ForestCard(Modifier.fillMaxWidth(), radius = 24.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Peak Surge Telemetry", style = Type.title(15.sp, 800), color = c.ink)
                    Text("13:30 – 14:00 Window · transactions/min", style = Type.body(11.sp), color = c.faint)
                }
                Pill("Guard Active", c.green, c.greenTint, icon = Ic.Shield)
            }
            Spacer(Modifier.height(12.dp))
            val mini = listOf("13:30" to 6, "35" to 9, "40" to 12, "45" to 17, "50" to 24 + sp.surges * 4, "55" to 19, "14:00" to 14)
            BarChart(mini, 4, {}, height = 90.dp, valueSuffix = " tx/min")
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface2).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveDot(c.green, size = 8.dp)
            Spacer(Modifier.width(10.dp))
            Text("All systems operational", style = Type.body(13.sp, 600), color = c.ink, modifier = Modifier.weight(1f))
            Text("28ms · 11 nodes", style = Type.mono(11.sp, 600), color = c.faint)
        }
    }
}

/** New feed items rise into place (12dp + fade). */
@Composable
private fun RiseIn(content: @Composable () -> Unit) {
    val a = remember { Animatable(0f) }
    val dy = with(LocalDensity.current) { 12.dp.toPx() }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(420, easing = Motion.EaseOutQuint)) }
    Box(Modifier.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * dy }) { content() }
}

@Composable
private fun EventCard(ev: FeedEvent) {
    val c = FF.c
    val (icon, fg, bg) = when (ev.type) {
        FeedType.Escrow -> Triple(Ic.Lock, c.blue, c.blueTint)
        FeedType.Restock -> Triple(Ic.CheckCircle, c.green, c.greenTint)
        FeedType.Fifo -> Triple(Ic.Users, c.blue, c.blueTint)
        FeedType.Clearance -> Triple(Ic.Tag, c.amber, c.amberTint)
        FeedType.Stockout -> Triple(Ic.Alert, c.red, c.redTint)
        FeedType.Surge -> Triple(Ic.Bolt, c.red, c.redTint)
        FeedType.Dispatch -> Triple(Ic.Truck, c.ink, c.surface2)
    }
    Row(Modifier.fillMaxWidth().softCard(RoundedCornerShape(20.dp), elevated = false).padding(14.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ev.title, style = Type.title(14.sp, 800), color = c.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ev.time, style = Type.mono(10.sp, 500), color = c.faint)
            }
            Text(ev.subtitle, style = Type.body(12.sp, 700), color = fg)
            Spacer(Modifier.height(4.dp))
            Text(ev.body, style = Type.body(12.sp), color = c.muted)
            if (ev.chips.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ev.chips.forEach { Pill(it, c.muted, c.surface2) }
                }
            }
        }
    }
}
