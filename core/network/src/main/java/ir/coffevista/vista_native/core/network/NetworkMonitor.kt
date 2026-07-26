package ir.coffevista.vista_native.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.coffevista.vista_native.core.common.ApplicationScope
import ir.coffevista.vista_native.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

enum class NetworkState {
    ONLINE,
    OFFLINE,
}

interface NetworkMonitor {
    val state: StateFlow<NetworkState>
    fun currentState(): NetworkState
}

object AssumeOnlineNetworkMonitor : NetworkMonitor {
    private val online = kotlinx.coroutines.flow.MutableStateFlow(NetworkState.ONLINE)
    override val state: StateFlow<NetworkState> = online
    override fun currentState(): NetworkState = NetworkState.ONLINE
}

@Singleton
class AndroidNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope scope: CoroutineScope,
    dispatchers: DispatcherProvider,
) : NetworkMonitor {
    private val connectivity =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val state: StateFlow<NetworkState> = callbackFlow {
        trySend(currentState())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentState())
            }

            override fun onLost(network: Network) {
                trySend(currentState())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(currentState())
            }
        }
        connectivity.registerDefaultNetworkCallback(callback)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }
        .flowOn(dispatchers.io)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = currentState(),
        )

    override fun currentState(): NetworkState {
        val network = connectivity.activeNetwork ?: return NetworkState.OFFLINE
        val capabilities = connectivity.getNetworkCapabilities(network)
            ?: return NetworkState.OFFLINE
        return if (
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ) {
            NetworkState.ONLINE
        } else {
            NetworkState.OFFLINE
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkMonitorBindingsModule {
    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(
        implementation: AndroidNetworkMonitor,
    ): NetworkMonitor
}
