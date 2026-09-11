package com.pixijoy.ai.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.pixijoy.ai.core.network.NetworkStatus
import com.pixijoy.ai.di.NetworkEntryPoint
import dagger.hilt.android.EntryPointAccessors

@Composable
fun rememberNetworkStatus(): NetworkStatus {
    val context = LocalContext.current.applicationContext
    val monitor = remember {
        EntryPointAccessors.fromApplication(context, NetworkEntryPoint::class.java)
            .networkMonitor()
    }
    return monitor.status.collectAsState(initial = NetworkStatus()).value
}
