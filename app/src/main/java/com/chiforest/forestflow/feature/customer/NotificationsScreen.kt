package com.chiforest.forestflow.feature.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.RestockEta
import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.Features
import com.chiforest.forestflow.feature.ScreenColumn
import com.chiforest.forestflow.ui.components.BtnStyle
import com.chiforest.forestflow.ui.components.Chip
import com.chiforest.forestflow.ui.components.EmptyState
import com.chiforest.forestflow.ui.components.Eyebrow
import com.chiforest.forestflow.ui.components.ForestButton
import com.chiforest.forestflow.ui.components.ForestCard
import com.chiforest.forestflow.ui.components.ForestSwitch
import com.chiforest.forestflow.ui.components.Hairline
import com.chiforest.forestflow.ui.components.Ic
import com.chiforest.forestflow.ui.components.Pill
import com.chiforest.forestflow.ui.components.ProductArt
import com.chiforest.forestflow.ui.components.ToggleRow
import com.chiforest.forestflow.ui.components.PageTitle
import com.chiforest.forestflow.ui.components.softCard
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Type

@Composable
fun NotificationsScreen(s: AppState, dispatch: (Action) -> Unit) {
    val c = FF.c
    val cu = s.customer
    val vip = Features.vipClub
    // a filter saved while the VIP club was on may no longer exist
    val nf = if (!vip && cu.nf in listOf("escrow", "vip")) "all" else cu.nf
    val unread = s.unread(vip)
    val slide = with(LocalDensity.current) { 40.dp.roundToPx() }
    val exit = slideOutHorizontally { slide } + fadeOut() + shrinkVertically()
    ScreenColumn {
        PageTitle("Campus Notifications & Alerts", "Restock & Drops") {
            ForestButton("Mark All Read", { dispatch(Action.ReadAll) }, small = true, style = BtnStyle.Tonal, icon = Ic.Check, enabled = unread > 0)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(if (unread > 0) "All · $unread" else "All", nf == "all", { dispatch(Action.Nf("all")) })
            Chip("Restocks", nf == "restocks", { dispatch(Action.Nf("restocks")) }, icon = Ic.Box)
            if (vip) {
                Chip("Escrow & Drops", nf == "escrow", { dispatch(Action.Nf("escrow")) }, icon = Ic.Lock)
                Chip("VIP Privileges", nf == "vip", { dispatch(Action.Nf("vip")) }, icon = Ic.Star)
            }
        }

        DropAlertsBar(cu.dropAlerts) { dispatch(Action.DropAlerts) }

        // ---------------- watchlist
        if (nf == "all" || nf == "restocks") {
            val active = listOf(cu.osmanthusWatch, cu.peachWatch).count { it }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Eyebrow("Active Restock Watchlist") {
                    Pill("$active Active", c.ink, c.surface)
                    Spacer(Modifier.width(6.dp))
                    Pill("Node-Sync Live", c.green, c.greenTint, dot = true, pulse = true)
                }
                AnimatedVisibility(cu.osmanthusWatch, enter = expandVertically() + fadeIn(), exit = exit) {
                    WatchCard(
                        "osmanthus", "Osmanthus Plum Sparkling", "Node 01 — Building G Atrium",
                        RestockEta.forNode("N01", s.supplier)?.label ?: "Target Coil #04: Restocking Today 14:30", c.amber, c.amberTint,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ForestButton("Check Live Status", { dispatch(Action.LiveStatus) }, Modifier.weight(1f), small = true, style = BtnStyle.Dark, icon = Ic.Activity)
                            ForestButton("Unsubscribe", { dispatch(Action.Unsub("osmanthus")) }, small = true, style = BtnStyle.Outline)
                        }
                    }
                }
                AnimatedVisibility(cu.peachWatch, enter = expandVertically() + fadeIn(), exit = exit) {
                    WatchCard(
                        "white peach", "White Peach Sub-Chilled Restock", "Node 03 — Library 1F Hub",
                        "Coil #02 · 0 Sugar · Chilled", c.blue, c.blueTint,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Push App Alert", style = Type.body(12.sp, 600), color = c.ink)
                            ForestSwitch(cu.peachPush, { dispatch(Action.PushAlert) }, "Push App Alert")
                            Spacer(Modifier.weight(1f))
                            Text("SMS Drop", style = Type.body(12.sp, 600), color = c.ink)
                            ForestSwitch(cu.peachSms, { dispatch(Action.SmsAlert) }, "SMS Drop")
                        }
                        ForestButton("Unsubscribe", { dispatch(Action.Unsub("peach")) }, Modifier.fillMaxWidth(), small = true, style = BtnStyle.Ghost)
                    }
                }
                if (active == 0) {
                    EmptyState(Ic.BellOff, "No restock alerts yet", "Tap Notify Me on an out-of-stock coil to join the FIFO queue.", "Browse Inventory") { dispatch(Action.Tab(0)) }
                }
            }
        }

        // ---------------- live feed
        val showUrgent = (nf == "all" || nf == "escrow") && "urgent" !in cu.dismissed
        val showTimeout = vip && (nf == "all" || nf == "escrow") && "timeout" !in cu.dismissed
        val showPerks = vip && (nf == "all" || nf == "vip") && "perks" !in cu.dismissed
        if (nf != "restocks") {
            Eyebrow("Live Campus Activity") { Text("Past 2 hours", style = Type.body(11.sp, 600), color = c.faint) }
        }
        AnimatedVisibility(showUrgent, exit = exit) {
            FeedCard(
                unread = "urgent" !in cu.read, tag = "URGENT DROP ALERT", tagColor = c.red, tagBg = c.redTint, ago = "4m ago",
                icon = Ic.Bolt, highlight = true,
                title = "White Peach Zero Sugar Restocked!",
            ) {
                Text(
                    buildAnnotatedString {
                        append("Node 02 (AI Lab 2F) just filled with fresh batch. ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.red)) { append("9 cans remaining") }
                        append(" in rapid dispenser.")
                    },
                    style = Type.body(13.sp), color = c.muted,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (vip && Features.campusMap) {
                        ForestButton("Navigate & Hold", { dispatch(Action.HoldNav(System.currentTimeMillis())) }, Modifier.weight(1f), small = true, icon = Ic.Navigation)
                        ForestButton("Dismiss", { dispatch(Action.Dismiss("urgent")) }, small = true, style = BtnStyle.Outline)
                    } else {
                        ForestButton("Dismiss", { dispatch(Action.Dismiss("urgent")) }, Modifier.weight(1f), small = true, style = BtnStyle.Outline)
                    }
                }
            }
        }
        AnimatedVisibility(showTimeout, exit = exit) {
            FeedCard(
                unread = "timeout" !in cu.read, tag = "ESCROW TIMEOUT", tagColor = c.amber, tagBg = c.amberTint, ago = "28m ago",
                icon = Ic.Clock, title = "Coil #03 Escrow released",
            ) {
                Text("Coil #03 Escrow released (expired 5-min pickup window). Tap below to instantly re-order.", style = Type.body(13.sp), color = c.muted)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ForestButton("Re-order Now", { dispatch(Action.Reorder(System.currentTimeMillis())) }, Modifier.weight(1f), small = true, style = BtnStyle.Dark, icon = Ic.Refresh)
                    ForestButton("Dismiss", { dispatch(Action.Dismiss("timeout")) }, small = true, style = BtnStyle.Outline)
                }
            }
        }
        AnimatedVisibility(showPerks, exit = exit) {
            FeedCard(
                unread = "perks" !in cu.read, tag = "CAMPUS PASS PERKS", tagColor = c.blue, tagBg = c.blueTint, ago = "1h ago",
                icon = Ic.Star, title = "Tier 2 VIP discount",
            ) {
                Text("Tier 2 VIP: 10% discount auto-applied to your next Smart Chiller pickup today.", style = Type.body(13.sp), color = c.muted)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ForestButton("Open VIP Pass", { dispatch(Action.Tab(3)) }, Modifier.weight(1f), small = true, style = BtnStyle.Tonal, icon = Ic.IdCard)
                    ForestButton("Dismiss", { dispatch(Action.Dismiss("perks")) }, small = true, style = BtnStyle.Outline)
                }
            }
        }
        if (nf != "restocks" && !showUrgent && !showTimeout && !showPerks) {
            EmptyState(Ic.CheckCircle, "You're all caught up", "Tap Notify Me on a sold-out drink, or turn on Get drop alerts, and the updates land here.")
        }

        // ---------------- preferences
        if (nf == "all") {
            Eyebrow("Alert Preferences")
            ForestCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                ToggleRow("Restock Push Notifications", "Receive instant drops for watched coils", cu.prefPush, { dispatch(Action.Pref("push")) }, Ic.Bell)
                Hairline(Modifier.padding(start = 46.dp))
                ToggleRow("Sound & Haptic Alerts", "Soft chime on restock dispense", cu.prefSound, { dispatch(Action.Pref("sound")) }, Ic.Volume)
                Hairline(Modifier.padding(start = 46.dp))
                ToggleRow("Quiet Campus Hours", "Mute from 23:00 to 07:00 (Study mode)", cu.prefQuiet, { dispatch(Action.Pref("quiet")) }, Ic.Moon)
            }
        }
    }
}

