package com.chiforest.forestflow.feature.customer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.Camera
import com.chiforest.forestflow.core.Campus
import com.chiforest.forestflow.core.MapData
import com.chiforest.forestflow.core.MapFocus
import com.chiforest.forestflow.core.MapNode
import com.chiforest.forestflow.core.Reducer
import com.chiforest.forestflow.ui.components.pressable
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Motion
import com.chiforest.forestflow.ui.theme.Type
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Pan / pinch / double-tap campus map. Bitmap drawn through the camera; pins, labels and the
 * directions line are drawn in screen space so they stay crisp at any zoom.
 */
@Composable
fun CampusMapView(
    campus: Campus,
    bitmap: ImageBitmap,
    s: AppState,
    map: MapData,
    dispatch: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = FF.c
    val reduce = FF.reduceMotion
    val scope = rememberCoroutineScope()
    val camera = remember(campus.id) { MapCamera(s.cameras[campus.id] ?: campus.initialCamera()) }

    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val geo = remember(campus.id, w, h) { MapGeo(w, h, bitmap.width.toFloat(), bitmap.height.toFloat(), campus.maxZoom) }
        // the first frame after (re)layout: make sure the restored camera is legal for this viewport
        LaunchedEffect(geo) { camera.set(geo.clamp(camera.snapshot())) }

        val animJob = remember(campus.id) { arrayOfNulls<Job>(1) }
        fun animateTo(target: Camera, ms: Int = 450, save: Boolean = true) {
            animJob[0]?.cancel()
            val from = camera.snapshot()
            val to = geo.clamp(target)
            animJob[0] = scope.launch {
                val t = Animatable(0f)
                t.animateTo(1f, tween(if (reduce) 1 else ms, easing = Motion.EaseOut)) {
                    val k = value
                    camera.set(Camera(from.zoom + (to.zoom - from.zoom) * k, from.cx + (to.cx - from.cx) * k, from.cy + (to.cy - from.cy) * k))
                }
                if (save) dispatch(Action.SaveCamera(campus.id, camera.snapshot()))
            }
        }

        // Focus requests (pin select, search, locate, compass) from anywhere in the app.
        LaunchedEffect(s.mapFocus?.nonce, campus.id, geo) {
            val f = s.mapFocus ?: return@LaunchedEffect
            // During a campus crossfade both maps are composed: only the current campus may consume.
            if (campus.id != s.campusId) return@LaunchedEffect
            when (f) {
                is MapFocus.Node -> map.node(f.id)?.takeIf { n -> campus.nodes.any { it.id == n.id } }?.let { n ->
                    animateTo(geo.centeredOn(n.anchor[0], n.anchor[1], maxOf(camera.zoom, 1.6f)))
                }
                is MapFocus.Building -> campus.building(f.code)?.takeIf { f.campusId == campus.id }?.let { b ->
                    animateTo(geo.centeredOn(b.anchor[0], b.anchor[1], maxOf(camera.zoom, 2f)))
                }
                is MapFocus.Locate -> map.node(s.youAreHereNodeId)?.takeIf { n -> campus.nodes.any { it.id == n.id } }?.let { n ->
                    animateTo(geo.centeredOn(n.anchor[0], n.anchor[1], maxOf(camera.zoom, 1.8f)))
                }
                is MapFocus.Compass -> animateTo(campus.initialCamera())
            }
            dispatch(Action.FocusConsumed)
        }

        val dim = c.isDark && s.layers.dimDark
        val dimFilter = remember(dim) {
            if (dim) ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.82f, 0.82f, 0.82f, 1f) }) else null
        }
        val t = rememberInfiniteTransition(label = "map")
        val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "pulse")
        val dash by t.animateFloat(0f, -60f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "dash")

        val here = map.node(s.youAreHereNodeId)?.takeIf { n -> campus.nodes.any { it.id == n.id } }
        val dirNode = s.directionsTo?.let { map.node(it) }?.takeIf { n -> campus.nodes.any { it.id == n.id } && here != null }
        val pulseBuilding = s.pulseBuilding?.let { campus.building(it) }

        // ---------------- bitmap + vector overlays
        // One container owns pan/zoom so drags starting on a pin still move the map.
        Box(
            Modifier
                .fillMaxSize()
                .testTag("campusMap")
                .pointerInput(geo) {
                    val tracker = VelocityTracker()
                    var lastTapAt = 0L
                    var lastTapPos = Offset.Zero
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val onPin = down.isConsumed // a pin's clickable already took this press
                        var pointers = 1
                        animJob[0]?.cancel()
                        tracker.resetTracking()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        var raw = camera.snapshot()
                        var moved = false
                        var acc = Offset.Zero
                        var lastTime = down.uptimeMillis
                        do {
                            // Initial pass: the map claims the drag before the page's verticalScroll sees it.
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            pointers = maxOf(pointers, ev.changes.size)
                            if (!moved && ev.changes.any { it.isConsumed }) break
                            val zoomCh = ev.calculateZoom()
                            val pan = ev.calculatePan()
                            val centroid = ev.calculateCentroid(useCurrent = false)
                            if (!moved) {
                                acc += pan
                                if (acc.getDistance() > viewConfiguration.touchSlop * 0.5f || ev.changes.size > 1 || abs(zoomCh - 1f) > 0.02f) moved = true
                            }
                            if (moved && centroid != Offset.Unspecified) {
                                val anchor = geo.toImage(centroid, raw)
                                val z2 = (raw.zoom * zoomCh).coerceIn(0.6f, campus.maxZoom * 1.6f)
                                val s2 = geo.scale(z2)
                                raw = Camera(
                                    z2,
                                    anchor.x - (centroid.x - w / 2f) / (s2 * geo.iw) - pan.x / (s2 * geo.iw),
                                    anchor.y - (centroid.y - h / 2f) / (s2 * geo.ih) - pan.y / (s2 * geo.ih),
                                )
                                camera.set(geo.soft(raw))
                                ev.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                            ev.changes.firstOrNull()?.let {
                                tracker.addPosition(it.uptimeMillis, it.position)
                                lastTime = it.uptimeMillis
                            }
                        } while (ev.changes.any { it.pressed })
                        if (!moved && pointers == 1 && !onPin && lastTime - down.uptimeMillis < 300) {
                            // double-tap: fit ↔ 2.5× at the tap point
                            val gap = down.uptimeMillis - lastTapAt
                            if (gap < 300 && (down.position - lastTapPos).getDistance() < 48.dp.toPx()) {
                                lastTapAt = 0L
                                val cur = camera.snapshot()
                                if (cur.zoom > 2f) {
                                    animateTo(Camera(1f, cur.cx, cur.cy))
                                } else {
                                    val a = geo.toImage(down.position, cur)
                                    animateTo(Camera(2.5f, a.x, a.y))
                                }
                            } else {
                                lastTapAt = down.uptimeMillis
                                lastTapPos = down.position
                            }
                        }
                        if (moved) {
                            // hand the release velocity to the settle: project where the flick was going
                            val v = tracker.calculateVelocity()
                            val cur = camera.snapshot()
                            val sc = geo.scale(cur.zoom)
                            val flick = lastTime > 0 && (abs(v.x) > 300f || abs(v.y) > 300f)
                            val target = if (flick) Camera(
                                cur.zoom,
                                cur.cx - MapGeo.project(v.x) / (sc * geo.iw),
                                cur.cy - MapGeo.project(v.y) / (sc * geo.ih),
                            ) else cur
                            animateTo(target, if (flick) 650 else 380)
                        }
                    }
                },
        ) {
        Canvas(Modifier.fillMaxSize()) {
            val cam = Camera(camera.zoom, camera.cx, camera.cy)
            val sc = geo.scale(cam.zoom)
            val dw = geo.iw * sc
            val dh = geo.ih * sc
            val left = w / 2f - cam.cx * dw
            val top = h / 2f - cam.cy * dh
            drawImage(
                bitmap,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bitmap.width, bitmap.height),
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
                filterQuality = FilterQuality.Medium,
                colorFilter = dimFilter,
            )

            // entrances: red dots (like the dark Building Entrances reference)
            if (s.layers.entrances) for (e in campus.entrances) {
                val p = geo.toScreen(e.anchor[0], e.anchor[1], cam)
                drawCircle(Color.White, 7.dp.toPx(), p)
                drawCircle(Color(0xFFE5484D), 5.dp.toPx(), p)
            }

            // pulsing ring on a searched building
            if (pulseBuilding != null) {
                val p = geo.toScreen(pulseBuilding.anchor[0], pulseBuilding.anchor[1], cam)
                val k = if (reduce) 0.5f else pulse
                drawCircle(Color(0xFFDB322F).copy(alpha = (1f - k) * 0.9f), 12.dp.toPx() + k * 26.dp.toPx(), p, style = Stroke(3.dp.toPx()))
                drawCircle(Color(0xFFDB322F), 6.dp.toPx(), p)
                drawCircle(Color.White, 2.5.dp.toPx(), p)
            }

            // directions: animated dashed line + arrowhead (straight line; the maps have no routing graph)
            if (here != null && dirNode != null) {
                val a = geo.toScreen(here.anchor[0], here.anchor[1], cam)
                val b = geo.toScreen(dirNode.anchor[0], dirNode.anchor[1], cam)
                drawLine(Color.White, a, b, 7.dp.toPx(), StrokeCap.Round)
                drawLine(
                    Color(0xFF0B63E5), a, b, 4.dp.toPx(), StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), if (reduce) 0f else dash),
                )
                val ang = Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat()
                rotate(ang, b) {
                    val l = 12.dp.toPx()
                    val tip = Offset(b.x - 10.dp.toPx(), b.y)
                    val path = Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(tip.x - l, tip.y - l * 0.6f)
                        lineTo(tip.x - l, tip.y + l * 0.6f)
                        close()
                    }
                    drawPath(path, Color(0xFF0B63E5))
                }
            }

            // You Are Here: red pin with pulsing halo
            if (here != null) {
                val p = geo.toScreen(here.anchor[0], here.anchor[1], cam)
                val k = if (reduce) 0.4f else pulse
                drawCircle(Color(0xFFDB322F).copy(alpha = 0.28f * (1f - k)), 10.dp.toPx() + 22.dp.toPx() * k, p)
                drawCircle(Color.White, 9.dp.toPx(), p)
                drawCircle(Color(0xFFB7131A), 6.5.dp.toPx(), p)
                drawCircle(Color.White, 2.5.dp.toPx(), p)
            }
        }

        // ---------------- screen-space composables (pins, labels, chips)
        val expanded by remember { derivedStateOf { camera.zoom >= 2.2f } }
        val nodes = if (s.layers.nodes) campus.nodes else emptyList()

        if (s.layers.entrances && campus.entrances.isNotEmpty()) {
            AnchoredLayer(campus.entrances.map { it.anchor }, geo, camera, AnchorMode.Below) {
                campus.entrances.forEach { e ->
                    Text(
                        "${e.name} ${e.nameZh}", style = Type.body(10.sp, 700), color = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xE6101828)).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        if (s.layers.poi && campus.pointsOfInterest.isNotEmpty()) {
            AnchoredLayer(campus.pointsOfInterest.map { it.anchor }, geo, camera, AnchorMode.Center) {
                campus.pointsOfInterest.forEach { p ->
                    Text(
                        "${p.name} · ${p.nameZh}", style = Type.body(10.sp, 700), color = Color(0xFF0B63E5),
                        modifier = Modifier.shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White)
                            .border(1.dp, Color(0x330B63E5), CircleShape).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
        if (nodes.isNotEmpty()) {
            // selected pin last so it draws on top
            val ordered = nodes.sortedBy { it.id == s.selectedNodeId }
            AnchoredLayer(ordered.map { it.anchor }, geo, camera, AnchorMode.Tip) {
                ordered.forEach { n ->
                    val match = Reducer.matches(n, s.mapFilter, s.buildingFilter, s.search, map)
                    NodePin(n, n.id == s.selectedNodeId, expanded, match, n.id in s.savedNodes) {
                        dispatch(Action.Pin(n.id))
                    }
                }
            }
        }
        if (here != null && dirNode != null) {
            val mid = listOf((here.anchor[0] + dirNode.anchor[0]) / 2f, (here.anchor[1] + dirNode.anchor[1]) / 2f)
            AnchoredLayer(listOf(mid), geo, camera, AnchorMode.Center) {
                Text(
                    "${dirNode.walkMin} min walk", style = Type.body(11.sp, 800), color = Color.White,
                    modifier = Modifier.shadow(6.dp, CircleShape).clip(CircleShape).background(Color(0xFF0B63E5)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        }  // gesture container
    }
}

enum class AnchorMode { Tip, Center, Below }

/** Places each child at its image anchor, re-laid-out (not recomposed) as the camera moves. */
@Composable
private fun AnchoredLayer(
    anchors: List<List<Float>>,
    geo: MapGeo,
    camera: MapCamera,
    mode: AnchorMode,
    content: @Composable () -> Unit,
) {
    val gap = with(LocalDensity.current) { 10.dp.roundToPx() }
    Layout(content, Modifier.fillMaxSize()) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            val cam = Camera(camera.zoom, camera.cx, camera.cy)
            placeables.forEachIndexed { i, p ->
                val a = anchors.getOrNull(i) ?: return@forEachIndexed
                val pt = geo.toScreen(a[0], a[1], cam)
                val x = pt.x.roundToInt() - p.width / 2
                val y = when (mode) {
                    AnchorMode.Tip -> pt.y.roundToInt() - p.height
                    AnchorMode.Center -> pt.y.roundToInt() - p.height / 2
                    AnchorMode.Below -> pt.y.roundToInt() + gap
                }
                p.place(x, y)
            }
        }
    }
}

@Composable
private fun NodePin(n: MapNode, selected: Boolean, expanded: Boolean, match: Boolean, saved: Boolean, onClick: () -> Unit) {
    val c = FF.c
    val pop by animateFloatAsState(if (selected) 1f else 0.9f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "pop")
    val alpha by animateFloatAsState(if (match) 1f else 0.2f, label = "match")
    val status = if (n.isLow) c.amber else Color(0xFF12B76A)
    val bg = if (selected) Color(0xFF101828) else Color.White
    val fg = if (selected) Color.White else Color(0xFF101828)
    Column(
        Modifier
            .graphicsLayer {
                scaleX = pop
                scaleY = pop
                this.alpha = alpha
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            }
            .semantics { contentDescription = "${n.shortName}, ${n.label}, ${n.pct}% stock" }
            .pressable(scaleTo = 0.9f, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .shadow(if (selected) 10.dp else 4.dp, CircleShape, ambientColor = Color.Black, spotColor = Color(0x55000000))
                .clip(CircleShape)
                .background(bg)
                .padding(start = 6.dp, end = if (selected || expanded) 10.dp else 8.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(18.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFB7131A), Color(0xFFDB322F)))),
                contentAlignment = Alignment.Center,
            ) { Text("气", style = Type.title(10.sp, 800), color = Color.White) }
            Spacer(Modifier.width(5.dp))
            Box(Modifier.size(7.dp).clip(CircleShape).background(status))
            Spacer(Modifier.width(5.dp))
            Text(
                if (selected || expanded) "${n.shortName} · ${n.pct}% · ${n.walkMin}m" else n.num,
                style = Type.num(11.sp, 800), color = fg, maxLines = 1,
            )
            if (saved) {
                Spacer(Modifier.width(4.dp))
                Text("★", style = Type.body(10.sp, 700), color = if (selected) Color(0xFFFFB454) else c.amber)
            }
        }
        Canvas(Modifier.size(width = 12.dp, height = 7.dp)) {
            val p = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(p, bg)
        }
    }
}
