package com.chiforest.forestflow.core

/** Result of a reduction: the next state plus one-off side effects for the ViewModel to run. */
data class Next(val state: AppState, val effects: List<Effect> = emptyList())

/**
 * Pure state machine. No Android, no clocks: time arrives inside actions.
 * Mirrors the prototype's act() switch one case per action.
 */
class Reducer(private val map: MapData, private val seedEscrow: Boolean = true) {

    fun reduce(s: AppState, a: Action): Next = when (a) {
        // ---------------- Login ----------------
        is Action.SetRole -> Next(s.copy(loginRole = a.role), listOf(Effect.Haptic()))
        Action.Agree -> Next(s.copy(agreed = !s.agreed))
        is Action.Phone -> Next(s.copy(phone = a.value.filter(Char::isDigit).take(11)))
        is Action.Code -> Next(s.copy(smsCode = a.value.filter(Char::isDigit).take(6)))
        is Action.GetCode -> when {
            s.codeLeftSec(a.now) > 0 -> Next(s)
            !validPhone(s.phone) -> shake(s, "phone", "Enter a valid 11-digit mobile number")
            else -> Next(
                s.copy(codeSentAt = a.now),
                listOf(Effect.Toast("验证码已发送 · Code sent to ${maskPhone(s.phone)}", ToastIcon.Check), Effect.ScheduleAutofill),
            )
        }
        Action.CodeAutofill -> Next(s.copy(smsCode = DEMO_CODE))
        is Action.Login -> login(s, a.method)
        Action.Visitor -> if (!s.agreed) agreeFirst(s) else
            Next(s.copy(loggingIn = "visitor"), listOf(Effect.StartLogin("visitor", s.loginRole)))
        is Action.LoginSucceeded -> {
            val role = a.session.role
            // Every account sees only its own data: its saved copy, the data already on screen if it is
            // the same account signing back in, or fresh demo data for a first sign-in.
            val base = when {
                a.restored != null -> a.restored
                s.ownerUid == a.session.uid -> s
                else -> Seed.fresh(a.now, escrow = seedEscrow)
            }
            Next(
                base.copy(
                    session = a.session, ownerUid = a.session.uid, loginRole = role, agreed = true,
                    loggingIn = null, casBusy = false, casError = null,
                    sheet = SheetKind.Guide, guideStep = 0, customerTab = 0, supplierTab = 0, phone = s.phone, smsCode = "", codeSentAt = 0,
                    shakeField = null, shakeNonce = s.shakeNonce, mapFocus = null, focusNonce = s.focusNonce,
                ),
                listOf(
                    Effect.Toast(
                        if (role == Role.Student) "Welcome, ${a.session.displayName} · 欢迎回来" else "Admin console ready · ${a.session.displayName}",
                        ToastIcon.Check,
                    ),
                    Effect.Haptic(),
                    Effect.Log("login", "${a.session.method} · ${role.name}"),
                ),
            )
        }
        Action.CasStarted -> Next(s.copy(casBusy = true, casError = null))
        is Action.CasFailed -> Next(s.copy(casBusy = false, casError = a.message, loggingIn = null, shakeField = "cas", shakeNonce = s.shakeNonce + 1))

        // ---------------- Shell ----------------
        is Action.Tab -> tab(s, a.index)
        Action.Profile -> Next(s.copy(sheet = SheetKind.Profile))
        is Action.OpenSheet -> Next(s.copy(sheet = a.kind))
        Action.CloseSheet -> Next(s.copy(sheet = null, casError = null))
        Action.Logout -> Next(
            s.copy(session = null, sheet = null, loggingIn = null, agreed = false, phone = "", smsCode = "", codeSentAt = 0),
            listOf(Effect.SignOut, Effect.Toast("Signed out", ToastIcon.Check), Effect.Log("logout", s.session?.displayName ?: "")),
        )
        is Action.Reset -> Next(
            Seed.fresh(a.now, s.session, seedEscrow).copy(campusId = s.campusId),
            listOf(Effect.Toast("Demo data reset", ToastIcon.Check), Effect.Haptic(), Effect.Log("reset", "fresh()")),
        )
        is Action.Tick -> tick(s, a.now)
        Action.Back -> back(s)

        // ---------------- Inventory ----------------
        is Action.Quick -> quick(s, a.kind, a.now)
        is Action.CoilNav -> {
            val node = map.node(s.activeNodeId)
            Next(
                focusNode(s.copy(customerTab = 1), s.activeNodeId),
                listOf(Effect.Toast("Slot ${a.coil} at ${node?.shortName ?: ""} shown on the map", ToastIcon.Navigation), Effect.Haptic()),
            )
        }
        Action.Notify -> watch(s, "Osmanthus")
        Action.Release -> if (s.customer.escrow == null) Next(s) else Next(
            s.copy(customer = s.customer.copy(escrow = null)),
            listOf(Effect.Toast("Hold cancelled · the can is back on sale", ToastIcon.Lock), Effect.Haptic(strong = true), Effect.Log("release", s.customer.escrow.product)),
        )

        // ---------------- Map ----------------
        is Action.Pin -> {
            val target = map.campusOfNode(a.nodeId)
            val dir = if (s.directionsTo != null && s.directionsTo != a.nodeId && target?.id == map.campusOfNode(s.youAreHereNodeId)?.id) a.nodeId else s.directionsTo?.takeIf { it == a.nodeId }
            Next(focusNode(s.copy(directionsTo = dir), a.nodeId), listOf(Effect.Haptic()))
        }
        is Action.Campus -> if (a.id == s.campusId) Next(s) else {
            val c = map.campus(a.id)
            val sel = if (map.campusOfNode(s.selectedNodeId)?.id == c.id) s.selectedNodeId else c.nodes.first().id
            Next(s.copy(campusId = c.id, selectedNodeId = sel, buildingFilter = null, directionsTo = null, search = ""), listOf(Effect.Haptic()))
        }
        is Action.MapFilter -> Next(s.copy(mapFilter = a.filter), listOf(Effect.Haptic()))
        is Action.BuildingFilter -> Next(s.copy(buildingFilter = a.code, sheet = null), listOf(Effect.Haptic()))
        is Action.Search -> Next(s.copy(search = a.query))
        is Action.SearchBuilding -> {
            val n = s.focusNonce + 1
            Next(
                s.copy(
                    campusId = a.campusId, search = "", pulseBuilding = a.code,
                    mapFocus = MapFocus.Building(a.campusId, a.code, n), focusNonce = n,
                    selectedNodeId = if (map.campusOfNode(s.selectedNodeId)?.id == a.campusId) s.selectedNodeId else map.campus(a.campusId).nodes.first().id,
                ),
                listOf(Effect.Toast("Building ${a.code} has no vending machine yet", ToastIcon.Pin)),
            )
        }
        Action.Locate -> {
            val node = map.node(s.youAreHereNodeId)
            val campus = map.campusOfNode(s.youAreHereNodeId)
            val n = s.focusNonce + 1
            Next(
                s.copy(campusId = campus?.id ?: s.campusId, mapFocus = MapFocus.Locate(n), focusNonce = n),
                listOf(Effect.Toast("Located you near Building ${node?.building} · ${node?.shortName}", ToastIcon.Navigation), Effect.Haptic()),
            )
        }
        Action.Compass -> {
            val n = s.focusNonce + 1
            Next(s.copy(mapFocus = MapFocus.Compass(n), focusNonce = n), listOf(Effect.Haptic()))
        }
        is Action.LayerToggle -> {
            val l = s.layers
            Next(
                s.copy(
                    layers = when (a.key) {
                        "nodes" -> l.copy(nodes = !l.nodes)
                        "entrances" -> l.copy(entrances = !l.entrances)
                        "poi" -> l.copy(poi = !l.poi)
                        "dim" -> l.copy(dimDark = !l.dimDark)
                        else -> l
                    },
                ),
                listOf(Effect.Haptic()),
            )
        }
        is Action.Save -> {
            val saved = a.nodeId in s.savedNodes
            val name = map.node(a.nodeId)?.shortName ?: a.nodeId
            Next(
                s.copy(savedNodes = if (saved) s.savedNodes - a.nodeId else s.savedNodes + a.nodeId),
                listOf(Effect.Toast(if (saved) "$name removed from saved" else "$name saved", ToastIcon.Check), Effect.Haptic()),
            )
        }
        Action.Directions -> directions(s)
        Action.ViewStock -> Next(
            s.copy(activeNodeId = s.selectedNodeId, customerTab = 0),
            listOf(Effect.Haptic(), Effect.Toast("Showing stock at ${map.node(s.selectedNodeId)?.shortName}", ToastIcon.Check)),
        )
        is Action.SaveCamera -> Next(s.copy(cameras = s.cameras + (a.campusId to a.camera)))
        Action.FocusConsumed -> Next(s.copy(mapFocus = null))

        // ---------------- Notifications ----------------
        is Action.Nf -> Next(s.copy(customer = s.customer.copy(nf = a.filter)), listOf(Effect.Haptic()))
        Action.ReadAll -> Next(
            s.copy(customer = s.customer.copy(read = Seed.notificationIds)),
            listOf(Effect.Toast("All caught up", ToastIcon.Check), Effect.Haptic()),
        )
        Action.LiveStatus -> Next(s, listOf(Effect.Toast("Machine 01, slot 4 is being restocked at 14:30 · runner Zhang is on the way", ToastIcon.Bolt)))
        is Action.Unsub -> {
            val c = s.customer
            val next = if (a.which == "osmanthus") c.copy(osmanthusWatch = false, watch = c.watch - "Osmanthus") else c.copy(peachWatch = false)
            Next(s.copy(customer = next), listOf(Effect.Toast("Unsubscribed", ToastIcon.Bell), Effect.Haptic(), Effect.Log("unsub", a.which)))
        }
        Action.PushAlert -> {
            val on = !s.customer.peachPush
            Next(s.copy(customer = s.customer.copy(peachPush = on)), listOf(Effect.Toast(if (on) "App alerts on for White Peach" else "App alerts off", ToastIcon.Bell)))
        }
        Action.SmsAlert -> {
            val on = !s.customer.peachSms
            Next(s.copy(customer = s.customer.copy(peachSms = on)), listOf(Effect.Toast(if (on) "SMS drop alerts on" else "SMS drop alerts off", ToastIcon.Bell)))
        }
        is Action.HoldNav -> {
            val c = s.customer
            val escrow = c.escrow ?: Escrow("N02", 1, "White Peach", a.now, a.now + Escrow.CLAIM_MS)
            val next = s.copy(customer = c.copy(escrow = escrow, read = (c.read + "urgent").distinct()), customerTab = 1)
            Next(
                focusNode(next, "N02").let { it.copy(directionsTo = if (map.campusOfNode(it.youAreHereNodeId)?.id == "tc") "N02" else null) },
                listOf(
                    Effect.Toast(if (c.escrow == null) "Holding White Peach at Machine 02 · 10 min" else "Directions to Machine 02", ToastIcon.Lock),
                    Effect.Haptic(strong = true),
                    Effect.Log("holdNav", "N02 coil 1"),
                ),
            )
        }
        is Action.Dismiss -> Next(
            s.copy(customer = s.customer.copy(dismissed = (s.customer.dismissed + a.id).distinct())),
            listOf(Effect.Haptic()),
        )
        is Action.Reorder -> {
            val c = s.customer
            if (c.escrow != null) {
                Next(s.copy(customerTab = 3), listOf(Effect.Toast("You already have a can on hold", ToastIcon.Lock)))
            } else {
                Next(
                    s.copy(
                        customer = c.copy(escrow = Escrow(s.activeNodeId, 3, "Calamansi Lime", a.now, a.now + Escrow.CLAIM_MS), read = (c.read + "timeout").distinct()),
                        customerTab = 3,
                    ),
                    listOf(Effect.Toast("Held again · slot 3 is yours for 5 min", ToastIcon.Lock), Effect.Haptic(strong = true), Effect.Log("reorder", "coil 3")),
                )
            }
        }
        is Action.Pref -> {
            val c = s.customer
            val next = when (a.key) {
                "push" -> c.copy(prefPush = !c.prefPush)
                "sound" -> c.copy(prefSound = !c.prefSound)
                else -> c.copy(prefQuiet = !c.prefQuiet)
            }
            Next(s.copy(customer = next), listOf(Effect.Haptic()))
        }

        // ---------------- VIP ----------------
        Action.CopyCode -> Next(s, listOf(Effect.CopyText(Escrow.BACKUP_CODE), Effect.Haptic()))
        Action.Qr -> Next(s.copy(sheet = SheetKind.Qr), listOf(Effect.Haptic()))
        Action.Redeem -> Next(s.copy(sheet = SheetKind.Redeem))
        is Action.DoRedeem -> redeem(s, a.id)

        // ---------------- Supplier ----------------
        is Action.SelectCoil -> Next(s.copy(supplier = s.supplier.copy(selected = a.id)), listOf(Effect.Haptic()))
        is Action.RestockCoil -> restockCoil(s, a.clock)
        is Action.Velocity -> Next(s.copy(supplier = s.supplier.copy(velocitySel = a.index)), listOf(Effect.Haptic()))
        is Action.AiDispatch -> {
            val sp = s.supplier
            val stage = if (sp.stage < 3) sp.stage + 1 else sp.stage
            val ev = event(sp, FeedType.Dispatch, "AI Dispatch Issued", "3 cases White Peach · 2 Lychee", a.clock,
                "Predictive engine raised a restock ticket ahead of the 13:55 White Peach stockout.", listOf("Runner Zhang", "Smart Cart 04"))
            Next(
                s.copy(supplier = sp.copy(stage = stage, feed = listOf(ev) + sp.feed, eventSeq = sp.eventSeq + 1), supplierTab = 1, aBadge = s.aBadge + 1),
                listOf(Effect.Toast("Dispatch ticket sent to Zhang", ToastIcon.Truck), Effect.Haptic(), Effect.Log("aiDispatch", "stage $stage")),
            )
        }
        Action.PingRunner -> {
            val sp = s.supplier
            Next(
                s.copy(supplier = sp.copy(runnerPinged = true, stage = maxOf(sp.stage, 3).coerceAtMost(4))),
                listOf(Effect.Toast("Zhang pinged · Runner en route, ETA 6 min", ToastIcon.Truck), Effect.Haptic(), Effect.Log("pingRunner", "")),
            )
        }
        Action.CallRunner -> Next(s, listOf(Effect.Toast("Calling Zhang · Logistics 02…", ToastIcon.Info), Effect.Haptic()))
        is Action.Approve -> approveClearance(s, a.clock)
        Action.Decline -> {
            val name = Clearance.candidate(s.supplier)?.name ?: ""
            Next(s.copy(supplier = s.supplier.copy(clearance = "dismissed", clearanceDismissed = name)), listOf(Effect.Toast("Clearance suggestion dismissed"), Effect.Haptic()))
        }
        is Action.EndClearance -> {
            val sp = s.supplier
            if (a.name !in sp.prices) Next(s) else {
                val ev = event(sp, FeedType.Clearance, "Clearance Ended", a.name, a.clock,
                    "${a.name} back to ${Clearance.yuan(Clearance.LIST_PRICE)} on digital shelf tags.", listOf("E-Ink Shelf Updated"))
                Next(
                    s.copy(supplier = sp.copy(prices = sp.prices - a.name, feed = listOf(ev) + sp.feed, eventSeq = sp.eventSeq + 1)),
                    listOf(Effect.Toast("${a.name} back to ${Clearance.yuan(Clearance.LIST_PRICE)}", ToastIcon.Check), Effect.Haptic(), Effect.Log("endClearance", a.name)),
                )
            }
        }
        Action.Guide -> Next(s.copy(sheet = SheetKind.Guide, guideStep = 0), listOf(Effect.Haptic()))
        is Action.GuideStep -> {
            val last = (s.session?.role?.let { Guide.steps(it).size } ?: 1) - 1
            Next(s.copy(guideStep = (s.guideStep + a.delta).coerceIn(0, last.coerceAtLeast(0))), listOf(Effect.Haptic()))
        }
        is Action.GuideGo -> tab(s.copy(sheet = null), a.tab)
        is Action.FindDrink -> findDrink(s, a.key)
        is Action.Watch -> watch(s, a.key)
        Action.DropAlerts -> {
            val on = !s.customer.dropAlerts
            val names = Seed.drops.joinToString(", ") { it.name }
            Next(
                s.copy(customer = s.customer.copy(dropAlerts = on)),
                if (on) listOf(
                    Effect.Toast("Drop alerts on · we'll notify your phone", ToastIcon.Bell), Effect.Haptic(),
                    Effect.SystemNotify("Product drop alerts are on", "Coming up: $names. You'll get a notification when each one lands."),
                    Effect.Log("dropAlerts", "on"),
                ) else listOf(Effect.Toast("Drop alerts off", ToastIcon.Bell), Effect.Haptic(), Effect.Log("dropAlerts", "off")),
            )
        }
        is Action.ProductDetails -> Next(s.copy(sheet = SheetKind.Product, productKey = a.key), listOf(Effect.Haptic()))
        is Action.StockCampus -> Next(s.copy(campusId = a.campusId), listOf(Effect.Haptic()))
        is Action.Surge -> {
            val sp = s.supplier
            val ev = event(sp, FeedType.Surge, "Foot-Traffic Surge", "450 students · Hall G", a.clock,
                "Simulated dismissal surge: +64 cans dispensed, queue +13. Forecast recalculated.", listOf("Predictive AI", "28°C"))
            Next(
                s.copy(
                    supplier = sp.copy(
                        dispensed = sp.dispensed + 64, queue = sp.queue + 13, fillBase = sp.fillBase - 11,
                        feed = listOf(ev) + sp.feed, eventSeq = sp.eventSeq + 1, surges = sp.surges + 1,
                        restockConfirmed = false, stage = if (sp.stage >= 4) 2 else sp.stage,
                    ),
                    aBadge = s.aBadge + 1,
                ),
                listOf(Effect.Toast("Rush simulated · 64 more cans sold, 13 more students waiting", ToastIcon.Bolt), Effect.Haptic(strong = true), Effect.Log("surge", "+64 / +13")),
            )
        }
        is Action.ConfirmRestock -> {
            val sp = s.supplier
            if (sp.restockConfirmed) Next(s) else {
                val ev = event(sp, FeedType.Restock, "Restock Confirmed", "Zhang — stock 100%", a.clock,
                    "All 16 slots are full. Waiting students were told, first come first served.", listOf("Checked by machine sensors"))
                Next(
                    s.copy(
                        supplier = sp.copy(
                            coils = sp.coils.map { it.copy(level = 15) }, stage = 4, queue = 0, urgentResolved = true,
                            restockConfirmed = true, feed = listOf(ev) + sp.feed, eventSeq = sp.eventSeq + 1,
                        ),
                    ),
                    listOf(Effect.Toast("Machine 01 is full again", ToastIcon.Check), Effect.Haptic(strong = true), Effect.Log("confirmRestock", "16 coils → 15")),
                )
            }
        }
        Action.Ping -> Next(
            s.copy(supplier = s.supplier.copy(urgentPinged = true)),
            listOf(Effect.Toast("Runner Zhang pinged", ToastIcon.Bell), Effect.Haptic()),
        )
        is Action.AlertFilter -> Next(s.copy(supplier = s.supplier.copy(alertFilter = a.filter)), listOf(Effect.Haptic()))
        Action.Mute -> {
            val m = !s.supplier.muted
            Next(s.copy(supplier = s.supplier.copy(muted = m)), listOf(Effect.Toast(if (m) "Alerts muted" else "Alerts on", ToastIcon.Bell), Effect.Haptic()))
        }
    }

