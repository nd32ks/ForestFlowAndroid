package com.chiforest.forestflow.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chiforest.forestflow.auth.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Single source of truth: StateFlow<AppState> + SharedFlow<Effect>, persisted to SQLite on every change. */
class AppViewModel(private val c: AppContainer) : ViewModel() {
    private val reducer = Reducer(c.mapData)
    val mapData: MapData get() = c.mapData
    val authBackend: String get() = c.auth.backendName

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /** Wall clock, ticking every second; countdowns are derived from it. */
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 64)
    val effects: SharedFlow<Effect> = _effects

    private val _activity = MutableStateFlow<List<LogRow>>(emptyList())
    val activity: StateFlow<List<LogRow>> = _activity.asStateFlow()

    init {
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) {
                runCatching { c.db.loadState() }.getOrNull()?.let { runCatching { json.decodeFromString(AppState.serializer(), it) }.getOrNull() }
            }
            _state.value = restored ?: Seed.fresh(System.currentTimeMillis())
            _ready.value = true
            dispatch(Action.Tick(System.currentTimeMillis())) // expire escrows that lapsed while closed
            persist()
        }
        viewModelScope.launch(Dispatchers.IO) { runCatching { c.localAuth.seedDemoAccounts() } }
        viewModelScope.launch {
            while (true) {
                val t = System.currentTimeMillis()
                delay(1000 - t % 1000)
                _now.value = System.currentTimeMillis()
                if (_ready.value) dispatch(Action.Tick(_now.value))
            }
        }
    }

    private fun persist() {
        viewModelScope.launch {
            _state.map { json.encodeToString(AppState.serializer(), it) }
                .distinctUntilChanged()
                .drop(1)
                .conflate() // no debounce: a sign-out is on disk before the user can kill the app
                .collect { snapshot -> withContext(Dispatchers.IO) { runCatching { c.db.saveState(snapshot, System.currentTimeMillis()) } } }
        }
    }

    fun dispatch(action: Action) {
        val next = reducer.reduce(_state.value, action)
        _state.value = next.state
        next.effects.forEach(::run)
    }

    fun clock(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())

    private fun run(effect: Effect) {
        when (effect) {
            is Effect.StartLogin -> viewModelScope.launch {
                delay(650)
                val name = when (effect.method) {
                    "wechat" -> if (effect.role == Role.Student) "WeChat Student" else "WeChat Supplier"
                    "sms" -> "Mobile ${Reducer.maskPhone(_state.value.phone)}"
                    else -> if (effect.role == Role.Student) "Campus Visitor" else "Visiting Staff"
                }
                dispatch(Action.LoginSucceeded(Session("guest-${effect.method}-${System.currentTimeMillis()}", name, effect.role, effect.method)))
            }
            Effect.ScheduleAutofill -> viewModelScope.launch {
                delay(900)
                dispatch(Action.CodeAutofill)
            }
            Effect.SignOut -> viewModelScope.launch { c.auth.signOut() }
            is Effect.Log -> {
                val role = _state.value.session?.role?.name ?: "Guest"
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        c.db.log(System.currentTimeMillis(), role, effect.action, effect.detail)
                        _activity.value = c.db.recentLogs(8)
                    }
                }
            }
            else -> _effects.tryEmit(effect)
        }
    }

    fun refreshActivity() {
        viewModelScope.launch(Dispatchers.IO) { runCatching { _activity.value = c.db.recentLogs(8) } }
    }

    /** XJTLU CAS sheet: email/password against the active AuthRepository. Role must match the selected tab. */
    fun casSubmit(email: String, password: String, create: Boolean, name: String) {
        if (_state.value.casBusy) return
        if (!_state.value.agreed) {
            dispatch(Action.CasFailed("Accept the user agreement first"))
            return
        }
        val wanted = _state.value.loginRole
        dispatch(Action.CasStarted)
        viewModelScope.launch {
            val r = runCatching {
                if (create) c.auth.signUp(name, email, password, wanted) else c.auth.signIn(email, password)
            }.getOrElse { AuthResult.Err("Sign-in is unavailable right now, try again") }
            when (r) {
                is AuthResult.Err -> dispatch(Action.CasFailed(r.message))
                is AuthResult.Ok -> if (r.session.role != wanted) {
                    c.auth.signOut()
                    dispatch(Action.CasFailed("This is a ${r.session.role.name} account · switch to the ${r.session.role.name} tab"))
                } else {
                    delay(250)
                    dispatch(Action.LoginSucceeded(r.session))
                }
            }
        }
    }

    class Factory(private val c: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(c) as T
    }
}