@Composable
private fun WatchCard(
    product: String,
    title: String,
    location: String,
    status: String,
    statusColor: Color,
    statusBg: Color,
    actions: @Composable () -> Unit,
) {
    val c = FF.c
    ForestCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductArt(product, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = Type.title(15.sp, 800), color = c.ink)
                Text(location, style = Type.body(12.sp), color = c.faint)
            }
            Pill("Subscribed", c.green, c.greenTint, icon = Ic.Check)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(statusBg).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Ic.Clock, null, tint = statusColor, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text(status, style = Type.mono(12.sp, 600), color = statusColor)
        }
        Spacer(Modifier.height(10.dp))
        actions()
    }
}

@Composable
private fun FeedCard(
    unread: Boolean,
    tag: String,
    tagColor: Color,
    tagBg: Color,
    ago: String,
    icon: ImageVector,
    title: String,
    highlight: Boolean = false,
    body: @Composable () -> Unit,
) {
    val c = FF.c
    Column(
        Modifier.fillMaxWidth().softCard(RoundedCornerShape(22.dp))
            .then(if (highlight) Modifier.border(1.5.dp, c.redLine, RoundedCornerShape(22.dp)) else Modifier)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(tagBg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tagColor, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(tag, style = Type.eyebrow(10.sp), color = tagColor, modifier = Modifier.weight(1f))
            Text(ago, style = Type.body(11.sp), color = c.faint)
            if (unread) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(c.red2))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = Type.title(16.sp, 800), color = c.ink)
        Spacer(Modifier.height(4.dp))
        body()
    }
}
