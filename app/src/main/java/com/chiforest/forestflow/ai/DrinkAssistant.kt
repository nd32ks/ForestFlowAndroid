package com.chiforest.forestflow.ai

import androidx.compose.runtime.Immutable

@Immutable
data class DrinkOption(val id: Int, val key: String, val name: String, val traits: String, val stock: Int) {
    val inStock: Boolean get() = stock > 0
}

@Immutable
data class Recommendation(val option: DrinkOption?, val reason: String, val source: Source) {
    enum class Source { Ai, Quick }
}

/** What each product is like, in the words a student would use. Used by the offline matcher and sent to the model. */
object DrinkCatalog {
    private val traits = mapOf(
        "White Peach" to "sweet, fruity, light, 0 sugar",
        "Lychee" to "sweet, floral, sparkling, 0 sugar",
        "Calamansi" to "sour, citrus, refreshing, vitamin C",
        "Osmanthus" to "floral, tart plum, traditional, 0 sugar",
        "Grape" to "sweet, rich, dark grape, sparkling, 0 sugar",
        "Mango" to "sweet, tropical mango, sparkling, 0 sugar",
        "Electro" to "still, electrolytes, after sport, grapefruit, 0 sugar",
        "Monster" to "energy drink, high caffeine, citrus, 0 sugar",
        "Red Bull" to "energy drink, caffeine, sweet, contains sugar",
        "Ice Tea" to "black tea, lemon, sweet, contains sugar, caffeine",
        "Green Tea" to "tea, grassy, light, unsweetened, caffeine",
    )

    fun options(products: List<com.chiforest.forestflow.core.Product>, ready: Map<String, Int>): List<DrinkOption> =
        products.map { DrinkOption(it.coil, it.key, it.name, traits[it.key] ?: "", ready[it.key] ?: 0) }
}

/**
 * Offline matcher. Free, instant, and the fallback whenever the model is unreachable.
 * Also answers clear requests ("lychee", "something sour") so the model is only asked about vague ones.
 */
object QuickMatcher {
    private val words = mapOf(
        "White Peach" to listOf("peach", "sweet", "fruity", "light", "juice", "soft", "白桃", "甜"),
        "Lychee" to listOf("lychee", "litchi", "sweet", "floral", "fizz", "sparkling", "bubbly", "荔枝", "甜"),
        "Calamansi" to listOf("lime", "calamansi", "citrus", "sour", "tangy", "vitamin", "vit c", "refresh", "fresh", "cool", "tired", "study", "青桔", "酸"),
        "Osmanthus" to listOf("osmanthus", "plum", "floral", "tart", "traditional", "herbal", "smoky", "different", "桂花", "乌梅"),
        "Grape" to listOf("grape", "kyoho", "rich", "sweet", "berry", "葡萄"),
        "Mango" to listOf("mango", "tropical", "sweet", "fruity", "芒果"),
        "Green Tea" to listOf("green tea", "tea", "grassy", "light", "unsweetened", "绿茶", "茶"),
        "Electro" to listOf("electrolyte", "electro", "alienergy", "grapefruit", "sport", "gym", "workout", "run", "thirsty", "hydrate", "sweat", "电解质", "西柚"),
        "Monster" to listOf("monster", "energy", "caffeine", "awake", "tired", "exam", "all-nighter", "boost", "魔爪"),
        "Red Bull" to listOf("red bull", "redbull", "energy", "caffeine", "awake", "tired", "boost", "红牛"),
        "Ice Tea" to listOf("ice tea", "iced tea", "black tea", "lemon tea", "sweet tea", "冰红茶", "红茶"),
    )
    private val byName = setOf("peach", "lychee", "litchi", "lime", "calamansi", "osmanthus", "plum", "grape", "mango", "green tea", "electrolyte", "alienergy", "monster", "red bull", "ice tea", "白桃", "荔枝", "青桔", "桂花", "乌梅", "葡萄", "芒果", "绿茶", "电解质", "魔爪", "红牛", "冰红茶")
    private val noSugar = listOf("no sugar", "sugar free", "sugar-free", "zero sugar", "0 sugar", "diet", "healthy", "low sugar", "无糖", "零糖")

    class Result(val best: DrinkOption?, val reason: String, val confident: Boolean)

