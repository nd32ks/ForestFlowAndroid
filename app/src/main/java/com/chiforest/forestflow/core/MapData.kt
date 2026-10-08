package com.chiforest.forestflow.core

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MapData(val version: Int = 1, val note: String = "", val campuses: List<Campus>) {
    val allNodes: List<MapNode> by lazy { campuses.flatMap { it.nodes } }
    fun node(id: String): MapNode? = allNodes.firstOrNull { it.id == id }
    fun campus(id: String): Campus = campuses.firstOrNull { it.id == id } ?: campuses.first()
    fun campusOfNode(id: String): Campus? = campuses.firstOrNull { c -> c.nodes.any { it.id == id } }

    companion object {
        val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): MapData = json.decodeFromString(serializer(), text)
        fun load(context: Context): MapData =
            parse(context.assets.open("maps/map_data.json").bufferedReader().use { it.readText() })
    }
}

@Serializable
data class Campus(
    val id: String,
    val name: String,
    val nameZh: String,
    val subtitle: String,
    val image: String,
    val northBearingDeg: Float = 0f,
    val initialCenter: List<Float>,
    val initialZoom: Float = 1f,
    val maxZoom: Float = 4f,
    val buildings: List<Building> = emptyList(),
    val entrances: List<Landmark> = emptyList(),
    val pointsOfInterest: List<Landmark> = emptyList(),
    val nodes: List<MapNode> = emptyList(),
) {
    fun building(code: String): Building? = buildings.firstOrNull { it.code.equals(code, ignoreCase = true) }
}

@Serializable
data class Building(
    val code: String,
    val name: String,
    val nameZh: String,
    val departments: List<String> = emptyList(),
    val anchor: List<Float>,
)

@Serializable
data class Landmark(val id: String, val name: String, val nameZh: String, val anchor: List<Float>)

@Serializable
data class MapNode(
    val id: String,
    val num: String,
    val name: String,
    val label: String,
    val building: String,
    val location: String,
    val status: String,
    val pct: Int,
    val tempC: Float,
    val walkMin: Int,
    val distanceM: Int,
    val anchor: List<Float>,
    val ready: Map<String, Int> = emptyMap(),
    val demo: Boolean = true,
) {
    val isLow: Boolean get() = status == "low"
    val shortName: String get() = "Node $num"
}

/** Pure search over a campus set: nodes first, then buildings. Matches EN + ZH names, codes and departments. */
object MapSearch {
    data class Results(val nodes: List<MapNode>, val buildings: List<Pair<Campus, Building>>)

    fun search(data: MapData, query: String): Results {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return Results(emptyList(), emptyList())
        val nodes = mutableListOf<MapNode>()
        val buildings = mutableListOf<Pair<Campus, Building>>()
        for (campus in data.campuses) {
            for (b in campus.buildings) {
                val hay = (listOf(b.code, b.name, b.nameZh, "bldg ${b.code}", "building ${b.code}") + b.departments)
                    .joinToString("|").lowercase()
                if (hay.contains(q) || b.code.lowercase() == q) buildings += campus to b
            }
            for (n in campus.nodes) {
                val b = campus.building(n.building)
                val hay = (listOf(n.id, n.name, n.shortName, n.label, n.location, n.building) +
                    listOfNotNull(b?.name, b?.nameZh) + (b?.departments ?: emptyList()))
                    .joinToString("|").lowercase()
                if (hay.contains(q)) nodes += n
            }
        }
        return Results(nodes, buildings)
    }
}
