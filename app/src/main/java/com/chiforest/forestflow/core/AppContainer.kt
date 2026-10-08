package com.chiforest.forestflow.core

import android.content.Context
import com.chiforest.forestflow.auth.AuthRepository
import com.chiforest.forestflow.auth.FirebaseAuthRepository
import com.chiforest.forestflow.auth.LocalAuthRepository
import com.google.firebase.FirebaseApp

/** Manual DI. One instance per process. */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    val mapData: MapData = MapData.load(app)
    val db = ForestDatabase(app)
    val localAuth = LocalAuthRepository(db)

    /** Firebase only when google-services.json produced default options; otherwise fully offline. */
    val auth: AuthRepository = runCatching {
        if (FirebaseApp.getApps(app).isNotEmpty() || FirebaseApp.initializeApp(app) != null) FirebaseAuthRepository() else null
    }.getOrNull() ?: localAuth
}
