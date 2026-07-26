package ir.coffevista.vista_native.core.network

import ir.coffevista.vista_native.core.common.EpochClock
import ir.coffevista.vista_native.core.common.AccessTokenProvider
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import javax.net.ssl.SSLHandshakeException

class NetworkSecurityPolicyTest {
    @Test
    fun retryIsBoundedAndOnlyForIdempotentTransportFailures() {
        val policy = BoundedRetryPolicy(maxRetries = 1)

        assertTrue(policy.shouldRetry("GET", 0, IOException("network")))
        assertFalse(policy.shouldRetry("GET", 1, IOException("network")))
        assertFalse(policy.shouldRetry("POST", 0, IOException("network")))
        assertFalse(policy.shouldRetry("GET", 0, SSLHandshakeException("tls")))
    }

    @Test
    fun authorizationIsAttachedOnlyToTheExactInternalHost() {
        val environment = AppEnvironment(
            name = EnvironmentName.BETA,
            internalBaseUrl = "https://api.coffevista.ir/",
            debugBuild = true,
        )
        val interceptor = InternalAuthorizationInterceptor(
            environment,
            AccessTokenProvider { "access-secret" },
        )
        var captured: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                captured = chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("".toResponseBody())
                    .build()
            }
            .build()

        client.newCall(Request.Builder().url("https://api.coffevista.ir/v1/me").build())
            .execute()
            .close()
        assertEquals("Bearer access-secret", captured?.header("Authorization"))

        client.newCall(Request.Builder().url("https://api.coffevista.ir.evil.test/v1/me").build())
            .execute()
            .close()
        assertNull(captured?.header("Authorization"))
    }

    @Test
    fun externalUrlPolicyRejectsSchemeSubdomainCredentialsAndBadPath() {
        val policy = ExternalUrlPolicy(
            allowedHosts = setOf("media.coffevista.ir"),
            allowedPathPrefixes = setOf("/public/"),
        )

        assertEquals(
            UrlDisposition.EXTERNAL_BROWSER,
            policy.evaluate("https://media.coffevista.ir/public/a.jpg").disposition,
        )
        assertEquals(
            UrlDisposition.REJECT,
            policy.evaluate("http://media.coffevista.ir/public/a.jpg").disposition,
        )
        assertEquals(
            UrlDisposition.REJECT,
            policy.evaluate("https://media.coffevista.ir.evil.test/public/a.jpg").disposition,
        )
        assertEquals(
            UrlDisposition.REJECT,
            policy.evaluate("https://user:pass@media.coffevista.ir/public/a.jpg").disposition,
        )
        assertEquals(
            UrlDisposition.REJECT,
            policy.evaluate("https://media.coffevista.ir/private/a.jpg").disposition,
        )
    }

    @Test
    fun externalDispositionNeverForwardsCredentialHeaders() {
        val policy = ExternalUrlPolicy(setOf("media.coffevista.ir"))

        val safe = policy.safeHeaders(
            mapOf(
                "Authorization" to "Bearer secret",
                "cookie" to "session=secret",
                "X-Api-Key" to "secret",
                "Accept" to "image/*",
            ),
        )

        assertEquals(mapOf("Accept" to "image/*"), safe)
    }

    @Test
    fun tlsModesHaveExplicitMissingExpiredMismatchAndRotationBehavior() {
        val evaluator = TlsPolicyEvaluator(EpochClock { NOW })
        val current = "a".repeat(64)
        val next = "b".repeat(64)

        assertTrue(evaluator.evaluate(policy(TlsMode.OFF), "c".repeat(64)).allow)

        val monitorMissing = evaluator.evaluate(policy(TlsMode.MONITOR), current)
        assertTrue(monitorMissing.allow)
        assertEquals(TlsFailureReason.MISSING_POLICY, monitorMissing.report)

        val enforceExpired = evaluator.evaluate(
            policy(TlsMode.ENFORCE, current, next, expiresAt = NOW - 1),
            current,
        )
        assertFalse(enforceExpired.allow)
        assertEquals(TlsFailureReason.EXPIRED_POLICY, enforceExpired.report)

        val enforceCurrent = evaluator.evaluate(
            policy(TlsMode.ENFORCE, current, next, expiresAt = NOW + 60),
            current,
        )
        val enforceNext = evaluator.evaluate(
            policy(TlsMode.ENFORCE, current, next, expiresAt = NOW + 60),
            next,
        )
        assertTrue(enforceCurrent.allow)
        assertTrue(enforceNext.allow)
        assertNull(enforceNext.report)

        val enforceMismatch = evaluator.evaluate(
            policy(TlsMode.ENFORCE, current, next, expiresAt = NOW + 60),
            "c".repeat(64),
        )
        assertFalse(enforceMismatch.allow)
        assertEquals(TlsFailureReason.PIN_MISMATCH, enforceMismatch.report)
    }

    @Test
    fun unsignedInvalidAndStalePoliciesRetainLastVerifiedPolicy() {
        val store = TlsPolicyStore()
        val verified = policy(
            mode = TlsMode.MONITOR,
            current = "a".repeat(64),
            expiresAt = NOW + 60,
            revision = 2,
        )
        assertTrue(store.update(verified, signatureVerified = true) is TlsPolicyUpdate.Accepted)

        val unsigned = verified.copy(revision = 3, mode = TlsMode.ENFORCE)
        val unsignedResult = store.update(unsigned, signatureVerified = false)
        assertTrue(unsignedResult is TlsPolicyUpdate.Rejected)
        assertEquals(verified, store.current())

        val stale = verified.copy(revision = 1)
        assertTrue(store.update(stale, signatureVerified = true) is TlsPolicyUpdate.Rejected)
        assertEquals(verified, store.current())
    }

    @Test
    fun verifiedPolicyRevisionRebuildsInternalClientExactlyOnce() {
        val store = TlsPolicyStore()
        val environment = AppEnvironment(
            name = EnvironmentName.BETA,
            internalBaseUrl = "https://api.coffevista.ir/",
            debugBuild = true,
        )
        val factory = PolicyAwareInternalCallFactory(
            environment = environment,
            policyStore = store,
            evaluator = TlsPolicyEvaluator(EpochClock { NOW }),
            reporter = TlsFailureReporter { },
            authorizationInterceptor = InternalAuthorizationInterceptor(
                environment,
                AccessTokenProvider { null },
            ),
        )

        assertEquals(1, factory.currentGeneration())
        store.update(
            policy(
                mode = TlsMode.MONITOR,
                current = "a".repeat(64),
                expiresAt = NOW + 60,
                revision = 2,
            ),
            signatureVerified = true,
        )

        assertEquals(2, factory.currentGeneration())
        assertEquals(2, factory.currentGeneration())
    }

    private fun policy(
        mode: TlsMode,
        current: String? = null,
        next: String? = null,
        expiresAt: Long? = null,
        revision: Long = 1,
    ) = TlsPolicy(
        contractVersion = TlsPolicyStore.SUPPORTED_CONTRACT_VERSION,
        revision = revision,
        mode = mode,
        currentPinSha256 = current,
        nextPinSha256 = next,
        expiresAtEpochSeconds = expiresAt,
    )

    private companion object {
        const val NOW = 1_800_000_000L
    }
}
