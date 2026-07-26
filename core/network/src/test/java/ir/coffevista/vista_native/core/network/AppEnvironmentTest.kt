package ir.coffevista.vista_native.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppEnvironmentTest {
    @Test
    fun keepsBetaAndProductionAsExplicitEnvironmentNames() {
        val beta = AppEnvironment(
            name = EnvironmentName.BETA,
            internalBaseUrl = "https://api-beta.example.test/",
            debugBuild = true,
        )
        val production = AppEnvironment(
            name = EnvironmentName.PRODUCTION,
            internalBaseUrl = "https://api.example.test/",
            debugBuild = false,
        )

        assertEquals(EnvironmentName.BETA, beta.name)
        assertEquals(EnvironmentName.PRODUCTION, production.name)
    }

    @Test
    fun rejectsCleartextAndCredentialBearingOverrides() {
        assertThrows(IllegalArgumentException::class.java) {
            AppEnvironment(
                name = EnvironmentName.BETA,
                internalBaseUrl = "http://api-beta.example.test/",
                debugBuild = true,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            AppEnvironment(
                name = EnvironmentName.PRODUCTION,
                internalBaseUrl = "https://user:password@example.test/",
                debugBuild = false,
            )
        }
    }
}
