package com.chiforest.forestflow.core

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class Role { Student, Supplier }

@Immutable
@Serializable
data class Session(
    val uid: String,
    val displayName: String,
    val role: Role,
    val method: String,
    val email: String? = null,
    val guest: Boolean = true,
)

@Immutable
@Serializable
data class Escrow(
    val nodeId: String,
    val coil: Int,
    val product: String,
    val createdAt: Long,
    /** Absolute claim deadline (epoch ms) so the clock survives process death. */
    val expiresAt: Long,
) {
    fun claimLeftMs(now: Long) = (expiresAt - now).coerceAtLeast(0)
    /** The dispenser hold runs 5:00 past the claim window. */
    fun holdLeftMs(now: Long) = claimLeftMs(now) + HOLD_EXTRA_MS

    companion object {
        const val CLAIM_MS = 298_000L
        const val HOLD_EXTRA_MS = 300_000L
        const val BACKUP_CODE = "CF-9204"
    }
}

@Immutable
@Serializable
data class Camera(val zoom: Float, val cx: Float, val cy: Float)

@Immutable
@Serializable
data class Layers(
    val nodes: Boolean = true,
    val entrances: Boolean = false,
    val poi: Boolean = false,
    val dimDark: Boolean = true,
)

@Immutable
@Serializable
data class CustomerState(
    val osmanthusWatch: Boolean = false,
    /** Products (Seed.products keys) the student wants a phone alert for when they come back in stock. */
    val watch: List<String> = emptyList(),
    /** One switch for new product drops (pilot flavours and limited cans). */
    val dropAlerts: Boolean = false,
    val peachWatch: Boolean = true,
    val peachPush: Boolean = true,
    val peachSms: Boolean = false,
    val escrow: Escrow? = null,
    val points: Int = 420,
    val sugarKg: Float = 1.4f,
    val redeemed: List<String> = emptyList(),
    val nf: String = "all",
    val dismissed: List<String> = emptyList(),
    val read: List<String> = emptyList(),
    val prefPush: Boolean = true,
    val prefSound: Boolean = true,
    val prefQuiet: Boolean = false,
)

@Serializable
enum class FeedType { Escrow, Restock, Fifo, Clearance, Stockout, Surge, Dispatch }

@Immutable
@Serializable
data class FeedEvent(
    val id: String,
    val type: FeedType,
    val title: String,
    val subtitle: String,
    val time: String,
    val body: String,
    val chips: List<String> = emptyList(),
)

@Immutable
@Serializable
data class Coil(val id: String, val name: String, val level: Int, /** Cans sold per hour at normal traffic. */ val cph: Int = 6) {
    val state: String get() = when {
        level <= 0 -> "empty"
        level <= 6 -> "low"
        else -> "optimal"
    }
}

@Immutable
@Serializable
data class SupplierState(
    val coils: List<Coil> = Seed.coils,
    val selected: String = "C3",
    val dispensed: Int = 318,
    val queue: Int = 18,
    val fillBase: Int = 42,
    val stage: Int = 2,
    val runnerPinged: Boolean = false,
    val urgentPinged: Boolean = false,
    val urgentResolved: Boolean = false,
    val clearance: String = "pending",
    /** Marked-down prices by product name; a product not listed sells at Clearance.LIST_PRICE. */
    val prices: Map<String, Double> = emptyMap(),
    /** Product whose clearance suggestion was dismissed (shown again for any other product). */
    val clearanceDismissed: String = "",
    val restockConfirmed: Boolean = false,
    val feed: List<FeedEvent> = Seed.feed,
    val alertFilter: String = "all",
    val muted: Boolean = false,
    val velocitySel: Int = 4,
    val surges: Int = 0,
    val eventSeq: Int = 0,
) {
    val coilSum: Int get() = coils.sumOf { it.level }
    /** Network fill: min(100, fillBase + round((sum - 132) / 240 * 100)). */
    val networkFill: Int get() =
        (fillBase + Math.round((coilSum - 132) / 240.0 * 100).toInt()).coerceIn(0, 100)
    val emptyCount: Int get() = coils.count { it.level <= 0 }
    val lowCount: Int get() = coils.count { it.level in 1..6 }
    fun coil(id: String) = coils.firstOrNull { it.id == id }
}

