package ir.coffevista.vista_native.core.network

import ir.coffevista.vista_native.core.common.ErrorKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

class ErrorClassifierTest {
    @Test
    fun mapsHttpCategories() {
        assertEquals(
            ErrorKind.FORBIDDEN,
            ErrorClassifier.classify(RemoteFailure(403, "forbidden", null), "test").kind,
        )
        assertEquals(
            ErrorKind.CONFLICT,
            ErrorClassifier.classify(RemoteFailure(409, "conflict", null), "test").kind,
        )
        assertEquals(
            ErrorKind.SERVER,
            ErrorClassifier.classify(RemoteFailure(503, "server", null), "test").kind,
        )
    }

    @Test
    fun mapsTimeoutAndTlsAndKeepsOnlyCauseType() {
        val timeout = ErrorClassifier.classify(SocketTimeoutException("secret"), "test")
        val tls = ErrorClassifier.classify(SSLHandshakeException("secret"), "test")

        assertEquals(ErrorKind.TIMEOUT, timeout.kind)
        assertEquals(ErrorKind.TLS, tls.kind)
        assertNotNull(timeout.causeType)
        assertNotNull(tls.causeType)
    }

    @Test
    fun mapsBannedAndFeatureDisabledByFeature() {
        val banned = ErrorClassifier.classify(
            RemoteFailure(statusCode = 403, code = "banned", message = "banned"),
            "گفتگو",
        )
        assertEquals(ErrorKind.ACCOUNT_DISABLED, banned.kind)
        assertEquals("banned", banned.code)

        val chatDisabled = ErrorClassifier.classify(
            RemoteFailure(
                statusCode = 403,
                code = "FEATURE_DISABLED",
                message = "This feature is temporarily disabled by the control plane.",
                feature = "chat",
            ),
            "گفتگو",
        )
        assertEquals(ErrorKind.FORBIDDEN, chatDisabled.kind)
        assertEquals("بخش گفتگو موقتاً غیرفعال شده است", chatDisabled.messageFa)
    }
}
