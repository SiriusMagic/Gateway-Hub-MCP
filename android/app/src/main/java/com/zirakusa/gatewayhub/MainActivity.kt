package com.zirakusa.gatewayhub

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zirakusa.gatewayhub.data.KeystoreSessionStore
import com.zirakusa.gatewayhub.ui.HomeScreen
import com.zirakusa.gatewayhub.ui.HubTheme
import com.zirakusa.gatewayhub.ui.HubViewModel
import com.zirakusa.gatewayhub.ui.LoginScreen

class MainActivity : ComponentActivity() {
    private val vm: HubViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HubViewModel(KeystoreSessionStore(applicationContext), BuildConfig.DEFAULT_HUB_URL) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Links and the owner token are on screen: no screenshots, no previews in Recents.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            HubTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                if (state.connected) HomeScreen(state, vm) else LoginScreen(state, vm::connect)
            }
        }
    }
}
