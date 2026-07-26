package ir.coffevista.vista_native.core.worker

import java.security.MessageDigest
import java.util.UUID
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

sealed interface RetryDecision {
    data class RetryAfter(val delay: Duration) : RetryDecision
    data object Exhausted : RetryDecision
}

object FoundationWorkRetryPolicy {
    const val MAX_ATTEMPTS = 5
    val BASE_DELAY: Duration = 30.seconds
    val MAX_DELAY: Duration = 5.hours

    fun decision(
        runAttemptCount: Int,
        idempotencyKey: UUID,
    ): RetryDecision {
        require(runAttemptCount >= 0) { "Attempt count must not be negative" }
        if (runAttemptCount >= MAX_ATTEMPTS - 1) return RetryDecision.Exhausted

        val exponentialSeconds = (
            BASE_DELAY.inWholeSeconds * 2.0.pow(runAttemptCount.toDouble())
            ).toLong().coerceAtMost(MAX_DELAY.inWholeSeconds)
        val jitterPercent = deterministicJitterPercent(idempotencyKey, runAttemptCount)
        val jitteredSeconds = (
            exponentialSeconds * (100L + jitterPercent) / 100L
            ).coerceIn(BASE_DELAY.inWholeSeconds, MAX_DELAY.inWholeSeconds)
        return RetryDecision.RetryAfter(jitteredSeconds.seconds)
    }

    private fun deterministicJitterPercent(
        idempotencyKey: UUID,
        runAttemptCount: Int,
    ): Int {
        val digest = MessageDigest.getInstance("SHA-256").digest(
            "$idempotencyKey:$runAttemptCount".toByteArray(Charsets.UTF_8),
        )
        return (digest.first().toInt() and 0xFF) % 41 - 20
    }
}