    // ------------------------------------------------------------------

    private fun login(s: AppState, method: String): Next {
        if (!s.agreed) return agreeFirst(s)
        return when (method) {
            "sms" -> when {
                !validPhone(s.phone) -> shake(s, "phone", "Enter a valid 11-digit mobile number")
                s.smsCode.length != 6 -> shake(s, "code", "Enter the 6-digit SMS code")
                s.smsCode != DEMO_CODE -> shake(s, "code", "Incorrect code · tap 获取验证码 for a new one")
                else -> Next(s.copy(loggingIn = "sms"), listOf(Effect.StartLogin("sms", s.loginRole)))
            }
            "cas" -> Next(s.copy(sheet = SheetKind.Cas, casError = null))
            else -> Next(s.copy(loggingIn = method), listOf(Effect.StartLogin(method, s.loginRole), Effect.Haptic()))
        }
    }

    private fun agreeFirst(s: AppState) = shake(s, "agree", "Please accept the user agreement first")

    private fun shake(s: AppState, field: String, msg: String) = Next(
        s.copy(shakeField = field, shakeNonce = s.shakeNonce + 1),
        listOf(Effect.Toast(msg, ToastIcon.Alert), Effect.Haptic(strong = true)),
    )

    private fun findDrink(s: AppState, key: String): Next {
        val p = Seed.product(key) ?: return Next(s)
        val here = map.node(s.activeNodeId)?.ready?.get(key) ?: 0
        if (here > 0) return Next(s.copy(sheet = null, customerTab = 0), listOf(Effect.Toast("${p.name} is in stock right here · $here left", ToastIcon.Check), Effect.Haptic()))
        val n = Nearby.nearest(map, s.activeNodeId, key)
            ?: return Next(s.copy(sheet = null), listOf(Effect.Toast("${p.name} is sold out everywhere right now · tap Notify Me", ToastIcon.Bell)))
        val base = s.copy(sheet = null)
        return Next(
            if (Features.campusMap) focusNode(base.copy(customerTab = 1), n.id) else base,
            listOf(Effect.Toast("Nearest ${p.name}: ${n.shortName} · ${n.label} · ${n.walkMin} min walk (${n.ready[key]} left)", ToastIcon.Navigation), Effect.Haptic()),
        )
    }

