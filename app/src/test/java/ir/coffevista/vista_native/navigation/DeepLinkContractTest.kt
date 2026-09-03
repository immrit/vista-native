package ir.coffevista.vista_native.navigation

import ir.coffevista.vista_native.core.common.EpochClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkContractTest {
    @Test
    fun canonicalContractsParseToTypedPendingDestinations() {
        val cases = mapOf(
            "vista://post/post_42" to DeferredFeatureKind.POST,
            "vista://profile/user-42" to DeferredFeatureKind.PROFILE,
            "vista://group/invite_42" to DeferredFeatureKind.GROUP,
            "vista://chat/conversation-42" to DeferredFeatureKind.CHAT,
            "https://cafevista.ir/post/post_42" to DeferredFeatureKind.POST,
            "https://vista.me/profile/user-42" to DeferredFeatureKind.PROFILE,
            "https://www.cafevista.ir/group/invite_42" to DeferredFeatureKind.GROUP,
        )

        cases.forEach { (uri, expectedKind) ->
            val parsed = DeepLinkParser.parse(uri) as DeepLinkParseResult.Supported
            assertEquals(expectedKind, parsed.destination.kind)
        }
    }

    @Test
    fun invalidAndUnsupportedUrisHaveControlledFailureReasons() {
        val invalid = listOf(
            "vista://post",
            "vista://chat/",
            "vista://profile/a/b",
            "vista://group/unsafe%2Fvalue",
        )
        invalid.forEach { uri ->
            assertEquals(
                DeepLinkParseResult.Rejected(DeepLinkFailureReason.INVALID_ARGUMENT),
                DeepLinkParser.parse(uri),
            )
        }

        val unsupported = listOf(
            "vista://chat-detail/42",
            "javascript://post/42",
            "vista://post/42?token=secret",
            "vista://unknown/42",
            "https://cafevista.ir/feed",
            "https://coffevista.ir/post/42",
        )
        unsupported.forEach { uri ->
            assertEquals(
                DeepLinkParseResult.Rejected(DeepLinkFailureReason.UNSUPPORTED),
                DeepLinkParser.parse(uri),
            )
        }
    }

    @Test
    fun loggedOutColdLinkWaitsAndReplaysExactlyOnceAfterAuthentication() {
        val clock = MutableEpochClock()
        val coordinator = DeepLinkCoordinator(clock)

        coordinator.submit("vista://post/post_42", authenticated = false)
        val held = coordinator.state.value as DeepLinkDeliveryState.PendingSession
        coordinator.onSessionResolved(authenticated = false)
        val pending = coordinator.state.value as DeepLinkDeliveryState.PendingAuthentication
        assertEquals(held.id, pending.id)
        coordinator.submit("vista://post/post_42", authenticated = false)
        assertEquals(pending.id, coordinator.state.value.deliveryId())

        coordinator.onAuthenticationChanged(authenticated = true)
        val ready = coordinator.state.value as DeepLinkDeliveryState.Ready
        assertEquals(pending.id, ready.id)
        assertEquals(DeferredFeatureKind.POST, ready.destination.kind)

        coordinator.consume(ready.id)
        assertEquals(DeepLinkDeliveryState.Idle, coordinator.state.value)
        coordinator.submit("vista://post/post_42", authenticated = true)
        assertEquals(DeepLinkDeliveryState.Idle, coordinator.state.value)
    }

    @Test
    fun warmAuthenticatedLinkIsReadyAndCanBeDeliveredAgainAfterDedupWindow() {
        val clock = MutableEpochClock()
        val coordinator = DeepLinkCoordinator(clock)

        coordinator.onSessionResolved(authenticated = true)
        coordinator.submit("vista://chat/conversation-42", authenticated = true)
        val first = coordinator.state.value as DeepLinkDeliveryState.Ready
        coordinator.consume(first.id)
        clock.now += 3
        coordinator.submit("vista://chat/conversation-42", authenticated = true)

        val second = coordinator.state.value as DeepLinkDeliveryState.Ready
        assertTrue(second.id > first.id)
    }

    @Test
    fun unsupportedLinkBecomesSingleControlledFailureEffect() {
        val coordinator = DeepLinkCoordinator(MutableEpochClock())

        coordinator.submit("vista://chat-detail/42", authenticated = true)
        val held = coordinator.state.value as DeepLinkDeliveryState.PendingFailure
        coordinator.onSessionResolved(authenticated = true)
        val failure = coordinator.state.value as DeepLinkDeliveryState.Failure
        assertEquals(held.id, failure.id)
        coordinator.submit("vista://chat-detail/42", authenticated = true)

        assertEquals(failure.id, coordinator.state.value.deliveryId())
        assertEquals(DeepLinkFailureReason.UNSUPPORTED, failure.reason)
    }
}

private class MutableEpochClock(
    var now: Long = 1_800_000_000L,
) : EpochClock {
    override fun nowEpochSeconds(): Long = now
}

private fun DeepLinkDeliveryState.deliveryId(): Long = when (this) {
    is DeepLinkDeliveryState.PendingSession -> id
    is DeepLinkDeliveryState.PendingFailure -> id
    is DeepLinkDeliveryState.PendingAuthentication -> id
    is DeepLinkDeliveryState.Ready -> id
    is DeepLinkDeliveryState.Failure -> id
    DeepLinkDeliveryState.Idle -> error("Idle has no delivery id")
}
