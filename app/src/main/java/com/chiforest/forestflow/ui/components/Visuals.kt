package com.chiforest.forestflow.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chiforest.forestflow.R
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Type
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Real product photos where bundled (res/drawable-nodpi/can_*.jpg). */
fun canPhoto(key: String): Int? = when (key.lowercase()) {
    "white peach", "peach" -> R.drawable.can_white_peach
    "lychee", "lychee fizzy" -> R.drawable.can_lychee
    "calamansi", "calamansi lime", "kalamansi" -> R.drawable.can_calamansi
    "osmanthus", "osmanthus plum" -> R.drawable.can_osmanthus
    else -> null
}

fun canColor(key: String): Color = when (key.lowercase()) {
    "white peach", "peach" -> Color(0xFFF4A3A8)
    "lychee", "lychee fizzy" -> Color(0xFFE85D75)
    "calamansi", "calamansi lime", "kalamansi" -> Color(0xFF7CC24A)
    "osmanthus", "osmanthus plum" -> Color(0xFFB0473F)
    "grape" -> Color(0xFF7B4FA0)
    "oolong" -> Color(0xFFB07A3C)
    "jasmine" -> Color(0xFFD9C36A)
    "green tea" -> Color(0xFF3E9B5C)
    "barley" -> Color(0xFFC8A060)
    "electro" -> Color(0xFF3D8BD9)
    else -> Color(0xFFB7131A)
}

/** Photo if we have one, otherwise the drawn can. Out-of-stock tiles are desaturated at 70% opacity. */
@Composable
fun ProductArt(key: String, modifier: Modifier = Modifier, outOfStock: Boolean = false) {
    val res = canPhoto(key)
    val filter = if (outOfStock) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
    Box(modifier.graphicsLayer { alpha = if (outOfStock) 0.7f else 1f }) {
        if (res != null) {
            Image(painterResource(res), null, contentScale = ContentScale.Crop, colorFilter = filter, modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(FF.c.surface2), contentAlignment = Alignment.Center) {
                CanArt(canColor(key), Modifier.fillMaxHeight(0.8f))
            }
        }
    }
}

/** Canvas port of the prototype's can() SVG. */
@Composable
fun CanArt(labelColor: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(width = 34.dp, height = 60.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val body = Brush.horizontalGradient(listOf(Color(0xFFCFD3D8), Color(0xFFFFFFFF), Color(0xFFE3E6EA), Color(0xFFB9BEC4)))
            drawRoundRect(Color(0xFF9AA0A6), Offset(w * 0.1f, 0f), Size(w * 0.8f, h * 0.07f), CornerRadius(w * 0.2f))
            drawRoundRect(body, Offset(0f, h * 0.05f), Size(w, h * 0.92f), CornerRadius(w * 0.18f))
            drawRect(labelColor, Offset(0f, h * 0.36f), Size(w, h * 0.36f))
            drawRect(Color.White.copy(alpha = 0.35f), Offset(w * 0.16f, h * 0.08f), Size(w * 0.08f, h * 0.86f))
        }
        Text("气", style = Type.title(13.sp, 800), color = Color(0xFF1A1A1A), modifier = Modifier.graphicsLayer { translationY = -16f })
    }
}

/** Real QR (ZXing, EC level H) drawn module-by-module, with the red 气 badge in the middle. */
@Composable
fun QrCode(payload: String, modifier: Modifier = Modifier, ink: Color = Color(0xFF111418)) {
    val matrix = remember(payload) {
        QRCodeWriter().encode(
            payload, BarcodeFormat.QR_CODE, 0, 0,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H, EncodeHintType.MARGIN to 0),
        )
    }
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .semantics { contentDescription = "QR code for $payload" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val n = matrix.width
            val cell = size.minDimension / n
            val r = CornerRadius(cell * 0.32f)
            val m = cell * 0.08f
            for (y in 0 until n) for (x in 0 until n) {
                if (matrix.get(x, y)) {
                    drawRoundRect(ink, Offset(x * cell + m, y * cell + m), Size(cell - 2 * m, cell - 2 * m), r)
                }
            }
        }
        Box(
            Modifier.fillMaxSize(0.22f).clip(RoundedCornerShape(12.dp)).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.fillMaxSize(0.82f).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFB7131A), Color(0xFFDB322F)))),
                contentAlignment = Alignment.Center,
            ) { Text("气", style = Type.title(18.sp, 800), color = Color.White) }
        }
    }
}


/** Bars grow from the baseline with a 60ms stagger; tap selects a bar. */
@Composable
fun BarChart(
    values: List<Pair<String, Int>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 130.dp,
    peakIndex: Int = values.indexOfFirst { it.second == values.maxOf { v -> v.second } },
    valueSuffix: String = "",
) {
    val c = FF.c
    val max = values.maxOf { it.second }.toFloat()
    val anims = remember(values.size) { values.map { Animatable(0f) } }
    val reduce = FF.reduceMotion
    LaunchedEffect(values) {
        anims.forEachIndexed { i, a ->
            if (reduce) a.snapTo(1f) else launch {
                delay(60L * i)
                a.animateTo(1f, tween(520, easing = com.chiforest.forestflow.ui.theme.Motion.EaseOutQuint))
            }
        }
    }
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, (label, v) ->
            val sel = i == selected
            val peak = i == peakIndex
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics { this.selected = sel; contentDescription = "$label: $v$valueSuffix" }
                    .pressable(scaleTo = 0.95f, role = Role.Button) { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (sel) Text("$v", style = Type.num(11.sp, 800), color = if (peak) c.red else c.ink)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .fillMaxHeight((v / max) * 0.78f * anims[i].value + 0.001f)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(
                            when {
                                peak && sel -> Brush.verticalGradient(listOf(c.red2, c.red))
                                peak -> Brush.verticalGradient(listOf(c.red2.copy(alpha = 0.75f), c.red.copy(alpha = 0.75f)))
                                sel -> Brush.verticalGradient(listOf(c.ink, c.ink))
                                else -> Brush.verticalGradient(listOf(c.bar, c.bar))
                            },
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(label, style = Type.body(10.sp, if (sel) 700 else 500), color = if (sel) c.ink else c.faint, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}