    private fun watch(s: AppState, key: String): Next {
        val p = Seed.product(key) ?: return Next(s)
        val c = s.customer
        val on = if (key == "Osmanthus") !c.osmanthusWatch else key !in c.watch
        val next = c.copy(
            watch = if (on) (c.watch - key) + key else c.watch - key,
            osmanthusWatch = if (key == "Osmanthus") on else c.osmanthusWatch,
        )
        return Next(
            s.copy(customer = next),
            if (on) listOf(
                Effect.Toast("Subscribed · we'll notify your phone when ${p.name} is back", ToastIcon.Bell), Effect.Haptic(),
                Effect.SystemNotify("Following ${p.name}", "You'll get a notification here as soon as ${p.name} is restocked near you."),
                Effect.Log("watch", "on $key"),
            ) else listOf(Effect.Toast("${p.name} alert removed", ToastIcon.Bell), Effect.Haptic(), Effect.Log("watch", "off $key")),
        )
    }

    private fun approveClearance(s: AppState, clock: String): Next {
        val sp = s.supplier
        val c = Clearance.candidate(sp) ?: return Next(s, listOf(Effect.Toast("No slow-moving product to mark down")))
        val to = Clearance.salePrice(c.price)
        val ev = event(sp, FeedType.Clearance, "Clearance −${Clearance.DISCOUNT_PCT}% Approved", c.name, clock,
            "${c.name} cut to ${Clearance.yuan(to)} on digital shelf tags. ${c.stock} cans left, ${Clearance.hoursLabel(c.hours)}.",
            listOf("E-Ink Shelf Updated", "Old Price ${Clearance.yuan(c.price)}"))
        return Next(
            s.copy(supplier = sp.copy(clearance = "approved", prices = sp.prices + (c.name to to), feed = listOf(ev) + sp.feed, eventSeq = sp.eventSeq + 1)),
            listOf(Effect.Toast("${c.name} now ${Clearance.yuan(to)}", ToastIcon.Check), Effect.Haptic(), Effect.Log("approve", "${c.name} ${Clearance.yuan(to)}")),
        )
    }

