package com.chiforest.forestflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Motion
import com.chiforest.forestflow.ui.theme.Type
import kotlin.math.roundToInt

// ---------------------------------------------------------------- modifiers

/**
 * Press feedback on pointer-down (instant scale), springy release. No ripple: the scale is the feedback.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    scaleTo: Float = 0.96f,
    role: Role? = Role.Button,
    label: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleTo else 1f,
        animationSpec = if (pressed) spring(dampingRatio = 1f, stiffness = 2200f) else spring(dampingRatio = 0.45f, stiffness = 520f),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interaction, indication = null, enabled = enabled, onClickLabel = label, role = role, onClick = onClick)
}

/** Horizontal ±6dp shake over 400ms whenever [nonce] changes while [active]. */
fun Modifier.shake(active: Boolean, nonce: Int): Modifier = composed {
    val x = remember { Animatable(0f) }
    val px = with(LocalDensity.current) { 6.dp.toPx() }
    LaunchedEffect(nonce) {
        if (active && nonce > 0) {
            for (v in listOf(-1f, 1f, -0.8f, 0.8f, -0.4f, 0.4f, 0f)) x.animateTo(v * px, tween(57, easing = LinearEasing))
        }
    }
    this.offset { IntOffset(x.value.roundToInt(), 0) }
}

/** Soft two-layer shadow in light theme; hairline in dark (shadows vanish on dark surfaces). */
fun Modifier.softCard(shape: Shape, elevated: Boolean = true): Modifier = composed {
    val c = FF.c
    if (c.isDark) this.clip(shape).background(c.surface).border(1.dp, c.line, shape)
    else this
        .then(
            if (elevated) Modifier
                .shadow(14.dp, shape, ambientColor = Color(0x14101828), spotColor = Color(0x1A101828))
            else Modifier,
        )
        .clip(shape)
        .background(c.surface)
        .border(1.dp, c.line.copy(alpha = 0.7f), shape)
}

// ---------------------------------------------------------------- containers

@Composable
fun ForestCard(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.softCard(RoundedCornerShape(radius)).padding(padding), content = content)
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = FF.c.faint, trailing: @Composable (RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = Type.eyebrow(), color = color, modifier = Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

@Composable
fun PageTitle(eyebrow: String, title: String, modifier: Modifier = Modifier, trailing: @Composable (RowScope.() -> Unit)? = null) {
    Column(modifier) {
        Text(eyebrow.uppercase(), style = Type.eyebrow(), color = FF.c.red)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = Type.display(30.sp), color = FF.c.ink, modifier = Modifier.weight(1f))
            trailing?.invoke(this)
        }
    }
}

// ---------------------------------------------------------------- buttons

enum class BtnStyle { Primary, Dark, Tonal, Outline, Ghost, Green, WeChat }

