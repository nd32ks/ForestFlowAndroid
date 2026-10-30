package com.chiforest.forestflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import android.content.Context
import android.app.ActivityManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiforest.forestflow.core.ToastIcon
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Motion
import com.chiforest.forestflow.ui.theme.Type
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay

@Composable
fun glassStyle(): HazeStyle {
    val c = FF.c
    return remember(c) {
        HazeStyle(
            backgroundColor = c.bg,
            tint = HazeTint(c.glass),
            blurRadius = 18.dp,
            noiseFactor = 0.04f,
        )
    }
}

/**
 * Weak devices (flagged low-RAM, or a per-app heap of 128 MB or less) skip the live blur: it re-blurs everything
 * behind the header and nav bar every frame, which is the most expensive thing on screen. They get a plain
 * translucent panel instead.
 */
@Composable
fun rememberLowEndDevice(): Boolean {
    val ctx = LocalContext.current
    return remember {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.isLowRamDevice || am.memoryClass <= 128
    }
}

@Composable
fun Modifier.glass(hazeState: HazeState): Modifier {
    val c = FF.c
    val style = glassStyle()
    return if (rememberLowEndDevice()) background(c.bg.copy(alpha = 0.94f)) else hazeEffect(hazeState, style)
}

// ---------------------------------------------------------------- glass pill nav

data class NavItem(val icon: ImageVector, val label: String, val dot: Boolean = false, val count: Int = 0)

/**
 * The signature floating glass pill. Overlays content (screens scroll behind it), never part of the scroll.
 * The red-tinted indicator slides between items on an overshooting spring.
 */
@Composable
fun GlassNavBar(
    hazeState: HazeState,
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = FF.c
    val shape = RoundedCornerShape(34.dp)
    Box(
        modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .widthIn(max = 430.dp)
            .fillMaxWidth()
            .height(66.dp)
            .shadow(24.dp, shape, ambientColor = Color(0x33101828), spotColor = Color(0x40101828))
            .clip(shape)
            .glass(hazeState)
            .border(1.dp, c.glassLine, shape)
            .drawWithContent {
                drawContent()
                // inner top highlight: light catching the material's edge
                drawLine(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = if (c.isDark) 0.18f else 0.9f), Color.Transparent)),
                    Offset(size.width * 0.12f, 1.5f), Offset(size.width * 0.88f, 1.5f), strokeWidth = 1.5f,
                )
            }
            .padding(5.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().fillMaxHeight()) {
            val w = maxWidth / items.size
            val x by animateDpAsState(w * selected, Motion.indicator(), label = "navX")
            Box(
                Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(x.roundToPx(), 0) }
                    .width(w)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(29.dp))
                    .background(c.redTint)
                    .border(1.dp, c.redLine, RoundedCornerShape(29.dp)),
            )
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                items.forEachIndexed { i, item ->
                    val sel = i == selected
                    val lift by animateFloatAsState(if (sel) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 420f), label = "lift")
                    val tint by animateColorAsState(if (sel) c.red else c.faint, label = "tint")
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics {
                                this.selected = sel
                                contentDescription = item.label + if (item.count > 0) ", ${item.count} new" else if (item.dot) ", unread" else ""
                            }
                            .pressable(scaleTo = 0.9f, role = Role.Tab) { onSelect(i) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        Box {
                            Icon(
                                item.icon, null, tint = tint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        translationY = -lift * 1.dp.toPx()
                                        scaleX = 1f + 0.08f * lift
                                        scaleY = 1f + 0.08f * lift
                                    },
                            )
                            if (item.count > 0) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).offset(x = 9.dp, y = (-5).dp)
                                        .size(17.dp).clip(CircleShape).background(c.red2)
                                        .border(1.5.dp, c.surface, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) { Text(if (item.count > 9) "9+" else "${item.count}", style = Type.num(9.sp, 800), color = Color.White) }
                            } else if (item.dot) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-1).dp)
                                        .size(9.dp).clip(CircleShape).background(c.red2)
                                        .border(1.5.dp, c.surface, CircleShape),
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(item.label, style = Type.body(10.sp, if (sel) 700 else 600), color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- sticky glass header

@Composable
fun GlassHeader(
    hazeState: HazeState,
    admin: Boolean,
    subtitle: String,
    initials: String,
    onAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    temp: String? = null,
) {
    val c = FF.c
    Box(
        modifier
            .fillMaxWidth()
            .glass(hazeState)
            .drawWithContent {
                drawContent()
                drawLine(c.line.copy(alpha = 0.6f), Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f)
            }
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogoMark(36)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Chi Forest", style = Type.title(16.sp, 800), color = c.ink)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (admin) "ADMIN" else "CAMPUS",
                        style = Type.eyebrow(9.sp),
                        color = if (admin) Color.White else c.red,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (admin) (if (c.isDark) c.surface2 else c.navy) else c.redTint)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text(subtitle, style = Type.body(12.sp, 500), color = c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (temp != null) {
                Pill(temp, c.blue, c.blueTint, icon = Ic.Sun)
                Spacer(Modifier.width(4.dp))
            }
            Box(
                Modifier.size(48.dp).semantics { contentDescription = "Profile" }.pressable(scaleTo = 0.9f, onClick = onAvatar),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(c.navy2, c.navy)))
                        .border(2.dp, c.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(initials, style = Type.title(13.sp, 800), color = Color.White) }
            }
        }
    }
}