    private fun tab(s: AppState, index: Int): Next {
        val role = s.session?.role ?: return Next(s)
        return if (role == Role.Student) {
            if (index == s.customerTab) Next(s) else Next(s.copy(customerTab = index, search = ""), listOf(Effect.Haptic()))
        } else {
            if (index == s.supplierTab) Next(s) else Next(
                s.copy(supplierTab = index, aBadge = if (index == 2) 0 else s.aBadge),
                listOf(Effect.Haptic()),
            )
        }
    }

    private fun back(s: AppState): Next = when {
        s.sheet != null -> Next(s.copy(sheet = null, casError = null))
        s.session?.role == Role.Student && s.customerTab != 0 -> Next(s.copy(customerTab = 0))
        s.session?.role == Role.Supplier && s.supplierTab != 0 -> Next(s.copy(supplierTab = 0))
        else -> Next(s)
    }

    private fun tick(s: AppState, now: Long): Next {
        val e = s.customer.escrow ?: return Next(s)
        return if (now >= e.expiresAt) Next(
            s.copy(customer = s.customer.copy(escrow = null)),
            listOf(Effect.Toast("Your hold ran out · the can is back on sale", ToastIcon.Lock), Effect.Haptic(strong = true), Effect.Log("escrowExpired", e.product)),
        ) else Next(s)
    }