@Composable
fun ForestButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BtnStyle = BtnStyle.Primary,
    icon: ImageVector? = null,
    small: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val c = FF.c
    val shape = RoundedCornerShape(if (small) 12.dp else 14.dp)
    val (bg, fg) = when (style) {
        BtnStyle.Primary -> Color.Transparent to Color.White
        BtnStyle.Dark -> (if (c.isDark) c.surface2 else c.navy) to Color.White
        BtnStyle.Tonal -> c.redTint to c.red
        BtnStyle.Outline -> c.surface to c.ink
        BtnStyle.Ghost -> Color.Transparent to c.muted
        BtnStyle.Green -> c.green to Color.White
        BtnStyle.WeChat -> c.wechat to Color.White
    }
    val brush = if (style == BtnStyle.Primary) Brush.linearGradient(listOf(c.redBtn, c.red2)) else null
    Box(
        modifier
            .heightIn(min = if (small) 36.dp else 46.dp)
            .then(if (style == BtnStyle.Primary && enabled && !c.isDark) Modifier.shadow(10.dp, shape, ambientColor = c.red, spotColor = c.red.copy(alpha = 0.45f)) else Modifier)
            .clip(shape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(bg))
            .then(if (style == BtnStyle.Outline) Modifier.border(1.dp, c.line, shape) else Modifier)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .pressable(enabled = enabled && !loading, scaleTo = 0.96f, onClick = onClick)
            .padding(horizontal = if (small) 12.dp else 16.dp, vertical = if (small) 8.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = fg, strokeWidth = 2.2.dp, modifier = Modifier.size(20.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(icon, null, tint = fg, modifier = Modifier.size(if (small) 16.dp else 18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text, style = Type.title(if (small) 13.sp else 15.sp, 700), color = fg,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun IconOrb(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = FF.c.ink,
    bg: Color = FF.c.surface,
    border: Boolean = true,
) {
    val c = FF.c
    Box(
        modifier
            .size(maxOf(size, 48.dp))
            .semantics { contentDescription = description }
            .pressable(scaleTo = 0.9f, label = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .then(if (!c.isDark && border) Modifier.shadow(6.dp, CircleShape, ambientColor = Color(0x22101828), spotColor = Color(0x22101828)) else Modifier)
                .clip(CircleShape)
                .background(bg)
                .then(if (border) Modifier.border(1.dp, c.line, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.48f)) }
    }
}

// ---------------------------------------------------------------- pills & chips

@Composable
fun Pill(
    text: String,
    fg: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    dot: Boolean = false,
    pulse: Boolean = false,
    mono: Boolean = false,
) {
    Row(
        modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            LiveDot(fg, pulse = pulse, size = 6.dp)
            Spacer(Modifier.width(6.dp))
        }
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = if (mono) Type.mono(11.sp) else Type.body(11.sp, 700), color = fg, maxLines = 1)
    }
}

@Composable
fun LiveDot(color: Color, pulse: Boolean = true, size: Dp = 8.dp) {
    val reduce = FF.reduceMotion
    val t = rememberInfiniteTransition(label = "dot")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "p")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (pulse && !reduce) {
            Box(
                Modifier
                    .size(size)
                    .graphicsLayer { scaleX = 1f + p * 1.6f; scaleY = 1f + p * 1.6f; alpha = (1f - p) * 0.55f }
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Box(Modifier.size(size).clip(CircleShape).background(color))
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: ImageVector? = null,
) {
    val c = FF.c
    val bg by animateColorAsState(if (selected) (if (c.isDark) c.ink else c.navy) else c.surface, label = "chipBg")
    val fg by animateColorAsState(if (selected) (if (c.isDark) c.bg else Color.White) else c.ink, label = "chipFg")
    Row(
        modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else c.line, CircleShape)
            .semantics { this.selected = selected }
            .pressable(scaleTo = 0.94f, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = Type.body(13.sp, 600), color = fg, maxLines = 1)
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            Icon(trailing, null, tint = fg, modifier = Modifier.size(14.dp))
        }
    }
}

// ---------------------------------------------------------------- segmented control

@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
) {
    val c = FF.c
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(if (c.isDark) c.surface2 else Color(0xFFE6E8EB))
            .padding(3.dp),
    ) {
        val segW = maxWidth / options.size
        val x by animateDpAsState(segW * selected, Motion.indicator(), label = "thumb")
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(segW)
                .fillMaxHeight()
                .shadow(if (c.isDark) 0.dp else 4.dp, RoundedCornerShape(11.dp), ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
                .clip(RoundedCornerShape(11.dp))
                .background(if (c.isDark) c.bar else Color.White),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val sel = i == selected
                val fg by animateColorAsState(if (sel) c.ink else c.faint, label = "segFg")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { this.selected = sel }
                        .pressable(scaleTo = 0.97f, role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = Type.title(13.sp, if (sel) 700 else 600), color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- switch

@Composable
fun ForestSwitch(checked: Boolean, onChange: () -> Unit, description: String, modifier: Modifier = Modifier) {
    val c = FF.c
    val track by animateColorAsState(if (checked) c.switchOn else (if (c.isDark) c.bar else Color(0xFFE2E4E7)), label = "track")
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = 0.7f, stiffness = 520f), label = "knob")
    Box(
        modifier
            .size(width = 56.dp, height = 48.dp)
            .semantics {
                contentDescription = description
                stateDescription = if (checked) "On" else "Off"
            }
            .pressable(scaleTo = 0.94f, role = Role.Switch, onClick = onChange),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(width = 51.dp, height = 31.dp).clip(CircleShape).background(track)) {
            Box(
                Modifier
                    .offset { IntOffset(x.roundToPx(), 2.dp.roundToPx()) }
                    .size(27.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: () -> Unit, icon: ImageVector? = null) {
    val c = FF.c
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.surface2), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = c.ink, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.title(15.sp, 600), color = c.ink)
            Text(subtitle, style = Type.body(12.sp), color = c.faint)
        }
        ForestSwitch(checked, onChange, title)
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(FF.c.line))
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null, subColor: Color = FF.c.faint, icon: ImageVector? = null, accent: Color = FF.c.ink) {
    val c = FF.c
    Column(modifier.softCard(RoundedCornerShape(20.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = c.faint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(label.uppercase(), style = Type.eyebrow(10.sp), color = c.faint, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = Type.num(22.sp, 800), color = accent, maxLines = 1)
        if (sub != null) Text(sub, style = Type.body(11.sp, 600), color = subColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, action: String? = null, onAction: () -> Unit = {}) {
    val c = FF.c
    ForestCard(Modifier.fillMaxWidth(), padding = PaddingValues(24.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(c.redTint), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = c.red, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = Type.title(17.sp), color = c.ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(body, style = Type.body(13.sp), color = c.faint, textAlign = TextAlign.Center)
            if (action != null) {
                Spacer(Modifier.height(16.dp))
                ForestButton(action, onAction, style = BtnStyle.Primary, modifier = Modifier.widthIn(min = 180.dp))
            }
        }
    }
}

val Shape22 = RoundedCornerShape(22.dp)
val BorderLine: BorderStroke @Composable get() = BorderStroke(1.dp, FF.c.line)