@Composable
fun LogoMark(sizeDp: Int) {
    Box(
        Modifier.size(sizeDp.dp)
            .shadow(6.dp, CircleShape, ambientColor = Color(0xFFB7131A), spotColor = Color(0x80B7131A))
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFB7131A), Color(0xFFDB322F)))),
        contentAlignment = Alignment.Center,
    ) { Text("气", style = Type.title((sizeDp * 0.48f).sp, 800), color = Color.White) }
}

// ---------------------------------------------------------------- toasts

data class ToastMsg(val id: Long, val text: String, val icon: ToastIcon)

@Stable
class ToastController {
    var current by mutableStateOf<ToastMsg?>(null)
        private set
    private var seq = 0L
    fun show(text: String, icon: ToastIcon = ToastIcon.None) {
        current = ToastMsg(++seq, text, icon)
    }
    fun hide(id: Long) {
        if (current?.id == id) current = null
    }
}

fun ToastIcon.vector(): ImageVector? = when (this) {
    ToastIcon.None -> null
    ToastIcon.Check -> Ic.CheckCircle
    ToastIcon.Lock -> Ic.Lock
    ToastIcon.Alert -> Ic.Alert
    ToastIcon.Bell -> Ic.Bell
    ToastIcon.Bolt -> Ic.Bolt
    ToastIcon.Copy -> Ic.Copy
    ToastIcon.Gift -> Ic.Gift
    ToastIcon.Navigation -> Ic.Navigation
    ToastIcon.Truck -> Ic.Truck
    ToastIcon.Pin -> Ic.MapPin
    ToastIcon.Info -> Ic.Phone
}

/** Dark translucent capsule that springs up from the bottom, auto-hides after 2.4s. */
@Composable
fun ToastHost(controller: ToastController, bottomOffset: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val msg = controller.current
    LaunchedEffect(msg?.id) {
        if (msg != null) {
            delay(2400)
            controller.hide(msg.id)
        }
    }
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = bottomOffset), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = msg != null,
            enter = slideInVertically(spring(dampingRatio = 0.72f, stiffness = 380f)) { it } + fadeIn() + scaleIn(initialScale = 0.92f),
            exit = slideOutVertically { it / 2 } + fadeOut() + scaleOut(targetScale = 0.95f),
        ) {
            val last = msg ?: return@AnimatedVisibility
            Row(
                Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 400.dp)
                    .shadow(18.dp, CircleShape, ambientColor = Color.Black, spotColor = Color(0x66000000))
                    .clip(CircleShape)
                    .background(Color(0xE6161A20))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                last.icon.vector()?.let {
                    Icon(it, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(last.text, style = Type.body(13.sp, 600).copy(letterSpacing = 0.01.sp), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---------------------------------------------------------------- sheets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForestSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = FF.c
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = c.surface,
        contentColor = c.ink,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 38.dp, height = 5.dp).clip(CircleShape).background(c.line))
        },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp), content = content)
    }
}

@Composable
fun SheetTitle(title: String, subtitle: String? = null, onClose: () -> Unit) {
    val c = FF.c
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.display(22.sp, 800), color = c.ink)
            if (subtitle != null) Text(subtitle, style = Type.body(13.sp), color = c.faint)
        }
        IconOrb(Ic.Close, "Close", onClose, size = 34.dp, bg = c.surface2, border = false)
    }
}
