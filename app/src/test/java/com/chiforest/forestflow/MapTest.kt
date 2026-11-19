package com.chiforest.forestflow

import com.chiforest.forestflow.core.Camera
import com.chiforest.forestflow.core.MapData
import com.chiforest.forestflow.core.MapSearch
import com.chiforest.forestflow.feature.customer.MapGeo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MapTest {
    private val map = MapData.parse(File("src/main/assets/maps/map_data.json").readText())
    private val geo = MapGeo(w = 1000f, h = 1000f, iw = 1696f, ih = 1280f, maxZoom = 4f)

    @Test fun data_has_both_campuses_and_11_nodes() {
        assertEquals(listOf("tc", "sip"), map.campuses.map { it.id })
        assertEquals(11, map.allNodes.size)
        assertTrue(map.allNodes.all { it.anchor.size == 2 && it.anchor.all { a -> a in 0f..1f } })
    }

    @Test fun screen_image_roundtrip() {
        val cam = Camera(2f, 0.6f, 0.5f)
        val p = geo.toScreen(0.62f, 0.65f, cam)
        val back = geo.toImage(p, cam)
        assertEquals(0.62f, back.x, 1e-4f)
        assertEquals(0.65f, back.y, 1e-4f)
        // the camera centre maps to the viewport centre
        assertEquals(500f, geo.toScreen(0.6f, 0.5f, cam).x, 1e-3f)
    }

    @Test fun clamp_keeps_image_covering_viewport() {
        val c = geo.clamp(Camera(0.5f, 0f, 2f))
        assertEquals(1f, c.zoom)
        val topLeft = geo.toScreen(0f, 0f, c)
        val bottomRight = geo.toScreen(1f, 1f, c)
        assertTrue(topLeft.x <= 0.01f && topLeft.y <= 0.01f)
        assertTrue(bottomRight.x >= 999.9f && bottomRight.y >= 999.9f)
        assertEquals(4f, geo.clamp(Camera(9f, 0.5f, 0.5f)).zoom)
    }

    @Test fun rubber_band_resists_progressively() {
        val a = MapGeo.rubber(100f, 1000f)
        val b = MapGeo.rubber(400f, 1000f)
        assertTrue(a < 100f && b < 400f && b > a)
        assertTrue(b / 400f < a / 100f)
        val soft = geo.soft(Camera(1f, -0.5f, 0.5f))
        val hard = geo.clamp(Camera(1f, -0.5f, 0.5f))
        assertTrue(soft.cx < hard.cx && soft.cx > -0.5f)
    }

    @Test fun momentum_projection_matches_apple_formula() {
        assertEquals(1000f / 1000f * 0.995f / 0.005f, MapGeo.project(1000f), 1e-3f)
    }

    @Test fun drink_search_lists_machines_with_stock() {
        val hit = MapSearch.search(map, "lychee").drinks.single()
        assertEquals("Lychee", hit.product.key)
        assertTrue(hit.stock.isNotEmpty())
        assertTrue(hit.stock.all { (n, count) -> count > 0 && (n.ready["Lychee"] ?: 0) == count })
        assertEquals(hit.stock.map { it.first.walkMin }.sorted(), hit.stock.map { it.first.walkMin })
        // machines with none left are not listed
        val calamansi = MapSearch.search(map, "calamansi").drinks.single()
        assertTrue(calamansi.stock.none { it.first.id == "N06" || it.first.id == "N11" })
        // Chinese name works, one Latin letter does not flood the list
        assertTrue(MapSearch.search(map, "荔枝").drinks.any { it.product.key == "Lychee" })
        assertTrue(MapSearch.search(map, "a").drinks.isEmpty())
        assertTrue(MapSearch.search(map, "library").drinks.isEmpty())
        // the map pins follow a drink search: only machines that have it
        val tc = map.campuses.first { it.id == "tc" }
        val pinned = tc.nodes.filter { com.chiforest.forestflow.core.Reducer.matches(it, "all", null, "calamansi", map) }.map { it.id }
        assertTrue("N01" in pinned)
        assertTrue("N06" !in pinned)
    }

    @Test fun search_matches_en_zh_codes_and_departments() {
        assertTrue(MapSearch.search(map, "library").nodes.any { it.id == "N03" })
        assertTrue(MapSearch.search(map, "图书馆").buildings.any { it.second.code == "D" })
        assertTrue(MapSearch.search(map, "AIAC").buildings.any { it.second.code == "D" })
        assertTrue(MapSearch.search(map, "CB").nodes.any { it.id == "N07" })
        assertTrue(MapSearch.search(map, "Design").buildings.any { it.second.code == "DB" })
        assertTrue(MapSearch.search(map, "node 05").nodes.any { it.id == "N05" })
        assertTrue(MapSearch.search(map, "machine 05").nodes.any { it.id == "N05" })
        assertTrue(MapSearch.search(map, "").nodes.isEmpty())
    }
}
