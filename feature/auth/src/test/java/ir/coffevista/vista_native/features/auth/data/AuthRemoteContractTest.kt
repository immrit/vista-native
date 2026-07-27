package ir.coffevista.vista_native.features.auth.data

import ir.coffevista.vista_native.core.network.AppEnvironment
import ir.coffevista.vista_native.core.network.BootstrapStatusClient
import ir.coffevista.vista_native.core.network.EnvironmentName
import ir.coffevista.vista_native.core.network.InternalNetworkModule
import ir.coffevista.vista_native.core.network.TlsPolicyStore
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class AuthRemoteContractTest {
    @Test
    fun loginRequestAndSuccessfulEnvelopeMatchTheBackendContract() = runBlocking {
        val observedRequest = AtomicReference<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                observedRequest.set(request)
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(SUCCESS_BODY.toResponseBody(JSON))
                    .build()
            }
            .build()
        val environment = AppEnvironment(
            name = EnvironmentName.BETA,
            internalBaseUrl = "https://contract.invalid/",
            debugBuild = true,
        )
        val retrofit = InternalNetworkModule.provideInternalRetrofit(client, environment)
        val source = OkHttpAuthRemoteDataSource(
            retrofit = retrofit,
            bootstrapStatusClient = BootstrapStatusClient(
                client = client,
                environment = environment,
                tlsPolicyStore = TlsPolicyStore(),
            ),
        )

        val payload = source.login("vista", "password")

        assertEquals("user-42", payload.user.id)
        assertEquals("vista", payload.user.username)
        assertEquals("کاربر ویستا", payload.user.fullName)
        assertEquals(1_900_000_000L, payload.session.expiresAtEpochSeconds)
        assertFalse(payload.isNewUser)
        val request = observedRequest.get()
        assertEquals("POST", request.method)
        assertEquals("/v1/auth/login", request.url.encodedPath)
        val requestBuffer = Buffer()
        request.body?.writeTo(requestBuffer)
        val requestJson = requestBuffer.readUtf8()
        assertTrue(requestJson.contains("\"identifier\":\"vista\""))
        assertTrue(requestJson.contains("\"password\":\"password\""))
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        const val SUCCESS_BODY = """
            {
              "user": {
                "id": "user-42",
                "phone_number": "09123456789",
                "profile_completed": true,
                "has_password": true,
                "password_required": false,
                "account_status": "active",
                "username": "vista",
                "full_name": "کاربر ویستا"
              },
              "session": {
                "access_token": "e30.eyJleHAiOjE5MDAwMDAwMDB9.sig",
                "refresh_token": "refresh"
              },
              "is_new_user": false
            }
        """
    }
}