enum class SheetKind { Profile, Buildings, Layers, Redeem, Cas, Qr, Stations, Product, Guide }

@Immutable
sealed interface MapFocus {
    val nonce: Int
    data class Node(val id: String, override val nonce: Int) : MapFocus
    data class Building(val campusId: String, val code: String, override val nonce: Int) : MapFocus
    data class Locate(override val nonce: Int) : MapFocus
    data class Compass(override val nonce: Int) : MapFocus
}

@Immutable
@Serializable
data class AppState(
    val session: Session? = null,
    /** Account this data belongs to. Each account keeps its own copy (SQLite `user_state`). */
    val ownerUid: String? = null,
    val loginRole: Role = Role.Student,
    val agreed: Boolean = false,
    val phone: String = "",
    val smsCode: String = "",
    val codeSentAt: Long = 0,
    val customerTab: Int = 0,
    val supplierTab: Int = 0,
    val aBadge: Int = 2,
    val activeNodeId: String = "N01",
    val selectedNodeId: String = "N01",
    val campusId: String = "tc",
    val savedNodes: List<String> = emptyList(),
    val youAreHereNodeId: String = "N01",
    val cameras: Map<String, Camera> = emptyMap(),
    val layers: Layers = Layers(),
    val mapFilter: String = "all",
    val buildingFilter: String? = null,
    val directionsTo: String? = null,
    val customer: CustomerState = CustomerState(),
    val supplier: SupplierState = SupplierState(),
    // ---- UI-only, never persisted ----
    @Transient val sheet: SheetKind? = null,
    @Transient val search: String = "",
    @Transient val loggingIn: String? = null,
    @Transient val casError: String? = null,
    @Transient val casBusy: Boolean = false,
    @Transient val shakeField: String? = null,
    @Transient val shakeNonce: Int = 0,
    @Transient val mapFocus: MapFocus? = null,
    @Transient val focusNonce: Int = 0,
    @Transient val pulseBuilding: String? = null,
    /** Product whose details sheet is open (key from Seed.products). */
    @Transient val productKey: String? = null,
    /** Page of the sign-in guide that is showing. */
    @Transient val guideStep: Int = 0,
) {
    /** Unread = feed cards that are neither read nor dismissed. */
    val unread: Int get() = unread(vip = true)

    /** Unread count over the cards that are actually shown (the VIP cards are hidden when the VIP club is off). */
    fun unread(vip: Boolean): Int =
        (if (vip) Seed.notificationIds else Seed.notificationIds.filter { it == "urgent" })
            .count { it !in customer.read && it !in customer.dismissed }

    fun codeLeftSec(now: Long): Int =
        if (codeSentAt == 0L) 0 else (60 - ((now - codeSentAt) / 1000).toInt()).coerceAtLeast(0)
}

@Immutable
data class Drop(val id: String, val name: String, val nameZh: String, val pts: Int, val note: String)

object Seed {
    val notificationIds = listOf("urgent", "timeout", "perks")

    val coils = listOf(
        Coil("A1", "White Peach", 2, 12), Coil("A2", "White Peach", 9, 10), Coil("A3", "Kalamansi", 11, 6), Coil("A4", "Kalamansi", 8, 6),
        Coil("B1", "Lychee", 3, 9), Coil("B2", "Lychee", 2, 9), Coil("B3", "Grape", 12, 5), Coil("B4", "Grape", 10, 5),
        Coil("C1", "Oolong", 13, 4), Coil("C2", "Jasmine", 9, 4), Coil("C3", "Osmanthus Plum", 0, 8), Coil("C4", "Green Tea", 5, 7),
        Coil("D1", "Barley", 14, 3), Coil("D2", "Green Tea", 12, 3), Coil("D3", "Electro", 11, 3), Coil("D4", "Electro", 11, 3),
    )