    private fun quick(s: AppState, kind: String, now: Long): Next {
        val node = map.node(s.activeNodeId)
        return when (kind) {
            "Route" -> {
                val next = focusNode(s.copy(customerTab = 1), s.activeNodeId)
                val sameCampus = map.campusOfNode(s.youAreHereNodeId)?.id == map.campusOfNode(s.activeNodeId)?.id
                Next(
                    next.copy(directionsTo = if (sameCampus && s.youAreHereNodeId != s.activeNodeId) s.activeNodeId else null),
                    listOf(Effect.Toast(if (s.youAreHereNodeId == s.activeNodeId) "You're already at ${node?.shortName}" else "Directions started · ${node?.walkMin} min walk", ToastIcon.Navigation), Effect.Haptic()),
                )
            }
            "I'm Here" -> Next(
                s.copy(youAreHereNodeId = s.activeNodeId),
                listOf(Effect.Toast("Checked in at ${node?.shortName} · Building ${node?.building}", ToastIcon.Pin), Effect.Haptic(), Effect.Log("checkIn", s.activeNodeId)),
            )
            "Lock" -> when {
                s.customer.escrow != null -> Next(s, listOf(Effect.Toast("You already have a can on hold", ToastIcon.Lock), Effect.Haptic()))
                (node?.ready?.get("Calamansi") ?: 0) <= 0 -> Next(s, listOf(Effect.Toast("Slot 3 is empty at ${node?.shortName}", ToastIcon.Alert)))
                else -> Next(
                    s.copy(customer = s.customer.copy(escrow = Escrow(s.activeNodeId, 3, "Calamansi Lime", now, now + Escrow.CLAIM_MS))),
                    listOf(Effect.Toast("Can on hold · pick it up within 5 min", ToastIcon.Lock), Effect.Haptic(strong = true), Effect.Log("lock", "${s.activeNodeId} coil 3")),
                )
            }
            else -> Next(
                s,
                listOf(Effect.ShareText("${node?.name} · ${node?.location} — chilled Chi Forest drinks ready now. chiforest://node/${node?.id}")),
            )
        }
    }

