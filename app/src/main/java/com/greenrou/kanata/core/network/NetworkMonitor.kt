package com.greenrou.kanata.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    val isConnected: Flow<Boolean> = callbackFlow {
        val validatedNetworks = mutableSetOf<Network>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                validatedNetworks += network
                trySend(true)
            }

            override fun onLost(network: Network) {
                validatedNetworks -= network
                trySend(validatedNetworks.isNotEmpty())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (validated) validatedNetworks += network else validatedNetworks -= network
                trySend(validatedNetworks.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        trySend(isConnectedNow())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
            validatedNetworks.clear()
        }
    }.distinctUntilChanged()

    fun isConnectedNow(): Boolean =
        connectivityManager.activeNetwork?.let { network ->
            connectivityManager.getNetworkCapabilities(network)?.let { caps ->
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        } ?: false
}
