package ir.coffevista.vista_native.navigation

import ir.coffevista.vista_native.core.common.EpochClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class PendingDestination(
    val kind: DeferredFeatureKind,
    val reference: String,
) {
    val deliveryKey: String
        get() = "${kind.name}:$reference"
}

sealed interface DeepLinkParseResult {
    data class Supported(val destination: PendingDestination) : DeepLinkParseResult
    data class Rejected(val reason: DeepLinkFailureReason) : DeepLinkParseResult
}

object DeepLinkParser {
    fun parse(rawUri: String): DeepLinkParseResult {
        val uri = runCatching { URI(rawUri) }.getOrNull()
            ?: return DeepLinkParseResult.Rejected(DeepLinkFailureReason.INVALID_ARGUMENT)
        if (
            uri.scheme?.lowercase() != SCHEME ||
            uri.userInfo != null ||
            uri.port != -1 ||
            uri.query != null ||
            uri.fragment != null
        ) {
            return DeepLinkParseResult.Rejected(DeepLinkFailureReason.UNSUPPORTED)
        }
        val kind = when (uri.host?.lowercase()) {
            "post" -> DeferredFeatureKind.POST
            "profile" -> DeferredFeatureKind.PROFILE
            "group" -> DeferredFeatureKind.GROUP
            "chat" -> DeferredFeatureKind.CHAT
            else -> return DeepLinkParseResult.Rejected(DeepLinkFailureReason.UNSUPPORTED)
        }
        val segments = uri.path.orEmpty()
            .split('/')
            .filter(String::isNotBlank)
        if (segments.size != 1 || !REFERENCE_PATTERN.matches(segments.single())) {
            return DeepLinkParseResult.Rejected(DeepLinkFailureReason.INVALID_ARGUMENT)
        }
        return DeepLinkParseResult.Supported(
            PendingDestination(
                kind = kind,
                reference = segments.single(),
            ),
        )
    }

    private val REFERENCE_PATTERN = Regex("[A-Za-z0-9_-]{1,128}")
    private const val SCHEME = "vista"
}

sealed interface DeepLinkDeliveryState {
    data object Idle : DeepLinkDeliveryState

    data class PendingSession(
        val id: Long,
        val destination: PendingDestination,
    ) : DeepLinkDeliveryState

    data class PendingFailure(
        val id: Long,
        val reason: DeepLinkFailureReason,
        val deliveryKey: String,
    ) : DeepLinkDeliveryState

    data class PendingAuthentication(
        val id: Long,
        val destination: PendingDestination,
    ) : DeepLinkDeliveryState

    data class Ready(
        val id: Long,
        val destination: PendingDestination,
    ) : DeepLinkDeliveryState

    data class Failure(
        val id: Long,
        val reason: DeepLinkFailureReason,
        val deliveryKey: String,
    ) : DeepLinkDeliveryState
}