    val feed = listOf(
        FeedEvent("f1", FeedType.Escrow, "VIP Escrow", "hold 10:00", "13:48 · 2m ago",
            "Student #8821 holds Coil A1 White Peach (Tier 2 VIP pass auto-assigned).", listOf("Engaged")),
        FeedEvent("f2", FeedType.Restock, "Restock Confirmed", "Zhang — stock 100%", "13:47 · 3m ago",
            "Node 01 inventory synced to campus network. Sensor calibration passed with 0 variance.",
            listOf("Verified by NFC Token", "Batch #CF-2024-T01")),
        FeedEvent("f3", FeedType.Fifo, "FIFO Notification Push", "18 students (queue)", "13:46 · 4m ago",
            "Push notifications dispatched to waiting student wishlist & cart holds. Peak hour crowd surge at 13:50 managed.",
            listOf("18 Sent · 100% Delivery", "WeChat & UniApp Gateway")),
        FeedEvent("f4", FeedType.Clearance, "Clearance −20% Approved", "Osmanthus Plum", "13:45 · 5m ago",
            "Price adjusted to ¥4.40 across digital shelf tags. Algorithmic freshness promotion applied.",
            listOf("E-Ink Shelf Updated", "Old Price ¥5.50")),
        FeedEvent("f5", FeedType.Stockout, "Coil C3 Empty", "Osmanthus Plum", "13:41 · 9m ago",
            "Dispensed can #318 triggered zero sensor reading. Runner Zhang assigned automatically.",
            listOf("Zero Units Left", "Chamber 3.8°C nominal")),
    )

    val drops = listOf(
        Drop("d1", "Yuzu Sparkling Pilot", "柚子气泡水 · 试饮", 150, "Pilot batch · Node 01 only"),
        Drop("d2", "Sakura Peach Limited", "樱花白桃 · 限定", 220, "Spring limited · 2 per pass"),
        Drop("d3", "Oolong Zero Trial", "乌龙零糖 · 体验装", 90, "Tasting can · any node"),
    )

    val velocity = listOf(
        "11:00" to 42, "12:00" to 64, "13:00" to 88, "13:30" to 104, "14:00" to 120, "15:00" to 76, "16:00" to 52,
    )

