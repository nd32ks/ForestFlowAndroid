package com.chiforest.forestflow.feature.customer

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.chiforest.forestflow.core.Camera
import com.chiforest.forestflow.core.Campus
import kotlin.math.abs
import kotlin.math.max

/** Camera in normalized image coordinates: (cx, cy) is the image point at the viewport centre; zoom 1 = cover-fit. */
@Stable
class MapCamera(c: Camera) {
    var zoom by mutableFloatStateOf(c.zoom)
    var cx by mutableFloatStateOf(c.cx)
    var cy by mutableFloatStateOf(c.cy)
    fun snapshot() = Camera(zoom, cx, cy)
    fun set(c: Camera) {
        zoom = c.zoom
        cx = c.cx
        cy = c.cy
    }
}

/** Pure viewport geometry. Shared by drawing, hit-testing and the unit tests. */
class MapGeo(val w: Float, val h: Float, val iw: Float, val ih: Float, val maxZoom: Float) {
    /** Cover-fit: at zoom 1 the image fills the card, so it can never show empty bands. */
    val base: Float = max(w / iw, h / ih)

    fun scale(zoom: Float) = base * zoom

    fun toScreen(nx: Float, ny: Float, c: Camera): Offset {
        val s = scale(c.zoom)
        return Offset((nx - c.cx) * iw * s + w / 2f, (ny - c.cy) * ih * s + h / 2f)
    }

    fun toImage(p: Offset, c: Camera): Offset {
        val s = scale(c.zoom)
        return Offset(c.cx + (p.x - w / 2f) / (iw * s), c.cy + (p.y - h / 2f) / (ih * s))
    }

    private fun bounds(zoom: Float): FloatArray {
        val s = scale(zoom)
        val hx = w / (2f * s * iw)
        val hy = h / (2f * s * ih)
        return floatArrayOf(hx, 1f - hx, hy, 1f - hy)
    }

    /** Hard clamp: the image never leaves the viewport. */
    fun clamp(c: Camera): Camera {
        val z = c.zoom.coerceIn(1f, maxZoom)
        val b = bounds(z)
        val cx = if (b[0] >= b[1]) 0.5f else c.cx.coerceIn(b[0], b[1])
        val cy = if (b[2] >= b[3]) 0.5f else c.cy.coerceIn(b[2], b[3])
        return Camera(z, cx, cy)
    }

    /** Soft clamp used while the finger is down: progressive resistance past the edges (rubber-band). */
    fun soft(raw: Camera): Camera {
        val z = when {
            raw.zoom < 1f -> 1f - rubber(1f - raw.zoom, 1f, 0.35f)
            raw.zoom > maxZoom -> maxZoom + rubber(raw.zoom - maxZoom, maxZoom, 0.35f)
            else -> raw.zoom
        }
        val s = scale(z)
        val b = bounds(z)
        fun axis(v: Float, lo: Float, hi: Float, px: Float, dim: Float): Float {
            if (lo >= hi) return 0.5f
            return when {
                v < lo -> lo - rubber((lo - v) * px, dim) / px
                v > hi -> hi + rubber((v - hi) * px, dim) / px
                else -> v
            }
        }
        return Camera(z, axis(raw.cx, b[0], b[1], s * iw, w), axis(raw.cy, b[2], b[3], s * ih, h))
    }

    /** Camera that puts (nx, ny) at the viewport centre at [zoom], clamped. */
    fun centeredOn(nx: Float, ny: Float, zoom: Float) = clamp(Camera(zoom, nx, ny))

    companion object {
        /** Apple's rubber-band: the further past the bound, the less the content follows. */
        fun rubber(overshoot: Float, dim: Float, c: Float = 0.55f): Float =
            (overshoot * dim * c) / (dim + c * abs(overshoot))

        /** Momentum projection (WWDC "Designing Fluid Interfaces"), px/s → px of travel. */
        fun project(velocity: Float, decelerationRate: Float = 0.995f): Float =
            (velocity / 1000f) * decelerationRate / (1f - decelerationRate)
    }
}

fun Campus.initialCamera() = Camera(initialZoom, initialCenter[0], initialCenter[1])

/** Decoded campus maps, kept for the process lifetime (two images). */
object MapBitmaps {
    private val cache = HashMap<String, ImageBitmap>()

    @Synchronized
    fun get(context: Context, path: String): ImageBitmap = cache.getOrPut(path) {
        context.assets.open(path).use { BitmapFactory.decodeStream(it) }.asImageBitmap()
    }
}
