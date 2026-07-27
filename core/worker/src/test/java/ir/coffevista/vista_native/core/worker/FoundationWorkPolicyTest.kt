package ir.coffevista.vista_native.core.worker

import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class FoundationWorkPolicyTest {
    @Test
    fun identityIsDeterministicScopedAndDoesNotExposeAccountId() {
        val id = WorkIdentity(
            accountId = "sensitive-account-id",
            operation = "foundation_probe",
            operationId = OPERATION_ID,
        )

        assertEquals(id.uniqueName, id.copy().uniqueName)
        assertTrue(id.uniqueName.contains("foundation_probe"))
        assertTrue(id.accountTag.startsWith("vista-work-v1:account:"))
        assertFalse(id.uniqueName.contains("sensitive-account-id"))
        assertFalse(id.accountTag.contains("sensitive-account-id"))
    }

    @Test
    fun retryUsesDeterministicBoundedExponentialBackoffWithJitter() {
        val delays = (0 until FoundationWorkRetryPolicy.MAX_ATTEMPTS - 1)
            .map { attempt ->
                FoundationWorkRetryPolicy.decision(
                    runAttemptCount = attempt,
                    idempotencyKey = OPERATION_ID,
                ) as RetryDecision.RetryAfter
            }
            .map(RetryDecision.RetryAfter::delay)

        assertEquals(
            FoundationWorkRetryPolicy.decision(0, OPERATION_ID),
            FoundationWorkRetryPolicy.decision(0, OPERATION_ID),
        )
        assertTrue(delays.first() >= 30.seconds)
        assertTrue(delays.zipWithNext().all { (first, second) -> second > first })
        assertEquals(
            RetryDecision.Exhausted,
            FoundationWorkRetryPolicy.decision(
                FoundationWorkRetryPolicy.MAX_ATTEMPTS - 1,
                OPERATION_ID,
            ),
        )
    }

    @Test
    fun defaultConstraintsRequireOnlyNetworkAndStorage() {
        val constraints = WorkRequirements().toConstraints()

        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
        assertTrue(constraints.requiresStorageNotLow())
        assertFalse(constraints.requiresCharging())
        assertFalse(constraints.requiresDeviceIdle())
    }

    private companion object {
        val OPERATION_ID: UUID =
            UUID.fromString("f8a39560-09e3-48f0-90e2-9ff16e1e14b7")
    }
}