    fun pick(query: String, options: List<DrinkOption>): Result {
        val q = query.trim().lowercase()
        val scored = options.map { o ->
            var score = words[o.key].orEmpty().count { it in q }
            if (noSugar.any { it in q }) score += if ("0 sugar" in o.traits || "unsweetened" in o.traits) 1 else -3
            Triple(o, score, words[o.key].orEmpty().any { it in byName && it in q })
        }
        val available = scored.filter { it.first.inStock }
        if (available.isEmpty()) return Result(null, "Everything here is sold out. Turn on Notify Me and I will tell you.", true)
        val ranked = available.sortedWith(compareByDescending<Triple<DrinkOption, Int, Boolean>> { it.second }.thenByDescending { it.first.stock })
        val top = ranked.first()
        val unique = ranked.size == 1 || top.second > ranked[1].second
        val bestOverall = scored.maxByOrNull { it.second }
        val soldOutNote = bestOverall != null && !bestOverall.first.inStock && bestOverall.second > top.second
        val reason = when {
            soldOutNote -> "${bestOverall!!.first.name} is sold out here, so try this: ${top.first.traits}."
            top.second > 0 -> "Matches what you asked for: ${top.first.traits}."
            else -> "Our most popular pick right now: ${top.first.traits}."
        }
        val confident = unique && (top.second >= 2 || (top.second >= 1 && top.third))
        return Result(top.first, reason, confident)
    }
}

/** Sends a system and a user message to a chat model and returns its text, or null when it cannot. */
fun interface AiTransport {
    suspend fun complete(system: String, user: String): String?
}

/**
 * Picks a drink for a free-text request. Built to spend as few model calls as possible:
 * - a clear request is answered by [QuickMatcher] without calling the model;
 * - the model gets only the in-stock drinks, compact, with a short fixed instruction and a 60-token cap;
 * - repeat requests are served from a small cache (keyed on what is in stock, not on counts);
 * - a hard cap of [maxAiCalls] model calls per app run, and any failure falls back to [QuickMatcher].
 */
class DrinkAssistant(private val ai: AiTransport?, private val maxAiCalls: Int = 30) {
    private val cache = object : LinkedHashMap<String, Recommendation>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Recommendation>?) = size > 30
    }
    var aiCalls = 0
        private set

    val usesModel: Boolean get() = ai != null

    suspend fun recommend(query: String, options: List<DrinkOption>): Recommendation {
        val q = query.trim().lowercase().take(120)
        if (q.isBlank()) return Recommendation(null, "Tell me what you feel like, for example: something sweet.", Recommendation.Source.Quick)
        val key = q + "|" + options.joinToString(",") { "${it.id}${if (it.inStock) "y" else "n"}" }
        synchronized(cache) { cache[key] }?.let { return it }

        val quick = QuickMatcher.pick(q, options)
        val quickRec = Recommendation(quick.best, quick.reason, Recommendation.Source.Quick)
        val model = ai
        if (quick.confident || model == null || aiCalls >= maxAiCalls) {
            synchronized(cache) { cache[key] = quickRec }
            return quickRec
        }

        aiCalls++
        val raw = runCatching { model.complete(SYSTEM, userMessage(q, options)) }.getOrNull()
        val parsed = raw?.let { runCatching { parseReply(it, options) }.getOrNull() }
        return if (parsed != null) {
            synchronized(cache) { cache[key] = parsed }
            parsed
        } else {
            quickRec // not cached, so a later try can reach the model again
        }
    }

    companion object {
        const val SYSTEM = "Pick ONE drink for a student. Reply with JSON only: {\"pick\":<id>,\"why\":\"<12 words max>\"}. Choose only from the list."

        /** In-stock drinks only, one short line each, request last. */
        fun userMessage(query: String, options: List<DrinkOption>): String =
            options.filter { it.inStock }.joinToString("\n", postfix = "\nRequest: $query") { "${it.id}|${it.name}|${it.traits}" }

        private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }

        /** Accepts the reply only if it names an in-stock drink; anything else returns null (caller falls back). */
        fun parseReply(raw: String, options: List<DrinkOption>): Recommendation? {
            val body = Regex("\\{.*\\}", RegexOption.DOT_MATCHES_ALL).find(raw)?.value ?: return null
            val obj = runCatching { json.parseToJsonElement(body) as? kotlinx.serialization.json.JsonObject }.getOrNull() ?: return null
            val id = (obj["pick"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: return null
            val why = (obj["why"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.trim().orEmpty().take(140)
            val option = options.firstOrNull { it.id == id && it.inStock } ?: return null
            return Recommendation(option, why.ifBlank { option.traits }, Recommendation.Source.Ai)
        }
    }
}
