package ir.coffevista.vista_native.core.network

import android.net.http.X509TrustManagerExtensions
import ir.coffevista.vista_native.core.common.EpochClock
import java.net.Socket
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager
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

    fun hydrateFromDisk(savedPolicy: TlsPolicy) {
        val normalized = savedPolicy.normalized()
        policy.updateAndGet { current ->
            if (current.revision == 0L && normalized.revision > 0L) normalized else current
        }
    }

    fun update(candidate: TlsPolicy, signatureVerified: Boolean): TlsPolicyUpdate {
        val retained = policy.get()
        if (
            signatureVerified &&
            candidate.contractVersion == SUPPORTED_CONTRACT_VERSION &&
            retained.revision > 0L &&
            candidate.mode == retained.mode &&
            candidate.currentPinSha256.normalizedPin() == retained.currentPinSha256.normalizedPin() &&
            candidate.nextPinSha256.normalizedPin() == retained.nextPinSha256.normalizedPin() &&
            candidate.expiresAtEpochSeconds == retained.expiresAtEpochSeconds &&
            candidate.revision == retained.revision
        ) {
            return TlsPolicyUpdate.Accepted(retained)
        }
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
) : X509ExtendedTrustManager() {
    private val trustManagerExtensions: X509TrustManagerExtensions? =
        runCatching { X509TrustManagerExtensions(delegate) }.getOrNull()

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        delegate.checkClientTrusted(chain, authType)
    }

    override fun checkClientTrusted(
        chain: Array<out X509Certificate>?,
        authType: String?,
        socket: Socket?,
    ) {
        if (delegate is X509ExtendedTrustManager) {
            delegate.checkClientTrusted(chain, authType, socket)
        } else {
            delegate.checkClientTrusted(chain, authType)
        }
    }

    override fun checkClientTrusted(
        chain: Array<out X509Certificate>?,
        authType: String?,
        engine: SSLEngine?,
    ) {
        if (delegate is X509ExtendedTrustManager) {
            delegate.checkClientTrusted(chain, authType, engine)
        } else {
            delegate.checkClientTrusted(chain, authType)
        }
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        verifyServerTrusted(chain, authType, host)
    }

    override fun checkServerTrusted(
        chain: Array<out X509Certificate>?,
        authType: String?,
        socket: Socket?,
    ) {
        val peerHost = socket?.inetAddress?.hostName?.takeIf(String::isNotBlank) ?: host
        verifyServerTrusted(chain, authType, peerHost)
    }

    override fun checkServerTrusted(
        chain: Array<out X509Certificate>?,
        authType: String?,
        engine: SSLEngine?,
    ) {
        val peerHost = engine?.peerHost?.takeIf(String::isNotBlank) ?: host
        verifyServerTrusted(chain, authType, peerHost)
    }

    fun checkServerTrusted(
        chain: Array<out X509Certificate>?,
        authType: String?,
        hostName: String?,
    ): List<X509Certificate> {
        val targetHost = hostName?.takeIf(String::isNotBlank) ?: host
        return verifyServerTrusted(chain, authType, targetHost)
    }

    private fun verifyServerTrusted(
        chain: Array<out X509Certificate>?,
        authType: String?,
        targetHost: String,
    ): List<X509Certificate> {
        val certs = chain?.map { it }?.toTypedArray()
            ?: throw CertificateException("Missing server certificate")
        val verifiedChain = try {
            val ext = trustManagerExtensions
            if (ext != null) {
                ext.checkServerTrusted(certs, authType, targetHost)
            } else {
                delegate.checkServerTrusted(certs, authType)
                certs.toList()
            }
        } catch (e: RuntimeException) {
            if (e is CertificateException) throw e
            delegate.checkServerTrusted(certs, authType)
            certs.toList()
        }
        val leaf = verifiedChain.firstOrNull() ?: certs.firstOrNull()
            ?: throw CertificateException("Missing server certificate")
        val certSha256 = sha256Hex(leaf.encoded)
        val spkiSha256 = runCatching { sha256Hex(leaf.publicKey.encoded) }.getOrNull()
        val policy = policyStore.current()
        val validPins = policy.validPins()
        val matchedFingerprint = when {
            certSha256 in validPins -> certSha256
            spkiSha256 != null && spkiSha256 in validPins -> spkiSha256
            else -> certSha256
        }
        val decision = evaluator.evaluate(policy, matchedFingerprint)
        decision.report?.let { reason ->
            reporter.report(TlsFailureReport(targetHost, certSha256, reason))
        }
        if (!decision.allow) throw CertificateException("TLS policy rejected certificate")
        return verifiedChain
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }

    override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers
}

internal fun TlsPolicy.validPins(): Set<String> =
    setOfNotNull(currentPinSha256.normalizedPin(), nextPinSha256.normalizedPin())

internal fun String?.normalizedPin(): String? = this
    ?.replace(":", "")
    ?.trim()
    ?.lowercase()
    ?.takeIf { pin -> pin.length == 64 && pin.all { it in '0'..'9' || it in 'a'..'f' } }
