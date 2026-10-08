package com.chiforest.forestflow.core

/** Every user intent in the app. Names mirror the prototype's act() switch (see docs/PARITY.md). */
sealed interface Action {
    // Login
    data class SetRole(val role: Role) : Action
    data object Agree : Action
    data class Phone(val value: String) : Action
    data class Code(val value: String) : Action
    data class GetCode(val now: Long) : Action
    data object CodeAutofill : Action
    data class Login(val method: String) : Action          // wechat | sms | cas
    data object Visitor : Action
    data class LoginSucceeded(val session: Session) : Action
    data class CasFailed(val message: String) : Action
    data object CasStarted : Action

    // Shell
    data class Tab(val index: Int) : Action
    data object Profile : Action
    data class OpenSheet(val kind: SheetKind) : Action
    data object CloseSheet : Action
    data object Logout : Action
    data class Reset(val now: Long) : Action
    data class Tick(val now: Long) : Action
    data object Back : Action

    // Inventory
    data class Quick(val kind: String, val now: Long) : Action  // Route | I'm Here | Lock | Share
    data class CoilNav(val coil: Int) : Action
    data object Notify : Action
    data object Release : Action

    // Map
    data class Pin(val nodeId: String) : Action
    data class Campus(val id: String) : Action
    data class MapFilter(val filter: String) : Action
    data class BuildingFilter(val code: String?) : Action
    data class Search(val query: String) : Action
    data class SearchBuilding(val campusId: String, val code: String) : Action
    data object Locate : Action
    data object Compass : Action
    data class LayerToggle(val key: String) : Action
    data class Save(val nodeId: String) : Action
    data object Directions : Action
    data object ViewStock : Action
    data class SaveCamera(val campusId: String, val camera: Camera) : Action
    data object FocusConsumed : Action

    // Notifications
    data class Nf(val filter: String) : Action
    data object ReadAll : Action
    data object LiveStatus : Action
    data class Unsub(val which: String) : Action
    data object PushAlert : Action
    data object SmsAlert : Action
    data class HoldNav(val now: Long) : Action
    data class Dismiss(val id: String) : Action
    data class Reorder(val now: Long) : Action
    data class Pref(val key: String) : Action                // push | sound | quiet

    // VIP
    data object CopyCode : Action
    data object Qr : Action
    data object Redeem : Action
    data class DoRedeem(val id: String) : Action

    // Supplier
    data class SelectCoil(val id: String) : Action
    data class RestockCoil(val clock: String) : Action
    data class Velocity(val index: Int) : Action
    data class AiDispatch(val clock: String) : Action
    data object PingRunner : Action
    data object CallRunner : Action
    data class Approve(val clock: String) : Action
    data object Decline : Action
    data class Surge(val clock: String) : Action
    data class ConfirmRestock(val clock: String) : Action
    data object Ping : Action
    data class AlertFilter(val filter: String) : Action
    data object Mute : Action
}

enum class ToastIcon { None, Check, Lock, Alert, Bell, Bolt, Copy, Gift, Navigation, Truck, Pin, Info }

sealed interface Effect {
    data class Toast(val text: String, val icon: ToastIcon = ToastIcon.None) : Effect
    data class Haptic(val strong: Boolean = false) : Effect
    data class CopyText(val text: String) : Effect
    data class ShareText(val text: String) : Effect
    data class StartLogin(val method: String, val role: Role) : Effect
    data object ScheduleAutofill : Effect
    data object SignOut : Effect
    /** Written to the activity_log table in SQLite. */
    data class Log(val action: String, val detail: String) : Effect
}
