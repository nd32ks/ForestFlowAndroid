package com.chiforest.forestflow.core

import android.content.Context
import com.chiforest.forestflow.BuildConfig
import com.chiforest.forestflow.auth.AuthRepository
import com.chiforest.forestflow.auth.LocalAuthRepository
import com.chiforest.forestflow.auth.NoRemote
import com.chiforest.forestflow.auth.PrefsTokenStore
import com.chiforest.forestflow.auth.RemoteStore
import com.chiforest.forestflow.auth.SupabaseApi
import com.chiforest.forestflow.auth.SupabaseBackend

/** Manual DI. One instance per process. */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    val mapData: MapData = MapData.load(app)

    /** On-device SQLite: offline cache and the whole app when no Supabase project is configured. */
    val db = ForestDatabase(app)
    val localAuth = LocalAuthRepository(db)

    /** Supabase (accounts + cloud copy of each account's data) when a project URL and key were built in. */
    val supabase: SupabaseBackend? =
        if (BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            SupabaseBackend(SupabaseApi(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY), PrefsTokenStore(app))
        } else {
            null
        }

    val auth: AuthRepository = supabase ?: localAuth
    val remote: RemoteStore = supabase ?: NoRemote
}