    private fun directions(s: AppState): Next {
        val node = map.node(s.selectedNodeId) ?: return Next(s)
        if (s.directionsTo == s.selectedNodeId) {
            return Next(s.copy(directionsTo = null), listOf(Effect.Toast("Directions cleared"), Effect.Haptic()))
        }
        val here = map.campusOfNode(s.youAreHereNodeId)
        if (here?.id != map.campusOfNode(node.id)?.id) {
            return Next(s, listOf(Effect.Toast("Check in on this campus first (I'm Here)", ToastIcon.Alert)))
        }
        if (s.youAreHereNodeId == node.id) {
            return Next(s, listOf(Effect.Toast("You're already at ${node.shortName}", ToastIcon.Pin)))
        }
        return Next(
            s.copy(directionsTo = node.id),
            listOf(Effect.Toast("Directions started · ${node.walkMin} min walk", ToastIcon.Navigation), Effect.Haptic(), Effect.Log("directions", node.id)),
        )
    }

    private fun redeem(s: AppState, id: String): Next {
        val drop = Seed.drops.firstOrNull { it.id == id } ?: return Next(s)
        val c = s.customer
        return when {
            id in c.redeemed -> Next(s)
            c.points < drop.pts -> Next(s, listOf(Effect.Toast("Not enough points · ${drop.pts - c.points} pts short", ToastIcon.Alert)))
            else -> Next(
                s.copy(customer = c.copy(points = c.points - drop.pts, redeemed = c.redeemed + id)),
                listOf(
                    Effect.Toast("Redeemed ${drop.name} · pickup code ${pickupCode(id)}", ToastIcon.Gift),
                    Effect.Haptic(strong = true),
                    Effect.Log("redeem", "${drop.name} −${drop.pts} pts"),
                ),
            )
        }
    }

