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
}
