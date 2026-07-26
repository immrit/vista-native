package ir.coffevista.vista_native.features.auth.data

import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.AuthSession
import ir.coffevista.vista_native.core.model.auth.AuthUser
import ir.coffevista.vista_native.core.model.auth.IdentifierLookup
import ir.coffevista.vista_native.core.model.auth.OtpChallenge
import ir.coffevista.vista_native.core.model.auth.OtpVerification
import ir.coffevista.vista_native.core.network.RemoteFailure
import ir.coffevista.vista_native.core.security.expiryFromJwt
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OkHttpAuthRemoteDataSource(
    private val client: OkHttpClient,
    baseUrl: String,
) : AuthRemoteDataSource {
    private val rootUrl = baseUrl.trimEnd('/')
    private val authUrl = "$rootUrl/v1/auth"

    override suspend fun lookupIdentifier(identifier: String): IdentifierLookup {
        val json = post("$authUrl/lookup", JSONObject().put("identifier", identifier))
        return IdentifierLookup(
            exists = json.optBoolean("exists", false),
            isPhone = json.optBoolean("is_phone", false),
            normalizedIdentifier = json.optionalString("normalized_identifier"),
            authFlow = json.optionalString("auth_flow"),
            accountStatus = json.optionalString("account_status"),
        )
    }

    override suspend fun login(identifier: String, password: String): AuthPayload {
        return parseAuthPayload(
            post(
                "$authUrl/login",
                JSONObject()
                    .put("identifier", identifier)
                    .put("password", password),
            ),
        )
    }

    override suspend fun sendOtp(phoneNumber: String): OtpChallenge {
        val json = post(
            "$authUrl/send-otp",
            JSONObject().put("phone_number", phoneNumber),
        )
        return OtpChallenge(
            expiresInSeconds = json.optInt("expires_in_seconds", 60).coerceAtLeast(1),
            debugCode = json.optionalString("debug_code"),
        )
    }

    override suspend fun verifyOtp(phoneNumber: String, code: String): OtpVerification {
        val json = post(
            "$authUrl/verify-otp",
            JSONObject()
                .put("phone_number", phoneNumber)
                .put("code", code),
        )
        if (json.optBoolean("is_2fa_required", false)) {
            val token = json.optionalString("two_factor_token")
                ?: throw org.json.JSONException("two_factor_token missing")
            return OtpVerification.PasswordChallenge(token)
        }
        return OtpVerification.Authenticated(parseAuthPayload(json.getJSONObject("auth")))
    }

    override suspend fun verifyTwoFactor(
        twoFactorToken: String,
        password: String,
    ): AuthPayload {
        return parseAuthPayload(
            post(
                "$authUrl/2fa/verify",
                JSONObject()
                    .put("two_factor_token", twoFactorToken)
                    .put("password", password),
            ),
        )
    }

    override suspend fun setPassword(accessToken: String, password: String) {
        post(
            "$authUrl/2fa/setup",
            JSONObject().put("password", password),
            accessToken = accessToken,
        )
    }

    override suspend fun refresh(refreshToken: String): AuthPayload {
        return parseAuthPayload(
            post(
                "$authUrl/refresh",
                JSONObject().put("refresh_token", refreshToken),
            ),
        )
    }

    override suspend fun isMaintenanceMode(): Boolean {
        val request = Request.Builder()
            .url("$rootUrl/api/v1/system/status")
            .get()
            .build()
        val json = execute(request)
        val root = json.optJSONObject("data") ?: json
        return root.optBoolean("maintenance", false) ||
            root.optBoolean("maintenance_mode", false)
    }

    private suspend fun post(
        url: String,
        body: JSONObject,
        accessToken: String? = null,
    ): JSONObject {
        val builder = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
        if (!accessToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $accessToken")
        }
        return execute(builder.build())
    }

    private suspend fun execute(request: Request): JSONObject {
        val response = client.newCall(request).await()
        response.use {
            val rawBody = it.body?.string().orEmpty()
            val json = runCatching { JSONObject(rawBody) }.getOrNull()
            if (!it.isSuccessful) {
                val errorObject = json?.optJSONObject("error")
                val flatError = json?.opt("error") as? String
                val code = json?.optionalString("code")
                    ?: errorObject?.optionalString("code")
                    ?: flatError?.trim()?.takeIf(String::isNotEmpty)
                val message = json?.optionalString("message")
                    ?: errorObject?.optionalString("message")
                val retryAfter = json?.optInt("retry_after_seconds")
                    ?.takeIf { seconds -> seconds > 0 }
                    ?: it.header("Retry-After")?.toIntOrNull()
                throw RemoteFailure(it.code, code, message, retryAfter)
            }
            return json ?: throw org.json.JSONException("Expected JSON response")
        }
    }

    private fun parseAuthPayload(json: JSONObject): AuthPayload {
        val userJson = json.getJSONObject("user")
        val sessionJson = json.getJSONObject("session")
        val accessToken = sessionJson.getString("access_token")
        val expiry = expiryFromJwt(accessToken)
            ?: throw org.json.JSONException("Session expiry missing")
        return AuthPayload(
            user = AuthUser(
                id = userJson.getString("id"),
                phoneNumber = userJson.optionalString("phone_number"),
                profileCompleted = userJson.optBoolean("profile_completed", false),
                hasPassword = userJson.optBoolean("has_password", false),
                passwordRequired = userJson.optBoolean("password_required", false),
                accountStatus = userJson.optString("account_status", "active"),
                username = userJson.optionalString("username"),
                fullName = userJson.optionalString("full_name"),
            ),
            session = AuthSession(
                accessToken = accessToken,
                refreshToken = sessionJson.getString("refresh_token"),
                expiresAtEpochSeconds = expiry,
            ),
            isNewUser = json.optBoolean("is_new_user", false),
        )
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, exception: IOException) {
                if (continuation.isActive) continuation.resumeWithException(exception)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        })
    }

    private fun JSONObject.optionalString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeIf(String::isNotEmpty)
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
