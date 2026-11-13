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
import androidx.compose.ui.draw.clip
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
import com.chiforest.forestflow.core.Clearance
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
fun TelemetryScreen(s: AppState, map: com.chiforest.forestflow.core.MapData, clock: () -> String, dispatch: (Action) -> Unit) {
    val c = FF.c
    val sp = s.supplier
    val mine = map.node("N01")
    ScreenColumn {
        PageTitle(mine?.shortName ?: "Machine 01", "Telemetry")

        // Which machine this page is about, and how to see the others (user testing: "Node 01" and switching campus were unclear)
        ForestCard(Modifier.fillMaxWidth(), radius = 22.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Ic.MapPin, null, tint = c.red, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(mine?.location ?: "Bldg G Ground Floor Atrium", style = Type.title(15.sp, 800), color = c.ink)
                    Text("${map.campusOfNode("N01")?.name ?: "Taicang"} campus · the machine you look after", style = Type.body(12.sp), color = c.muted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("This page shows only this machine. To check machines on the other campus (Taicang or SIP), open the Stock tab and pick the campus at the top.", style = Type.body(12.sp), color = c.muted)
            Spacer(Modifier.height(10.dp))
            ForestButton("See all machines, Taicang and SIP", { dispatch(Action.Tab(3)) }, Modifier.fillMaxWidth(), small = true, style = BtnStyle.Outline, icon = Ic.Box)
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(c.blueTint, c.amberTint)))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Ic.Sun, null, tint = c.amber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Rush expected at 13:50", style = Type.title(14.sp, 700), color = c.ink)
                Text("Lectures in Hall G end then, and it’s 28°C outside, so cold drinks will sell fast.", style = Type.body(12.sp), color = c.muted)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Sold today", "${sp.dispensed}", Modifier.weight(1f), sub = "cans", subColor = c.muted, icon = Ic.Vending)
            StatTile(
                "Stock", "${sp.networkFill}%", Modifier.weight(1f),
                sub = if (sp.networkFill < 50) "Restock soon" else "Enough stock", subColor = if (sp.networkFill < 50) c.amber else c.green, icon = Ic.Drop,
                accent = if (sp.networkFill < 50) c.amber else c.ink,
            )
            StatTile("Next rush", "13:50", Modifier.weight(1f), sub = "Hall G lets out", subColor = c.red, icon = Ic.Bolt)
        }

        // coil matrix
        ForestCard(Modifier.fillMaxWidth(), radius = 26.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Stock by slot", style = Type.title(17.sp, 800), color = c.ink)
                    Text("16 slots, 15 cans each. Tap a slot to see it or restock it.", style = Type.body(12.sp), color = c.faint)
                }
                Legend(c.red2, "Empty")
                Spacer(Modifier.width(8.dp))
                Legend(c.amber, "Low")
                Spacer(Modifier.width(8.dp))
                Legend(c.green, "OK")
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
                            Text("Slot ${sel.id} · ${sel.name}", style = Type.title(15.sp, 800), color = c.ink)
                            Spacer(Modifier.width(6.dp))
                            when (sel.state) {
                                "empty" -> Pill("Empty", c.red, c.redTint)
                                "low" -> Pill("Low", c.amber, c.amberTint)
                                else -> Pill("OK", c.green, c.greenTint)
                            }
                        }
                        Text(
                            if (sel.state == "empty") "0 cans left · ran out at 13:41 · ${sp.queue} students waiting"
                            else "${sel.level} of 15 cans left",
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
                Text("Restock forecast", style = Type.title(15.sp, 800), color = c.ink, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.redTint).padding(14.dp)) {
                Text("White Peach Sparkling Water", style = Type.title(15.sp, 800), color = c.ink)
                Text(
                    if (sp.surges > 0) "Likely to run out at 13:51, earlier because of the rush" else "Likely to run out at 13:55, when 450 students leave Hall G",
                    style = Type.body(13.sp, 600), color = c.red,
                )
                Text("Forecast updated ${clock()}", style = Type.body(11.sp, 500), color = c.faint)
            }
            Spacer(Modifier.height(12.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            Text("Suggested: send the runner with", style = Type.body(12.sp, 600), color = c.faint)
            Text("3 cases White Peach and 2 cases Lychee Fizzy", style = Type.title(15.sp, 700), color = c.ink)
            Spacer(Modifier.height(12.dp))
            ForestButton(
                when {
                    sp.stage >= 4 -> "Restocked"
                    sp.stage >= 3 -> "Runner on the way · see Dispatch"
                    else -> "Send runner"
                },
                { if (sp.stage >= 3) dispatch(Action.Tab(1)) else dispatch(Action.AiDispatch(clock())) },
                Modifier.fillMaxWidth(), style = BtnStyle.Dark, icon = Ic.Truck, enabled = sp.stage < 4,
            )
        }

        // velocity chart
        ForestCard(Modifier.fillMaxWidth(), radius = 26.dp) {
            Text("Cans sold per hour", style = Type.title(17.sp, 800), color = c.ink)
            Text("Tap a bar to see that hour. 14:00 is the expected rush.", style = Type.body(12.sp), color = c.faint)
            Spacer(Modifier.height(14.dp))
            BarChart(Seed.velocity, sp.velocitySel, { dispatch(Action.Velocity(it)) }, valueSuffix = " cans")
            Spacer(Modifier.height(10.dp))
            val (label, v) = Seed.velocity[sp.velocitySel]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (label == "14:00") "14:00 · expected rush" else "Around $label", style = Type.body(13.sp, 600), color = c.ink, modifier = Modifier.weight(1f))
                Pill(if (label == "14:00") "$v cans, peak" else "$v cans", if (label == "14:00") c.red else c.ink, if (label == "14:00") c.redTint else c.surface2, mono = true)
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
        PageTitle("Restock run", "Dispatch")

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(c.navy2, c.navy))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Restock for Machine 01", style = Type.title(16.sp, 800), color = Color.White)
                Text("Machine is ${sp.networkFill}% full", style = Type.body(12.sp), color = Color.White.copy(alpha = 0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${sp.queue}", style = Type.num(26.sp, 800), color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveDot(Color(0xFFFF6A62), size = 6.dp)
                    Spacer(Modifier.width(5.dp))
                    Text("students waiting", style = Type.body(11.sp, 600), color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        // pipeline
        ForestCard(Modifier.fillMaxWidth(), radius = 26.dp) {
            Eyebrow("Progress")
            Spacer(Modifier.height(14.dp))
            val steps = listOf(
                Triple("Run-out forecast", "13:21", Ic.Activity),
                Triple("Runner sent", "13:28", Ic.Activity),
                Triple("Runner on the way", "6 min away", Ic.Truck),
                Triple("Machine restocked", "Not yet", Ic.CheckCircle),
            )
            steps.forEachIndexed { i, (title, time, icon) ->
                val n = i + 1
                val done = sp.stage > n || sp.stage == 4
                val active = sp.stage == n && sp.stage < 4
                PipelineStep(title, if (active) "$time · now" else if (done && n == 4) "Done ${clock()}" else if (done) time else if (n == 4) "Not yet" else time, icon, done, active, last = i == steps.lastIndex)
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
                        Text("Runner Zhang", style = Type.title(16.sp, 800), color = c.ink)
                        Spacer(Modifier.width(6.dp))
                        
                    }
                    Text("Carrying 2 cases on cart 04", style = Type.body(12.sp), color = c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (sp.stage >= 3) c.greenTint else c.surface2).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Ic.Route, null, tint = if (sp.stage >= 3) c.green else c.muted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("From East Corridor Quad to Machine 01", style = Type.body(13.sp, 600), color = c.ink, modifier = Modifier.weight(1f))
                Text(if (sp.stage >= 4) "Arrived" else "ETA 6 min", style = Type.mono(12.sp, 700), color = if (sp.stage >= 3) c.green else c.muted)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ForestButton(
                    if (sp.runnerPinged || sp.stage >= 3) "On the way" else "Ping", { dispatch(Action.PingRunner) },
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
                    Text("What to load", style = Type.title(17.sp, 800), color = c.ink)
                    Text("${sp.dispensed} cans sold today at Machine 01", style = Type.body(12.sp), color = c.faint)
                }
                Pill("6 cases", c.ink, c.surface2)
            }
            Spacer(Modifier.height(8.dp))
            ManifestRow("White Peach", "White Peach · A1", "Slot A1 · 3 cases (72 cans)", sp.coil("A1"))
            Hairline()
            ManifestRow("Lychee", "Lychee Fizzy · B2", "Slot B2 · 2 cases (48 cans)", sp.coil("B2"))
            Hairline()
            ManifestRow("Green Tea", "Green Tea Zero Sugar · C4", "Slot C4 · 1 case (24 cans)", sp.coil("C4"))
        }

        // dynamic clearance: live markdowns, then the next suggestion (slowest seller over the threshold)
        Clearance.live(sp).forEach { l ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.greenTint).border(1.dp, c.green.copy(alpha = 0.3f), RoundedCornerShape(22.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Ic.CheckCircle, null, tint = c.green, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Price cut live · ${l.name} ${Clearance.yuan(l.price)}", style = Type.title(15.sp, 800), color = c.green)
                    Text("List ${Clearance.yuan(Clearance.LIST_PRICE)} · ${l.stock} cans · ${Clearance.hoursLabel(l.hours)}", style = Type.body(12.sp), color = c.muted)
                }
                ForestButton("End", { dispatch(Action.EndClearance(l.name, clock())) }, small = true, style = BtnStyle.Outline)
            }
        }
        val cand = Clearance.candidate(sp)
        AnimatedVisibility(cand != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            if (cand != null) {
                val to = Clearance.salePrice(cand.price)
                ForestCard(Modifier.fillMaxWidth().border(1.5.dp, c.amber.copy(alpha = 0.35f), RoundedCornerShape(26.dp)), radius = 26.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Ic.Tag, null, tint = c.amber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Suggested price cut", style = Type.body(12.sp, 700), color = c.amber, modifier = Modifier.weight(1f))
                        Pill("Slot ${cand.coils.joinToString(", ")}", c.ink, c.surface2)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProductArt(cand.name, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(cand.name, style = Type.title(15.sp, 800), color = c.ink)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(Clearance.yuan(cand.price), style = Type.num(13.sp, 600).copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough), color = c.faint)
                                Spacer(Modifier.width(6.dp))
                                Text(Clearance.yuan(to), style = Type.num(17.sp, 800), color = c.red)
                                Spacer(Modifier.width(6.dp))
                                Pill("−${Clearance.DISCOUNT_PCT}%", c.red, c.redTint)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This drink sells slowest: ${cand.stock} cans would take ${Clearance.hoursLabel(cand.hours).removeSuffix(" to sell out")} to sell. A cheaper price helps clear it.",
                        style = Type.body(12.sp), color = c.muted,
                    )
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
                Text("Demo: try a rush", style = Type.body(12.sp, 700), color = c.faint, modifier = Modifier.weight(1f))
                
            }
            Spacer(Modifier.height(10.dp))
            ForestButton("Simulate a lunch rush", { dispatch(Action.Surge(clock())) }, Modifier.fillMaxWidth(), style = BtnStyle.Tonal, icon = Ic.Bolt)
            Spacer(Modifier.height(6.dp))
            Text("Pretends 450 students just left Hall G at 13:50", style = Type.body(11.sp, 500), color = c.faint, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Column {
            ForestButton(
                if (sp.restockConfirmed) "Restock done" else "Mark restock as done",
                { dispatch(Action.ConfirmRestock(clock())) }, Modifier.fillMaxWidth().height(58.dp),
                style = if (sp.restockConfirmed) BtnStyle.Green else BtnStyle.Primary,
                icon = if (sp.restockConfirmed) Ic.CheckCircle else Ic.Box, enabled = !sp.restockConfirmed,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (sp.restockConfirmed) "Machine 01 is full again. Waiting students have been told." else "Tap when the runner has filled the machine. Waiting students get a message.",
                style = Type.body(12.sp), color = c.faint, modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

private fun pct(c: Coil?) = ((c?.level ?: 0) * 100) / 15

/** Manifest wording follows the live fill level, so a refilled coil never reads "Critical 100%". */
internal fun fillLabel(pct: Int) = when {
    pct >= 100 -> "Full"
    pct <= 20 -> "Critical"
    pct <= 50 -> "Low"
    else -> "Steady"
}

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
private fun ManifestRow(product: String, title: String, sub: String, coil: Coil?) {
    val c = FF.c
    val p = pct(coil)
    val level = "${fillLabel(p)} $p%"
    val (priority, fg, bg) = when {
        p >= 100 -> Triple("LOADED", c.green, c.greenTint)
        p <= 20 -> Triple("HIGH", c.red, c.redTint)
        p <= 50 -> Triple("MEDIUM", c.amber, c.amberTint)
        else -> Triple("LOW", c.green, c.greenTint)
    }
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
        PageTitle("Alerts", "Alerts")

        ForestCard(Modifier.fillMaxWidth(), radius = 24.dp, padding = PaddingValues(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(if (sp.muted) c.surface2 else c.redTint), contentAlignment = Alignment.Center) {
                    Icon(if (sp.muted) Ic.BellOff else Ic.Bell, null, tint = if (sp.muted) c.faint else c.red, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Alerts to your phone", style = Type.title(15.sp, 800), color = c.ink)
                        Spacer(Modifier.width(6.dp))
                        if (!sp.muted) LiveDot(c.green, size = 6.dp)
                    }
                    Text("You get a notification when a slot runs out or a runner arrives.", style = Type.body(11.sp), color = c.faint)
                }
                ForestSwitch(!sp.muted, { dispatch(Action.Mute) }, "Phone alerts on")
            }
        }

        AnimatedVisibility(!sp.urgentResolved, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFB7131A), Color(0xFF8E0E14))))
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill("Needs action", Color(0xFFB7131A), Color.White, icon = Ic.Alert)
                    Spacer(Modifier.weight(1f))
                    Text("1 empty slot · ${sp.queue} students waiting", style = Type.body(12.sp, 700), color = Color.White.copy(alpha = 0.9f))
                }
                Spacer(Modifier.height(12.dp))
                Text("Slot C3 (Osmanthus Plum) is empty.", style = Type.title(18.sp, 800), color = Color.White)
                Text("When the runner refills it, waiting students are told in the order they asked.", style = Type.body(13.sp), color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painterResource(R.drawable.runner_zhang), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp).clip(CircleShape).border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Runner on this job", style = Type.body(10.sp, 600), color = Color.White.copy(alpha = 0.7f))
                        Text("Zhang", style = Type.title(14.sp, 800), color = Color.White)
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
                Text("Nothing needs action. Every slot has stock.", style = Type.title(14.sp, 700), color = c.green)
            }
        }

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("All (${sp.feed.size})", sp.alertFilter == "all", { dispatch(Action.AlertFilter("all")) })
            Chip("Run out (${counts["stockout"]})", sp.alertFilter == "stockout", { dispatch(Action.AlertFilter("stockout")) })
            Chip("Restocks (${counts["restock"]})", sp.alertFilter == "restock", { dispatch(Action.AlertFilter("restock")) })
            Chip("Holds (${counts["escrow"]})", sp.alertFilter == "escrow", { dispatch(Action.AlertFilter("escrow")) })
            Chip("Price cuts (${counts["clearance"]})", sp.alertFilter == "clearance", { dispatch(Action.AlertFilter("clearance")) })
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (visible.isEmpty()) {
                Text("Nothing here yet. Pick another filter to see more.", style = Type.body(13.sp), color = c.faint, modifier = Modifier.padding(8.dp))
            }
            visible.forEach { ev -> key(ev.id) { RiseIn { EventCard(ev) } } }
        }

        // peak surge mini chart
        ForestCard(Modifier.fillMaxWidth(), radius = 24.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sales during the rush", style = Type.title(15.sp, 800), color = c.ink)
                    Text("13:30 to 14:00, sales per minute", style = Type.body(11.sp), color = c.faint)
                }
                
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
            Text("All machines are online", style = Type.body(13.sp, 600), color = c.ink, modifier = Modifier.weight(1f))
            Text("11 machines", style = Type.body(11.sp, 600), color = c.faint)
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