    private fun restockCoil(s: AppState, clock: String): Next {
        val sp = s.supplier
        val coil = sp.coil(sp.selected) ?: return Next(s)
        if (coil.level >= 15) return Next(s, listOf(Effect.Toast("${coil.id} is already full")))
        val isC3 = coil.id == "C3"
        val ev = event(sp, FeedType.Restock, "Slot ${coil.id} restocked", "${coil.name} · 15/15", clock,
            if (isC3) "Empty slot refilled. Waiting students were told and the alert is cleared." else "Slot filled to the top by runner Zhang.",
            listOf("Checked by machine sensors"))
        return Next(
            s.copy(
                supplier = sp.copy(
                    coils = sp.coils.map { if (it.id == coil.id) it.copy(level = 15) else it },
                    queue = if (isC3) 0 else sp.queue,
                    urgentResolved = sp.urgentResolved || isC3,
                    feed = listOf(ev) + sp.feed, eventSeq = sp.eventSeq + 1,
                ),
                aBadge = s.aBadge + 1,
            ),
            listOf(Effect.Toast("${coil.id} restocked · 15/15", ToastIcon.Check), Effect.Haptic(strong = true), Effect.Log("restockCoil", coil.id)),
        )
    }

    private fun event(sp: SupplierState, type: FeedType, title: String, sub: String, clock: String, body: String, chips: List<String>) =
        FeedEvent("e${sp.eventSeq + 1}", type, title, sub, "$clock · now", body, chips)

    private fun focusNode(s: AppState, nodeId: String): AppState {
        val campus = map.campusOfNode(nodeId) ?: return s
        val n = s.focusNonce + 1
        return s.copy(
            selectedNodeId = nodeId, campusId = campus.id, buildingFilter = null,
            mapFocus = MapFocus.Node(nodeId, n), focusNonce = n, pulseBuilding = null,
        )
    }

    companion object {
        const val DEMO_CODE = "482915"
        fun validPhone(p: String) = Regex("^1[3-9]\\d{9}$").matches(p)
        fun maskPhone(p: String) = if (p.length == 11) "${p.take(3)}****${p.takeLast(4)}" else p
        fun pickupCode(id: String) = "PK-" + (id.hashCode().toUInt() % 9000u + 1000u).toString()

        /** Map filter predicate (the prototype's match()). */
        fun matches(node: MapNode, filter: String, building: String?, query: String, map: MapData): Boolean {
            val f = when (filter) {
                "chilled" -> node.tempC < 4f
                "high" -> node.pct >= 75
                else -> true
            }
            val b = building == null || node.building.equals(building, ignoreCase = true)
            val q = query.isBlank() || MapSearch.search(map, query).nodes.any { it.id == node.id }
            return f && b && q
        }
    }
}
