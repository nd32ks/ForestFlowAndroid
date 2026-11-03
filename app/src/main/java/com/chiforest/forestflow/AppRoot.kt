package com.chiforest.forestflow

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.chiforest.forestflow.feature.customer.LocalDrinkAssistant
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chiforest.forestflow.core.Action
import com.chiforest.forestflow.core.AppState
import com.chiforest.forestflow.core.AppViewModel
import com.chiforest.forestflow.core.Effect
import com.chiforest.forestflow.core.Features
import com.chiforest.forestflow.core.LogRow
import com.chiforest.forestflow.core.Role
import com.chiforest.forestflow.core.SheetKind
import com.chiforest.forestflow.core.ToastIcon
import com.chiforest.forestflow.feature.customer.BuildingsSheet
import com.chiforest.forestflow.feature.customer.InventoryScreen
import com.chiforest.forestflow.feature.customer.LayersSheet
import com.chiforest.forestflow.feature.customer.MapScreen
import com.chiforest.forestflow.feature.customer.NotificationsScreen
import com.chiforest.forestflow.feature.customer.QrSheet
import com.chiforest.forestflow.feature.customer.RedeemSheet
import com.chiforest.forestflow.feature.customer.StationsSheet
import com.chiforest.forestflow.feature.customer.VipScreen
import com.chiforest.forestflow.feature.login.CasSheet
import com.chiforest.forestflow.feature.login.LoginScreen
import com.chiforest.forestflow.feature.supplier.AlertsScreen
import com.chiforest.forestflow.feature.supplier.DispatchScreen
import com.chiforest.forestflow.feature.supplier.TelemetryScreen
import com.chiforest.forestflow.ui.components.BtnStyle
import com.chiforest.forestflow.ui.components.ForestButton
import com.chiforest.forestflow.ui.components.ForestSheet
import com.chiforest.forestflow.ui.components.GlassHeader
import com.chiforest.forestflow.ui.components.rememberLowEndDevice
import com.chiforest.forestflow.ui.components.GlassNavBar
import com.chiforest.forestflow.ui.components.Hairline
import com.chiforest.forestflow.ui.components.Ic
import com.chiforest.forestflow.ui.components.LogoMark
import com.chiforest.forestflow.ui.components.NavItem
import com.chiforest.forestflow.ui.components.SheetTitle
import com.chiforest.forestflow.ui.components.ToastController
import com.chiforest.forestflow.ui.components.ToastHost
import com.chiforest.forestflow.ui.theme.FF
import com.chiforest.forestflow.ui.theme.Motion
import com.chiforest.forestflow.ui.theme.Type
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppRoot(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    // Held as State and read only inside the countdown composables, so the clock tick does not recompose the whole app.
    val now = vm.now.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()
    val toasts = remember { ToastController() }
    val view = LocalView.current
    val context = LocalContext.current
    val dispatch: (Action) -> Unit = vm::dispatch

    LaunchedEffect(Unit) {
        vm.effects.collect { e ->
            when (e) {
                is Effect.Toast -> toasts.show(e.text, e.icon)
                is Effect.Haptic -> view.performHapticFeedback(
                    if (e.strong) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.VIRTUAL_KEY,
                )
                is Effect.CopyText -> {
                    val ok = runCatching {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Chi Forest backup code", e.text))
                    }.isSuccess
                    toasts.show(if (ok) "Backup code ${e.text} copied" else "Backup code: ${e.text}", ToastIcon.Copy)
                }
                is Effect.ShareText -> runCatching {
                    context.startActivity(
                        Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, e.text), "Share station")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure { toasts.show("Sharing is unavailable on this device", ToastIcon.Alert) }
                else -> Unit
            }
        }
    }

    val role = s.session?.role
    val tab = if (role == Role.Supplier) s.supplierTab else s.customerTab
    BackHandler(enabled = s.sheet != null || (role != null && tab != 0)) { dispatch(Action.Back) }

    Box(Modifier.fillMaxSize().background(FF.c.bg)) {
        if (!ready) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LogoMark(72) }
        } else {
            val riseDp = with(LocalDensity.current) { 18.dp.roundToPx() }
            AnimatedContent(
                targetState = role,
                transitionSpec = {
                    (fadeIn(tween(Motion.ENTER_MS, easing = Motion.EaseOutQuint)) + slideInVertically(tween(Motion.ENTER_MS, easing = Motion.EaseOutQuint)) { riseDp }) togetherWith
                        fadeOut(tween(200))
                },
                label = "roleSwitch",
            ) { r ->
                when (r) {
                    null -> LoginScreen(s, now, dispatch)
                    else -> CompositionLocalProvider(LocalDrinkAssistant provides vm.assistant) { RoleShell(s, now, r, vm, dispatch) }
                }
            }
        }
        ToastHost(toasts, bottomOffset = if (role != null) 92.dp else 24.dp, modifier = Modifier.align(Alignment.BottomCenter))
    }

    when (s.sheet) {
        SheetKind.Profile -> ProfileSheet(s, vm, dispatch)
        SheetKind.Buildings -> BuildingsSheet(s, vm.mapData, dispatch)
        SheetKind.Layers -> LayersSheet(s, vm.mapData, dispatch)
        SheetKind.Stations -> StationsSheet(s, vm.mapData, dispatch)
        SheetKind.Redeem -> RedeemSheet(s, dispatch)
        SheetKind.Qr -> QrSheet(s, vm.mapData, dispatch)
        SheetKind.Cas -> CasSheet(s, vm.authBackend, vm::casSubmit, dispatch)
        null -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoleShell(s: AppState, now: State<Long>, role: Role, vm: AppViewModel, dispatch: (Action) -> Unit) {
    val haze = rememberHazeState()
    val map = vm.mapData
    val student = role == Role.Student
    // Hidden features drop out of the student tabs; a tab id saved earlier falls back to the first visible one.
    val studentTabs = Features.studentTabs()
    val tab = if (student) Features.effectiveStudentTab(s.customerTab) else s.supplierTab
    val slidePx = with(LocalDensity.current) { Motion.TabSlide.roundToPx() }
    val imeVisible = WindowInsets.isImeVisible

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().then(if (rememberLowEndDevice()) Modifier else Modifier.hazeSource(haze))) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(Motion.TAB_MS, easing = Motion.EaseOutQuint)) { dir * slidePx } +
                        fadeIn(tween(Motion.TAB_MS)) +
                        scaleIn(tween(Motion.TAB_MS, easing = Motion.EaseOutQuint), initialScale = 0.985f)) togetherWith
                        (slideOutHorizontally(tween(220)) { -dir * slidePx / 2 } + fadeOut(tween(160)))
                },
                label = "tabs",
            ) { t ->
                if (student) when (t) {
                    0 -> InventoryScreen(s, now, map, dispatch)
                    1 -> MapScreen(s, map, dispatch)
                    2 -> NotificationsScreen(s, dispatch)
                    else -> VipScreen(s, now, map, dispatch)
                } else when (t) {
                    0 -> TelemetryScreen(s, vm::clock, dispatch)
                    1 -> DispatchScreen(s, vm::clock, dispatch)
                    else -> AlertsScreen(s, dispatch)
                }
            }
        }

        val subtitle = when {
            !student -> "Node 01 Admin · Taicang"
            tab == 1 -> map.campus(s.campusId).subtitle
            else -> {
                val n = map.node(s.activeNodeId)
                "${map.campusOfNode(s.activeNodeId)?.subtitle ?: "XJTLU"} · ${n?.shortName ?: ""}"
            }
        }
        GlassHeader(
            haze, admin = !student, subtitle = subtitle,
            initials = initials(s.session?.displayName ?: "?"),
            onAvatar = { dispatch(Action.Profile) },
            temp = if (student) "28°" else null,
        )

        AnimatedVisibility(
            visible = !imeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { (it * 1.2f).toInt() } + fadeOut(),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                val items = if (student) studentTabs.map { id ->
                    when (id) {
                        0 -> NavItem(Ic.Box, "Inventory")
                        1 -> NavItem(Ic.Navigation, "Campus Map")
                        2 -> NavItem(Ic.Bell, "Notifications", dot = s.unread(Features.vipClub) > 0)
                        else -> NavItem(Ic.IdCard, "VIP Pass")
                    }
                } else listOf(
                    NavItem(Ic.BarChart, "Telemetry"),
                    NavItem(Ic.Truck, "Dispatch"),
                    NavItem(Ic.Bell, "Alerts", count = s.aBadge),
                )
                val selected = if (student) studentTabs.indexOf(tab).coerceAtLeast(0) else tab
                GlassNavBar(haze, items, selected, { i -> dispatch(Action.Tab(if (student) studentTabs[i] else i)) })
            }
        }
    }
}

private fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() && it.first().isLetter() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "CF"
    }
}

@Composable
private fun ProfileSheet(s: AppState, vm: AppViewModel, dispatch: (Action) -> Unit) {
    val c = FF.c
    val activity by vm.activity.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshActivity() }
    val session = s.session ?: return
    val node = vm.mapData.node(s.activeNodeId)
    ForestSheet(onDismiss = { dispatch(Action.CloseSheet) }) {
        SheetTitle(session.displayName, if (session.role == Role.Student) "Student · Campus app" else "Supplier · Admin console") { dispatch(Action.CloseSheet) }
        InfoRow("Role", session.role.name)
        Hairline()
        InfoRow("Station", if (session.role == Role.Student) "${node?.shortName} · ${node?.label}" else "Node 01 · Bldg G Atrium")
        Hairline()
        InfoRow("Session", buildString {
            append(
                when (session.method) {
                    "wechat" -> "WeChat (demo)"
                    "sms" -> "SMS code (demo)"
                    "cas" -> "XJTLU CAS · ${vm.authBackend}"
                    else -> "Visitor pass (demo)"
                },
            )
            session.email?.let { append(" · $it") }
        })
        Hairline()
        InfoRow("Database", "SQLite · forestflow.db")
        Spacer(Modifier.height(14.dp))
        Text("RECENT ACTIVITY · activity_log", style = Type.eyebrow(10.sp), color = c.faint)
        Spacer(Modifier.height(6.dp))
        if (activity.isEmpty()) {
            Text("No actions logged yet.", style = Type.body(13.sp), color = c.faint)
        } else {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface2).padding(horizontal = 12.dp, vertical = 6.dp)) {
                activity.take(6).forEach { LogLine(it) }
            }
        }
        Spacer(Modifier.height(16.dp))
        ForestButton("Reset demo data", { dispatch(Action.Reset(System.currentTimeMillis())) }, Modifier.fillMaxWidth(), style = BtnStyle.Outline, icon = Ic.Refresh)
        Spacer(Modifier.height(10.dp))
        ForestButton("Sign out", { dispatch(Action.Logout) }, Modifier.fillMaxWidth(), style = BtnStyle.Tonal, icon = Ic.Logout)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val c = FF.c
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Type.body(14.sp), color = c.faint, modifier = Modifier.width(92.dp))
        Text(value, style = Type.body(14.sp, 600), color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LogLine(row: LogRow) {
    val c = FF.c
    val time = remember(row.ts) { SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(row.ts)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Ic.Database, null, tint = c.faint, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(8.dp))
        Text(time, style = Type.mono(11.sp, 500), color = c.faint)
        Spacer(Modifier.width(8.dp))
        Text("${row.action} · ${row.detail}", style = Type.body(12.sp, 500), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
