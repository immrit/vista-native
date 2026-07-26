package ir.coffevista.vista_native.core.network

import ir.coffevista.vista_native.core.common.EpochClock
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.net.ssl.X509TrustManager

enum class TlsMode {
    OFF,
    MONITOR,
    ENFORCE,
}

data class TlsPolicy(
    val contractVersion: Int,
    val revision: Long,
    val mode: TlsMode,
    val currentPinSha256: String?,
    val nextPinSha256: String?,
    val expiresAtEpochSeconds: Long?,
)

enum class TlsPolicyRejection {
    UNSIGNED,
    INVALID_VERSION,
    STALE_REVISION,
    INVALID_PIN,
    PERSISTENCE_FAILURE,
}

sealed interface TlsPolicyUpdate {
    data class Accepted(val policy: TlsPolicy) : TlsPolicyUpdate
    data class Rejected(
        val reason: TlsPolicyRejection,
        val retainedPolicy: TlsPolicy,
    ) : TlsPolicyUpdate
}

enum class TlsFailureReason {
    MISSING_POLICY,
    EXPIRED_POLICY,
    PIN_MISMATCH,
}

data class TlsDecision(
    val allow: Boolean,
    val report: TlsFailureReason? = null,
)

class TlsPolicyStore(
    initialPolicy: TlsPolicy = DEFAULT_POLICY,
    private val persistVerifiedPolicy: (TlsPolicy) -> Boolean = { true },
) {
    private val policy = AtomicReference(initialPolicy)

    fun current(): TlsPolicy = policy.get()

    fun update(candidate: TlsPolicy, signatureVerified: Boolean): TlsPolicyUpdate {
        val retained = policy.get()
        val rejection = when {
            !signatureVerified -> TlsPolicyRejection.UNSIGNED
            candidate.contractVersion != SUPPORTED_CONTRACT_VERSION ->
                TlsPolicyRejection.INVALID_VERSION
            candidate.revision <= retained.revision -> TlsPolicyRejection.STALE_REVISION
            candidate.mode != TlsMode.OFF && candidate.validPins().isEmpty() ->
                TlsPolicyRejection.INVALID_PIN
            else -> null
        }
        if (rejection != null) return TlsPolicyUpdate.Rejected(rejection, retained)
        val normalized = candidate.normalized()
        if (!runCatching { persistVerifiedPolicy(normalized) }.getOrDefault(false)) {
            return TlsPolicyUpdate.Rejected(TlsPolicyRejection.PERSISTENCE_FAILURE, retained)
        }
        policy.set(normalized)
        return TlsPolicyUpdate.Accepted(policy.get())
    }

    private fun TlsPolicy.normalized(): TlsPolicy = copy(
        currentPinSha256 = currentPinSha256.normalizedPin(),
        nextPinSha256 = nextPinSha256.normalizedPin(),
    )

    companion object {
        const val SUPPORTED_CONTRACT_VERSION = 1
        val DEFAULT_POLICY = TlsPolicy(
            contractVersion = SUPPORTED_CONTRACT_VERSION,
            revision = 0,
            mode = TlsMode.OFF,
            currentPinSha256 = null,
            nextPinSha256 = null,
            expiresAtEpochSeconds = null,
        )
        val FAIL_CLOSED_POLICY = DEFAULT_POLICY.copy(mode = TlsMode.ENFORCE)
    }
}

class TlsPolicyEvaluator @Inject constructor(
    private val clock: EpochClock,
) {
    fun evaluate(policy: TlsPolicy, seenFingerprint: String): TlsDecision {
        if (policy.mode == TlsMode.OFF) return TlsDecision(allow = true)
        if (policy.validPins().isEmpty()) {
            return TlsDecision(
                allow = policy.mode == TlsMode.MONITOR,
                report = TlsFailureReason.MISSING_POLICY,
            )
        }
        val expiry = policy.expiresAtEpochSeconds
        if (expiry == null || expiry <= clock.nowEpochSeconds()) {
            return TlsDecision(
                allow = policy.mode == TlsMode.MONITOR,
                report = TlsFailureReason.EXPIRED_POLICY,
            )
        }
        if (seenFingerprint.normalizedPin() in policy.validPins()) {
            return TlsDecision(allow = true)
        }
        return TlsDecision(
            allow = policy.mode == TlsMode.MONITOR,
            report = TlsFailureReason.PIN_MISMATCH,
        )
    }
}

data class TlsFailureReport(
    val host: String,
    val seenFingerprint: String,
    val reason: TlsFailureReason,
)

fun interface TlsFailureReporter {
    fun report(failure: TlsFailureReport)
}

class PolicyTrustManager(
    private val delegate: X509TrustManager,
    private val host: String,
    private val policyStore: TlsPolicyStore,
    private val evaluator: TlsPolicyEvaluator,
    private val reporter: TlsFailureReporter,
) : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        delegate.checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        delegate.checkServerTrusted(chain, authType)
        val leaf = chain?.firstOrNull() ?: throw CertificateException("Missing server certificate")
        val seen = MessageDigest.getInstance("SHA-256")
            .digest(leaf.encoded)
            .joinToString("") { byte -> "%02x".format(byte) }
        val decision = evaluator.evaluate(policyStore.current(), seen)
        decision.report?.let { reason ->
            reporter.report(TlsFailureReport(host, seen, reason))
        }
        if (!decision.allow) throw CertificateException("TLS policy rejected certificate")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers
}

internal fun TlsPolicy.validPins(): Set<String> =
    setOfNotNull(currentPinSha256.normalizedPin(), nextPinSha256.normalizedPin())

internal fun String?.normalizedPin(): String? = this
    ?.replace(":", "")
    ?.trim()
    ?.lowercase()
    ?.takeIf { pin -> pin.length == 64 && pin.all { it in '0'..'9' || it in 'a'..'f' } }
