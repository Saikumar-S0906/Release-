package com.pixijoy.ai.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkStatus(
    val isOnline: Boolean = true,
    val isSlow: Boolean = false
)

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val status: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(readStatus())
            }

            override fun onLost(network: Network) {
                trySend(readStatus())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(readStatus(capabilities))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        trySend(readStatus())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    /** @deprecated Use [status] */
    val isOnline: Flow<Boolean> = status.map { it.isOnline }

    fun isCurrentlyOnline(): Boolean = readStatus().isOnline

    private fun readStatus(capabilities: NetworkCapabilities? = null): NetworkStatus {
        val network = connectivityManager.activeNetwork
        val caps = capabilities
            ?: network?.let { connectivityManager.getNetworkCapabilities(it) }
        val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val slow = if (caps == null || !online) {
            false
        } else {
            val downKbps = caps.linkDownstreamBandwidthKbps
            downKbps in 1 until SLOW_NETWORK_KBPS
        }
        return NetworkStatus(isOnline = online, isSlow = slow)
    }

    companion object {
        /** Below 1 Mbps downstream treated as slow for cloud AI UX */
        private const val SLOW_NETWORK_KBPS = 1000
    }
}
