package com.chiforest.forestflow

import com.chiforest.forestflow.core.MapData
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.hypot

/** Guards the hand-authored map data: every node sits on its own building, every building is on the image. */
class MapDataTest {
    private val map = MapData.parse(File("src/main/assets/maps/map_data.json").readText())

    @Test fun every_node_is_close_to_its_building() {
        for (c in map.campuses) for (n in c.nodes) {
            val b = c.building(n.building)
            assertNotNull("${n.id} building ${n.building} missing", b)
            val d = hypot(n.anchor[0] - b!!.anchor[0], n.anchor[1] - b.anchor[1])
            assertTrue("${n.id} is $d away from Bldg ${b.code}", d < 0.035f)
        }
    }

    @Test fun all_anchors_inside_image() {
        for (c in map.campuses) {
            val pts = c.buildings.map { it.anchor } + c.nodes.map { it.anchor } + c.entrances.map { it.anchor } + c.pointsOfInterest.map { it.anchor }
            assertTrue(pts.all { it[0] in 0.02f..0.98f && it[1] in 0.02f..0.98f })
        }
    }

    @Test fun node_ids_unique_and_ready_lists_present() {
        val ids = map.allNodes.map { it.id }
        assertTrue(ids.size == ids.toSet().size)
        assertTrue(map.allNodes.all { it.ready.containsKey("White Peach") && it.ready.containsKey("Calamansi") })
    }
}

class ManifestLabelTest {
    @Test fun label_follows_fill_level() {
        org.junit.Assert.assertEquals("Critical", com.chiforest.forestflow.feature.supplier.fillLabel(13))
        org.junit.Assert.assertEquals("Low", com.chiforest.forestflow.feature.supplier.fillLabel(33))
        org.junit.Assert.assertEquals("Steady", com.chiforest.forestflow.feature.supplier.fillLabel(73))
        org.junit.Assert.assertEquals("Full", com.chiforest.forestflow.feature.supplier.fillLabel(100))
    }
}