/** Every station on both campuses, emptiest first, with per-flavour stock. Same view as the Softr Supplier Stock page. */
@Composable
fun StockScreen(s: AppState, map: com.chiforest.forestflow.core.MapData, dispatch: (Action) -> Unit) {
    val c = FF.c
    val campus = s.campusId.takeIf { id -> map.campuses.any { it.id == id } }
    val nodes = (if (campus == null) map.allNodes else map.campus(campus).nodes).sortedBy { it.pct }
    val keys = com.chiforest.forestflow.core.Seed.products.map { it.key }
    val low = 3
    ScreenColumn {
        PageTitle("Stock at every machine", "Stock")
        Text("Pick a campus to switch between Taicang and SIP. Emptiest machines are listed first.", style = Type.body(13.sp), color = c.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Campus", style = Type.body(12.sp, 700), color = c.faint)
            map.campuses.forEach { cp ->
                val on = cp.id == s.campusId
                Text(
                    cp.name, style = Type.body(12.sp, 700), color = if (on) Color.White else c.muted,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.ink else c.surface)
                        .pressable(scaleTo = 0.95f, label = "Show ${cp.name}") { dispatch(Action.StockCampus(cp.id)) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
        val totalLow = nodes.sumOf { n -> keys.count { (n.ready[it] ?: 0) <= low } }
        ForestCard(Modifier.fillMaxWidth(), radius = 26.dp) {
            Text("${nodes.size} machines · $totalLow drinks running low (3 or fewer)", style = Type.body(12.sp, 700), color = c.faint)
            Spacer(Modifier.height(8.dp))
            keys.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                    row.forEach { k ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(c.surface2).padding(8.dp)) {
                            Text(k, style = Type.body(10.sp, 600), color = c.faint, maxLines = 1)
                            Text("${nodes.sumOf { it.ready[k] ?: 0 }}", style = Type.num(16.sp, 800), color = c.ink)
                        }
                    }
                }
            }
        }
        nodes.forEach { n ->
            ForestCard(Modifier.fillMaxWidth(), radius = 22.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${n.shortName} · ${n.label}", style = Type.title(14.sp, 800), color = c.ink, maxLines = 1)
                        Text("Bldg ${n.building} · ${n.location}", style = Type.body(11.sp), color = c.faint, maxLines = 1)
                    }
                    Pill(if (n.isLow) "low" else "online", if (n.isLow) c.amber else c.green, if (n.isLow) c.amberTint else c.greenTint)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.surface2)) {
                    Box(Modifier.fillMaxWidth(n.pct.coerceIn(0, 100) / 100f).height(6.dp).background(if (n.pct < 40) c.red else c.green))
                }
                Text("${n.pct}% full · ${n.tempC}°C", style = Type.body(11.sp), color = c.faint, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(6.dp))
                keys.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                        row.forEach { k ->
                            val v = n.ready[k] ?: 0
                            val isLow = v <= low
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (isLow) c.redTint else c.surface2).padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("$v", style = Type.num(15.sp, 800), color = if (isLow) c.red else c.ink)
                                Text(k, style = Type.body(9.sp, 600), color = c.faint, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
