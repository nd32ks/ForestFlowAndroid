package com.chiforest.forestflow.feature.customer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.MapData
import com.chiforest.forestflow.core.MapNode
import com.chiforest.forestflow.core.MapSearch
import com.chiforest.forestflow.core.Reducer
import com.chiforest.forestflow.core.SheetKind
import com.chiforest.forestflow.feature.ScreenColumn
import com.chiforest.forestflow.ui.components.BtnStyle
import com.chiforest.forestflow.ui.components.Chip
import com.chiforest.forestflow.ui.components.ForestButton
import com.chiforest.forestflow.ui.components.ForestSheet
import com.chiforest.forestflow.ui.components.Hairline
import com.chiforest.forestflow.ui.components.Ic
import com.chiforest.forestflow.ui.components.IconOrb
import com.chiforest.forestflow.ui.components.LiveDot
import com.chiforest.forestflow.ui.components.Pill
import com.chiforest.forestflow.ui.components.SegmentedControl
import com.chiforest.forestflow.ui.components.SheetTitle
import com.chiforest.forestflow.ui.components.ToggleRow
import com.chiforest.forestflow.ui.components.pressable
import com.chiforest.forestflow.ui.components.softCard
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MapScreen(s: AppState, map: MapData, dispatch: (Action) -> Unit) {
    val c = FF.c
    val campus = map.campus(s.campusId)
    val focus = LocalFocusManager.current
    val results = MapSearch.search(map, s.search)
    val visible = campus.nodes.filter { Reducer.matches(it, s.mapFilter, s.buildingFilter, s.search, map) }

    ScreenColumn {
        Box {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 2. search + locate
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(1f).height(50.dp).softCard(CircleShape).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Ic.Search, null, tint = c.faint, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value = s.search,
                            onValueChange = { dispatch(Action.Search(it)) },
                            singleLine = true,
                            textStyle = Type.body(15.sp, 500).copy(color = c.ink),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(c.red),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                            modifier = Modifier.weight(1f).semantics { contentDescription = "Search nodes or buildings" },
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (s.search.isEmpty()) Text("Search nodes or buildings…", style = Type.body(15.sp), color = c.faint, maxLines = 1)
                                    inner()
                                }
                            },
                        )
                        if (s.search.isNotEmpty()) {
                            Box(
                                Modifier.size(32.dp).clip(CircleShape).background(c.surface2)
                                    .pressable(scaleTo = 0.9f, label = "Clear search") { dispatch(Action.Search("")); focus.clearFocus() },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Ic.Close, null, tint = c.muted, modifier = Modifier.size(15.dp)) }
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    IconOrb(Ic.Locate, "Locate me", { focus.clearFocus(); dispatch(Action.Locate) }, size = 48.dp, tint = c.red)
                }

                // 3. campus switcher
                SegmentedControl(
                    listOf("Taicang 太仓", "SIP 园区"),
                    if (s.campusId == "tc") 0 else 1,
                    { dispatch(Action.Campus(if (it == 0) "tc" else "sip")) },
                )

                // 4. filter chips
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("All Stations (${campus.nodes.size})", s.mapFilter == "all", { dispatch(Action.MapFilter("all")) }, icon = Ic.Vending)
                    Chip("Chilled <4°C", s.mapFilter == "chilled", { dispatch(Action.MapFilter("chilled")) }, icon = Ic.Snow)
                    Chip("High Stock", s.mapFilter == "high", { dispatch(Action.MapFilter("high")) }, icon = Ic.TrendUp)
                    Chip(
                        s.buildingFilter?.let { "Bldg $it" } ?: "Bldg …", s.buildingFilter != null,
                        { dispatch(Action.OpenSheet(SheetKind.Buildings)) }, icon = Ic.Building, trailing = Ic.ChevronDown,
                    )
                }

                // 5. map card
                Box(
                    Modifier.fillMaxWidth().height(360.dp)
                        .shadow(if (c.isDark) 0.dp else 16.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x22101828), spotColor = Color(0x30101828))
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (c.isDark) c.surface2 else Color(0xFFF6F3E6))
                        .border(1.dp, c.line, RoundedCornerShape(26.dp)),
                ) {
                    Crossfade(targetState = campus.id, animationSpec = tween(250), label = "campus") { id ->
                        val cp = map.campus(id)
                        val ctx = LocalContext.current
                        val bmp by produceState<ImageBitmap?>(null, cp.image) {
                            value = withContext(Dispatchers.IO) { MapBitmaps.get(ctx, cp.image) }
                        }
                        val b = bmp
                        if (b == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = c.red, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                            }
                        } else {
                            CampusMapView(cp, b, s, map, dispatch, Modifier.fillMaxSize())
                        }
                    }
                    CompassRose(campus.northBearingDeg, Modifier.align(Alignment.TopStart).padding(12.dp))
                    Column(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        IconOrb(Ic.Compass, "Reset map view", { dispatch(Action.Compass) }, size = 40.dp)
                        IconOrb(Ic.Layers, "Map layers", { dispatch(Action.OpenSheet(SheetKind.Layers)) }, size = 40.dp)
                    }
                    Row(
                        Modifier.align(Alignment.BottomStart).padding(12.dp).clip(CircleShape)
                            .background(Color(0xCC101828)).padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LiveDot(Color(0xFF3DDC97), size = 6.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("${campus.name} Mesh Active", style = Type.body(10.sp, 700), color = Color.White)
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible.isEmpty(), Modifier.align(Alignment.BottomCenter).padding(bottom = 44.dp),
                        enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut(),
                    ) {
                        Pill("No stations match", Color.White, Color(0xE6101828), icon = Ic.Search)
                    }
                }

                // 6. node card
                val selected = map.node(s.selectedNodeId)?.takeIf { n -> campus.nodes.any { it.id == n.id } } ?: campus.nodes.first()
                AnimatedContent(
                    targetState = selected,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                    label = "nodeCard",
                ) { n -> NodeCard(n, s, map, dispatch) }
            }

            // search dropdown floats over the content
            androidx.compose.animation.AnimatedVisibility(
                s.search.isNotBlank(),
                modifier = Modifier.padding(top = 56.dp).zIndex(5f),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 340.dp).softCard(RoundedCornerShape(22.dp))
                        .verticalScroll(rememberScrollState()).padding(vertical = 6.dp),
                ) {
                    if (results.nodes.isEmpty() && results.buildings.isEmpty()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("No results for “${s.search}”", style = Type.body(14.sp), color = c.faint)
                            Text("Try a building letter like G, a node number like 02, or a place like “Library”.", style = Type.body(12.sp), color = c.faint)
                        }
                    }
                    results.nodes.forEach { n ->
                        val cp = map.campusOfNode(n.id)
                        ResultRow(Ic.Vending, c.red, n.shortName, "${n.label} · ${cp?.name} ${cp?.nameZh}") {
                            focus.clearFocus()
                            dispatch(Action.Search(""))
                            dispatch(Action.Pin(n.id))
                        }
                    }
                    results.buildings.forEach { (cp, b) ->
                        val nodeHere = cp.nodes.firstOrNull { it.building.equals(b.code, true) }
                        ResultRow(Ic.Building, c.blue, "Bldg ${b.code} · ${b.nameZh}", "${b.name} · ${cp.name}${if (nodeHere == null) " · no node" else ""}") {
                            focus.clearFocus()
                            if (nodeHere != null) {
                                dispatch(Action.Search(""))
                                dispatch(Action.Pin(nodeHere.id))
                            } else dispatch(Action.SearchBuilding(cp.id, b.code))
                        }
                    }
                    Hairline(Modifier.padding(vertical = 4.dp))
                    ResultRow(Ic.Activity, c.muted, "All stations list", "Accessible list of every vending node") {
                        focus.clearFocus()
                        dispatch(Action.OpenSheet(SheetKind.Stations))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, title: String, sub: String, onClick: () -> Unit) {
    val c = FF.c
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).pressable(scaleTo = 0.98f, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.title(14.sp, 700), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = Type.body(12.sp), color = c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Ic.ChevronRight, null, tint = c.faint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CompassRose(bearing: Float, modifier: Modifier) {
    Box(
        modifier.size(38.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(Color.White.copy(alpha = 0.92f))
            .semantics { contentDescription = "North is ${bearing.toInt()} degrees" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(26.dp).rotate(bearing)) {
            val cx = size.width / 2f
            val n = Path().apply { moveTo(cx, 0f); lineTo(cx + 5f * density / 2.5f + 2f, size.height / 2f); lineTo(cx - 5f * density / 2.5f - 2f, size.height / 2f); close() }
            val so = Path().apply { moveTo(cx, size.height); lineTo(cx + 5f * density / 2.5f + 2f, size.height / 2f); lineTo(cx - 5f * density / 2.5f - 2f, size.height / 2f); close() }
            drawPath(n, Color(0xFFDB322F))
            drawPath(so, Color(0xFF98A2B3))
            drawCircle(Color.White, 2.5f, Offset(cx, size.height / 2f))
        }
    }
}

@Composable
private fun NodeCard(n: MapNode, s: AppState, map: MapData, dispatch: (Action) -> Unit) {
    val c = FF.c
    val saved = n.id in s.savedNodes
    val directing = s.directionsTo == n.id
    Column(Modifier.fillMaxWidth().softCard(RoundedCornerShape(26.dp)).padding(18.dp)) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 4.dp).clip(CircleShape).background(c.line))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(n.name, style = Type.title(18.sp, 800), color = c.ink)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (n.isLow) Pill("Low Stock", c.amber, c.amberTint, dot = true) else Pill("Online", c.green, c.greenTint, dot = true, pulse = true)
                    if (n.demo) {
                        Spacer(Modifier.width(6.dp))
                        Pill("Demo location", c.faint, c.surface2)
                    }
                }
            }
            IconOrb(
                if (saved) Ic.BookmarkFilled else Ic.Bookmark, if (saved) "Remove bookmark" else "Bookmark node",
                { dispatch(Action.Save(n.id)) }, size = 40.dp, tint = if (saved) c.red else c.ink,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("${n.location} · ${n.distanceM}m away · Est. walk ${n.walkMin} min", style = Type.body(13.sp), color = c.faint)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("Chiller Core", "${n.tempC}°C", if (n.tempC < 4f) "Ideal" else "Warm", if (n.tempC < 4f) c.blue else c.amber, Ic.Thermo, Modifier.weight(1f))
            MetricTile("Total Stock", "${n.pct}%", if (n.pct >= 75) "High" else if (n.pct >= 40) "Steady" else "Low", if (n.pct >= 40) c.green else c.amber, Ic.Box, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Text("READY IN DISPENSARY", style = Type.eyebrow(10.sp), color = c.faint)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            n.ready.forEach { (k, v) ->
                if (v > 0) Pill("$k · $v", c.ink, c.surface2, dot = true) else Pill("$k · 0", c.faint, c.surface2)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ForestButton(
                if (directing) "Clear Route" else "Directions", { dispatch(Action.Directions) }, Modifier.weight(1f),
                style = if (directing) BtnStyle.Dark else BtnStyle.Primary, icon = Ic.Navigation,
            )
            ForestButton("View Stock", { dispatch(Action.ViewStock) }, Modifier.weight(1f), style = BtnStyle.Outline, icon = Ic.Box)
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, sub: String, accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    val c = FF.c
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(c.surface2).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = Type.body(12.sp, 600), color = c.faint)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = Type.num(22.sp, 800), color = c.ink)
            Spacer(Modifier.width(6.dp))
            Text(sub, style = Type.body(12.sp, 700), color = accent, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

// ---------------------------------------------------------------- map sheets

@Composable
fun BuildingsSheet(s: AppState, map: MapData, dispatch: (Action) -> Unit) {
    val c = FF.c
    val campus = map.campus(s.campusId)
    val withNodes = campus.buildings.filter { b -> campus.nodes.any { it.building.equals(b.code, true) } }
    ForestSheet(onDismiss = { dispatch(Action.CloseSheet) }) {
        SheetTitle("Buildings", "${campus.name} ${campus.nameZh} · buildings with vending nodes") { dispatch(Action.CloseSheet) }
        SheetRow("All buildings", "Show every station", s.buildingFilter == null) { dispatch(Action.BuildingFilter(null)) }
        withNodes.forEach { b ->
            val count = campus.nodes.count { it.building.equals(b.code, true) }
            SheetRow("Bldg ${b.code} · ${b.nameZh}", "${b.name} · $count node${if (count > 1) "s" else ""}", s.buildingFilter == b.code) {
                dispatch(Action.BuildingFilter(b.code))
            }
        }
    }
}

@Composable
private fun SheetRow(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    val c = FF.c
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) c.redTint else Color.Transparent)
            .pressable(scaleTo = 0.98f, onClick = onClick).heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.title(15.sp, 700), color = if (selected) c.red else c.ink)
            Text(sub, style = Type.body(12.sp), color = c.faint)
        }
        if (selected) Icon(Ic.Check, null, tint = c.red, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun LayersSheet(s: AppState, map: MapData, dispatch: (Action) -> Unit) {
    val campus = map.campus(s.campusId)
    ForestSheet(onDismiss = { dispatch(Action.CloseSheet) }) {
        SheetTitle("Map Layers", "${campus.name} ${campus.nameZh}") { dispatch(Action.CloseSheet) }
        ToggleRow("Vending nodes", "Chi Forest smart chillers", s.layers.nodes, { dispatch(Action.LayerToggle("nodes")) }, Ic.Vending)
        Hairline(Modifier.padding(start = 46.dp))
        ToggleRow(
            "Entrances", if (campus.entrances.isEmpty()) "Taicang campus only" else "Gates with English + 中文 labels",
            s.layers.entrances, { dispatch(Action.LayerToggle("entrances")) }, Ic.MapPin,
        )
        Hairline(Modifier.padding(start = 46.dp))
        ToggleRow(
            "Points of interest", if (campus.pointsOfInterest.isEmpty()) "Taicang campus only" else "Court · Sports Ground · Mini Hub",
            s.layers.poi, { dispatch(Action.LayerToggle("poi")) }, Ic.Star,
        )
        Hairline(Modifier.padding(start = 46.dp))
        ToggleRow("Dim map in dark mode", "Softens the bitmap to 82% brightness", s.layers.dimDark, { dispatch(Action.LayerToggle("dim")) }, Ic.Moon)
    }
}

@Composable
fun StationsSheet(s: AppState, map: MapData, dispatch: (Action) -> Unit) {
    ForestSheet(onDismiss = { dispatch(Action.CloseSheet) }) {
        SheetTitle("All Stations", "Every vending node on both campuses") { dispatch(Action.CloseSheet) }
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
            map.campuses.forEach { cp ->
                Text("${cp.name} ${cp.nameZh}".uppercase(), style = Type.eyebrow(10.sp), color = FF.c.faint, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                cp.nodes.forEach { n ->
                    SheetRow("${n.shortName} · ${n.label}", "${n.pct}% stock · ${n.tempC}°C · ${n.walkMin} min walk", n.id == s.selectedNodeId) {
                        dispatch(Action.CloseSheet)
                        dispatch(Action.Pin(n.id))
                    }
                }
            }
        }
    }
}