    /** Products shown as the four coil tiles on the student Inventory screen. */
    val products = listOf(
        Product(1, "White Peach", "White Peach", "白桃", listOf("480ml", "0 Sugar"),
            ProductInfo("Chi Forest", "Sparkling water with white peach. Light and fruity.", "Peach, soft sweetness, fine bubbles",
                0.0, 0, "None", "Sparkling water", "Label: 0 sugar · 0 calories · 0 fat")),
        Product(2, "Lychee", "Lychee Fizzy", "荔枝", listOf("330ml", "0 Sugar"),
            ProductInfo("Chi Forest", "Sparkling water with lychee. Floral and sweet with a crisp finish.", "Lychee, floral, bright fizz",
                0.0, 0, "None", "Sparkling water", "Sweetened with erythritol")),
        Product(3, "Calamansi", "Calamansi Lime", "青桔", listOf("330ml", "Vit C"),
            ProductInfo("Chi Forest", "Sparkling water with calamansi lime. Sour and refreshing.", "Calamansi, lime zest, tart",
                null, null, "None", "Sparkling water", null)),
        Product(4, "Osmanthus", "Osmanthus Plum", "桂花乌梅", listOf("330ml", "0 Sugar"),
            ProductInfo("Chi Forest", "Sparkling water with osmanthus and smoked plum. Tart and floral.", "Osmanthus, smoked plum, tart",
                null, null, "None", "Sparkling water", null)),
        Product(5, "Grape", "Grape Delight", "葡萄", listOf("330ml", "0 Sugar"),
            ProductInfo("Chi Forest", "Sparkling water with grape. Rich and sweet without the sugar.", "Dark grape, jammy, full fizz",
                0.0, 0, "None", "Sparkling water", "Label: zero sugar & cal · erythritol, sucralose")),
        Product(6, "Mango", "Sunny Mango", "芒果", listOf("330ml", "0 Sugar"),
            ProductInfo("Chi Forest", "Sparkling water with mango, naturally flavoured.", "Ripe mango, tropical, soft fizz",
                0.0, 0, "None", "Sparkling water", "Label: zero sugar & cal")),
        Product(7, "Electro", "Alienergy Grapefruit", "外星人电解质水 · 西柚", listOf("500ml", "0 Sugar"),
            ProductInfo("Alienergy", "Electrolyte water with grapefruit flavour, made for after sport.", "Grapefruit, light, slightly salty",
                0.0, 0, "None", "Electrolyte drink", "Label: electrolytes ≥ 200 mg · vitamins E and B6 · 0糖0卡")),
        Product(8, "Monster", "Monster Ultra", "魔爪 超越", listOf("355ml", "0 Sugar"),
            ProductInfo("Monster Energy", "Zero-sugar energy drink with a light citrus taste.", "Light citrus, crisp, lightly sweet",
                0.0, null, "≈32 mg / 100 ml", "Energy drink", "Not for children or pregnant women · high caffeine")),
        Product(9, "Red Bull", "Red Bull", "红牛维生素功能饮料", listOf("250ml", "Caffeine"),
            ProductInfo("Red Bull (China)", "Vitamin energy drink, the classic gold can.", "Sweet, tangy, syrupy",
                null, null, "50 mg per can", "Energy drink", "Label: taurine 125 mg · caffeine 50 mg · contains sugar")),
        Product(10, "Ice Tea", "Ice Red Tea", "康师傅冰红茶", listOf("500ml", "Lemon"),
            ProductInfo("Master Kong", "Sweetened black tea with lemon.", "Black tea, lemon, sweet",
                null, null, "Contains tea", "Tea drink", "Sweetened with white sugar and fructose syrup")),
        Product(11, "Green Tea", "Leaf Green Tea", "东方树叶 · 绿茶", listOf("500ml", "No Sugar"),
            ProductInfo("Nongfu Spring", "Unsweetened brewed green tea.", "Fresh leaf, light bitterness, clean",
                0.0, 0, "Contains tea", "Tea", "Brand claim: 0 sugar · 0 fat · 0 kcal")),
    )

    fun product(key: String?): Product? = products.firstOrNull { it.key == key }

    /** Port of the prototype's fresh(): the demo state, with an active escrow on Node 01 coil 3. */
    fun fresh(now: Long, session: Session? = null, escrow: Boolean = true): AppState = AppState(
        session = session,
        ownerUid = session?.uid,
        loginRole = session?.role ?: Role.Student,
        agreed = session != null,
        customer = CustomerState(
            // the seeded 10-minute hold belongs to the VIP club: no hold (and no expiry toast) when that is hidden
            escrow = if (escrow) Escrow("N01", 3, "Calamansi Lime", now, now + Escrow.CLAIM_MS) else null,
        ),
    )
}

@Immutable
data class Product(val coil: Int, val key: String, val name: String, val nameZh: String, val tags: List<String>, val info: ProductInfo)

/**
 * Details shown when a drink card is tapped. Numbers are per container and come from the label or the brand;
 * null means we could not confirm it, and the app says "see label" instead of guessing.
 */
@Immutable
data class ProductInfo(
    val brand: String,
    val description: String,
    val notes: String,
    val sugarG: Double?,
    val kcal: Int?,
    val caffeine: String,
    val type: String,
    val labelNote: String?,
)
