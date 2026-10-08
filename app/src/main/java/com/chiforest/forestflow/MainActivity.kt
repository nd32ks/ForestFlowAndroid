package com.chiforest.forestflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.chiforest.forestflow.core.AppContainer
import com.chiforest.forestflow.core.AppViewModel
import com.chiforest.forestflow.ui.theme.ForestTheme

class MainActivity : ComponentActivity() {
    internal val vm: AppViewModel by viewModels { AppViewModel.Factory(container(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ForestTheme { AppRoot(vm) }
        }
    }

    companion object {
        // Holds only the application context, never an Activity.
        @android.annotation.SuppressLint("StaticFieldLeak")
        @Volatile private var instance: AppContainer? = null

        /** Test hook: drop the process-wide container so the next launch reopens a fresh database. */
        @androidx.annotation.VisibleForTesting
        fun resetContainer() = synchronized(this) {
            instance?.db?.close()
            instance = null
        }
        fun container(activity: ComponentActivity): AppContainer =
            instance ?: synchronized(this) { instance ?: AppContainer(activity.applicationContext).also { instance = it } }
    }
}