@Singleton
class DeepLinkCoordinator @Inject constructor(
    private val clock: EpochClock,
) {
    private val mutableState = MutableStateFlow<DeepLinkDeliveryState>(
        DeepLinkDeliveryState.Idle,
    )
    val state: StateFlow<DeepLinkDeliveryState> = mutableState.asStateFlow()

    private var nextDeliveryId = 1L
    private var lastConsumedKey: String? = null
    private var lastConsumedAtEpochSeconds = Long.MIN_VALUE
    private var sessionResolved = false

    @Synchronized
    fun submit(
        rawUri: String?,
        authenticated: Boolean,
    ) {
        if (rawUri.isNullOrBlank()) return
        when (val parsed = DeepLinkParser.parse(rawUri)) {
            is DeepLinkParseResult.Rejected -> {
                val key = failureKey(parsed.reason, rawUri)
                if (isDuplicate(key)) return
                val id = nextDeliveryId++
                mutableState.value = if (sessionResolved) {
                    DeepLinkDeliveryState.Failure(
                        id = id,
                        reason = parsed.reason,
                        deliveryKey = key,
                    )
                } else {
                    DeepLinkDeliveryState.PendingFailure(
                        id = id,
                        reason = parsed.reason,
                        deliveryKey = key,
                    )
                }
            }
            is DeepLinkParseResult.Supported -> {
                val destination = parsed.destination
                if (isDuplicate(destination.deliveryKey)) return
                val id = nextDeliveryId++
                mutableState.value = if (!sessionResolved) {
                    DeepLinkDeliveryState.PendingSession(
                        id = id,
                        destination = destination,
                    )
                } else if (authenticated) {
                    DeepLinkDeliveryState.Ready(id, destination)
                } else {
                    DeepLinkDeliveryState.PendingAuthentication(id, destination)
                }
            }
        }
    }

    @Synchronized
    fun onSessionResolved(authenticated: Boolean) {
        sessionResolved = true
        mutableState.value = when (val pending = mutableState.value) {
            is DeepLinkDeliveryState.PendingSession -> {
                if (authenticated) {
                    DeepLinkDeliveryState.Ready(pending.id, pending.destination)
                } else {
                    DeepLinkDeliveryState.PendingAuthentication(pending.id, pending.destination)
                }
            }
            is DeepLinkDeliveryState.PendingFailure -> {
                DeepLinkDeliveryState.Failure(
                    id = pending.id,
                    reason = pending.reason,
                    deliveryKey = pending.deliveryKey,
                )
            }
            else -> pending
        }
    }

    @Synchronized
    fun onAuthenticationChanged(authenticated: Boolean) {
        sessionResolved = true
        val pending = mutableState.value as? DeepLinkDeliveryState.PendingAuthentication
            ?: return
        if (authenticated) {
            mutableState.value = DeepLinkDeliveryState.Ready(
                id = pending.id,
                destination = pending.destination,
            )
        }
    }

    @Synchronized
    fun consume(id: Long) {
        val current = mutableState.value
        val key = when {
            current is DeepLinkDeliveryState.Ready && current.id == id ->
                current.destination.deliveryKey
            current is DeepLinkDeliveryState.Failure && current.id == id ->
                current.deliveryKey
            else -> return
        }
        lastConsumedKey = key
        lastConsumedAtEpochSeconds = clock.nowEpochSeconds()
        mutableState.value = DeepLinkDeliveryState.Idle
    }

    @androidx.annotation.VisibleForTesting
    @Synchronized
    fun resetForTesting() {
        mutableState.value = DeepLinkDeliveryState.Idle
        lastConsumedKey = null
        lastConsumedAtEpochSeconds = Long.MIN_VALUE
        sessionResolved = false
    }

    private fun isDuplicate(key: String): Boolean {
        val currentKey = when (val current = mutableState.value) {
            is DeepLinkDeliveryState.PendingSession -> current.destination.deliveryKey
            is DeepLinkDeliveryState.PendingFailure -> current.deliveryKey
            is DeepLinkDeliveryState.PendingAuthentication -> current.destination.deliveryKey
            is DeepLinkDeliveryState.Ready -> current.destination.deliveryKey
            is DeepLinkDeliveryState.Failure -> current.deliveryKey
            DeepLinkDeliveryState.Idle -> null
        }
        if (currentKey == key) return true
        return lastConsumedKey == key &&
            clock.nowEpochSeconds() - lastConsumedAtEpochSeconds <= DUPLICATE_WINDOW_SECONDS
    }

    private fun failureKey(
        reason: DeepLinkFailureReason,
        rawUri: String,
    ): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(rawUri.toByteArray(Charsets.UTF_8))
            .take(12)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }
        return "failure:${reason.name}:$hash"
    }

    private companion object {
        const val DUPLICATE_WINDOW_SECONDS = 2
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface DeepLinkCoordinatorTestEntryPoint {
    fun deepLinkCoordinator(): DeepLinkCoordinator
}
