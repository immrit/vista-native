package ir.coffevista.vista_native.features.auth.data

import ir.coffevista.vista_native.core.common.SessionCredentialsProvider
import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.AuthSession
import ir.coffevista.vista_native.core.model.auth.AuthUser
import ir.coffevista.vista_native.core.model.auth.IdentifierLookup
import ir.coffevista.vista_native.core.model.auth.LoginOutcome
import ir.coffevista.vista_native.core.model.auth.OtpChallenge
import ir.coffevista.vista_native.core.model.auth.OtpVerification
import ir.coffevista.vista_native.core.model.auth.TwoFactorChallenge
import ir.coffevista.vista_native.core.model.auth.TwoFactorRecoveryChallenge
import ir.coffevista.vista_native.core.model.auth.TwoFactorResetResult
import ir.coffevista.vista_native.core.network.BackendErrorParser
import ir.coffevista.vista_native.core.network.BootstrapStatusClient
import ir.coffevista.vista_native.core.network.InternalApi
import ir.coffevista.vista_native.core.network.RemoteFailure
import ir.coffevista.vista_native.core.security.expiryFromJwt
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import javax.inject.Inject

interface AuthApi {
    @POST("v1/auth/lookup")
    suspend fun lookup(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/login")
    suspend fun login(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/send-otp")
    suspend fun sendOtp(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/verify-otp")
    suspend fun verifyOtp(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/2fa/verify")
    suspend fun verifyTwoFactor(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/2fa/recovery/send")
    suspend fun sendTwoFactorRecovery(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/2fa/recovery/verify")
    suspend fun verifyTwoFactorRecovery(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/2fa/reset/request")
    suspend fun requestTwoFactorReset(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/set-password")
    suspend fun setPassword(
        @Header("Authorization") authorization: String,
        @Body body: JsonObject,
    ): Response<Unit>

    @POST("v1/auth/recovery-options")
    suspend fun recoveryOptions(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/recovery-send")
    suspend fun sendRecoveryCode(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/recovery-verify")
    suspend fun verifyRecoveryCode(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/recovery-complete")
    suspend fun completeRecovery(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: JsonObject): Response<JsonObject>

    @POST("v1/auth/logout")
    suspend fun logout(@Body body: JsonObject): Response<Unit>
}

class OkHttpAuthRemoteDataSource @Inject constructor(
    @InternalApi retrofit: Retrofit,
    private val bootstrapStatusClient: BootstrapStatusClient,
) : AuthRemoteDataSource {
    private val api = retrofit.create(AuthApi::class.java)

    override suspend fun lookupIdentifier(identifier: String): IdentifierLookup {
        val json = execute(api.lookup(jsonBody("identifier" to identifier)))
        return IdentifierLookup(
            exists = json.boolean("exists"),
            isPhone = json.boolean("is_phone"),
            normalizedIdentifier = json.string("normalized_identifier"),
            authFlow = json.string("auth_flow"),
            accountStatus = json.string("account_status"),
        )
    }

    override suspend fun login(identifier: String, password: String): AuthPayload =
        parseAuthPayload(
            execute(
                api.login(jsonBody("identifier" to identifier, "password" to password)),
            ),
        )

    override suspend fun loginWithChallenge(identifier: String, password: String): LoginOutcome {
        val json = execute(
            api.login(jsonBody("identifier" to identifier, "password" to password)),
        )
        if (json.boolean("is_2fa_required")) {
            val token = json.string("two_factor_token")
                ?: throw kotlinx.serialization.SerializationException("two_factor_token missing")
            return LoginOutcome.TwoFactorRequired(
                TwoFactorChallenge(
                    twoFactorToken = token,
                    hint = json.string("two_fa_hint"),
                    hasRecovery = json.boolean("has_recovery"),
                    recoveryEmailPattern = json.string("recovery_email_pattern"),
                    pendingResetDate = json.long("pending_reset_date") ?: 0L,
                ),
            )
        }
        return LoginOutcome.Authenticated(parseAuthPayload(json))
    }

    override suspend fun sendOtp(phoneNumber: String): OtpChallenge {
        val json = execute(api.sendOtp(jsonBody("phone_number" to phoneNumber)))
        return OtpChallenge(
            expiresInSeconds = (json.int("expires_in_seconds") ?: 60).coerceAtLeast(1),
            debugCode = json.string("debug_code"),
        )
    }

    override suspend fun verifyOtp(phoneNumber: String, code: String): OtpVerification {
        val json = execute(
            api.verifyOtp(jsonBody("phone_number" to phoneNumber, "code" to code)),
        )
        if (json.boolean("is_2fa_required")) {
            val token = json.string("two_factor_token")
                ?: throw kotlinx.serialization.SerializationException(
                    "two_factor_token missing",
                )
            return OtpVerification.PasswordChallenge(
                twoFactorToken = token,
                hint = json.string("two_fa_hint"),
                hasRecovery = json.boolean("has_recovery"),
                recoveryEmailPattern = json.string("recovery_email_pattern"),
                pendingResetDate = json.long("pending_reset_date") ?: 0L,
            )
        }
        val auth = json["auth"]?.jsonObject
            ?: throw kotlinx.serialization.SerializationException("auth missing")
        return OtpVerification.Authenticated(parseAuthPayload(auth))
    }

    override suspend fun verifyTwoFactor(
        twoFactorToken: String,
        password: String,
    ): AuthPayload = parseAuthPayload(
        execute(
            api.verifyTwoFactor(
                jsonBody("two_factor_token" to twoFactorToken, "password" to password),
            ),
        ),
    )

    override suspend fun sendTwoFactorRecoveryCode(twoFactorToken: String): TwoFactorRecoveryChallenge {
        val json = execute(
            api.sendTwoFactorRecovery(jsonBody("two_factor_token" to twoFactorToken)),
        )
        return TwoFactorRecoveryChallenge(
            sessionId = json.string("session_id"),
            emailPattern = json.string("email_pattern"),
            message = json.string("message"),
        )
    }

    override suspend fun verifyTwoFactorRecoveryCode(
        twoFactorToken: String,
        sessionId: String?,
        code: String,
    ): AuthPayload {
        val body = buildJsonObject {
            put("two_factor_token", twoFactorToken)
            put("code", code)
            if (!sessionId.isNullOrBlank()) {
                put("session_id", sessionId)
            }
        }
        return parseAuthPayload(execute(api.verifyTwoFactorRecovery(body)))
    }

    override suspend fun requestTwoFactorReset(twoFactorToken: String): TwoFactorResetResult {
        val json = execute(
            api.requestTwoFactorReset(jsonBody("two_factor_token" to twoFactorToken)),
        )
        val completed = json.boolean("reset_completed")
        val authObj = json["auth"]?.jsonObject
        return TwoFactorResetResult(
            resetCompleted = completed,
            pendingResetDate = json.long("pending_reset_date") ?: 0L,
            waitSeconds = json.long("wait_seconds") ?: 0L,
            message = json.string("message"),
            auth = authObj?.let { parseAuthPayload(it) },
        )
    }

    override suspend fun setPassword(accessToken: String, password: String) {
        executeUnit(
            api.setPassword(
                authorization = "Bearer $accessToken",
                body = jsonBody("password" to password),
            ),
        )
    }

    override suspend fun recoveryOptions(identifier: String): List<RecoveryOption> {
        val json = execute(api.recoveryOptions(jsonBody("identifier" to identifier)))
        return json["options"]?.jsonArray.orEmpty().mapNotNull { item ->
            val option = item.jsonObject
            val id = option.string("id") ?: return@mapNotNull null
            val method = option.string("method") ?: return@mapNotNull null
            RecoveryOption(id, method.lowercase(), option.string("masked").orEmpty())
        }
    }

    override suspend fun sendRecoveryCode(optionId: String) {
        execute(api.sendRecoveryCode(jsonBody("option_id" to optionId)))
    }

    override suspend fun verifyRecoveryCode(optionId: String, code: String): String {
        val json = execute(api.verifyRecoveryCode(jsonBody("option_id" to optionId, "code" to code)))
        return json.string("password_reset_token")
            ?: throw kotlinx.serialization.SerializationException("password_reset_token missing")
    }

    override suspend fun completeRecovery(token: String, newPassword: String) {
        execute(api.completeRecovery(jsonBody("password_reset_token" to token, "new_password" to newPassword)))
    }

    override suspend fun refresh(refreshToken: String): AuthPayload =
        parseAuthPayload(
            execute(api.refresh(jsonBody("refresh_token" to refreshToken))),
        )

    override suspend fun isMaintenanceMode(): Boolean =
        bootstrapStatusClient.fetch().maintenance

    override suspend fun logout(refreshToken: String?) {
        val body = buildJsonObject {
            if (!refreshToken.isNullOrBlank()) {
                put("refresh_token", refreshToken)
            }
        }
        runCatching { executeUnit(api.logout(body)) }
    }

    private fun parseAuthPayload(json: JsonObject): AuthPayload {
        val user = json["user"]?.jsonObject
            ?: throw kotlinx.serialization.SerializationException("user missing")
        val session = json["session"]?.jsonObject
            ?: throw kotlinx.serialization.SerializationException("session missing")
        val accessToken = session.string("access_token")
            ?: throw kotlinx.serialization.SerializationException("access_token missing")
        val refreshToken = session.string("refresh_token")
            ?: throw kotlinx.serialization.SerializationException("refresh_token missing")
        val expiry = expiryFromJwt(accessToken)
            ?: throw kotlinx.serialization.SerializationException("session expiry missing")
        val userId = user.string("id")
            ?: throw kotlinx.serialization.SerializationException("user id missing")
        val backendSessionId = session.string("session_id")?.takeIf(String::isNotBlank)
        val backendSessionToken = session.string("session_token")?.takeIf(String::isNotBlank)
        if (backendSessionId != null && backendSessionToken != null) {
            SessionCredentialsProvider.current?.saveSessionCredentials(
                sessionId = backendSessionId,
                sessionToken = backendSessionToken,
                userId = userId,
            )
        }
        return AuthPayload(
            user = AuthUser(
                id = userId,
                phoneNumber = user.string("phone_number"),
                profileCompleted = user.boolean("profile_completed"),
                hasPassword = user.boolean("has_password"),
                passwordRequired = user.boolean("password_required"),
                accountStatus = user.string("account_status") ?: "active",
                username = user.string("username"),
                fullName = user.string("full_name"),
            ),
            session = AuthSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAtEpochSeconds = expiry,
            ),
            isNewUser = json.boolean("is_new_user"),
        )
    }

    private fun execute(response: Response<JsonObject>): JsonObject {
        if (!response.isSuccessful) throw response.toRemoteFailure()
        return response.body()
            ?: throw kotlinx.serialization.SerializationException("Expected JSON response")
    }

    private fun executeUnit(response: Response<Unit>) {
        if (!response.isSuccessful) throw response.toRemoteFailure()
    }

    private fun Response<*>.toRemoteFailure(): RemoteFailure {
        val error = BackendErrorParser.parse(
            rawBody = errorBody()?.string().orEmpty(),
            retryAfterHeader = headers()["Retry-After"],
        )
        return RemoteFailure(
            statusCode = code(),
            code = error.code,
            message = error.message,
            retryAfterSeconds = error.retryAfterSeconds,
            feature = error.feature,
        )
    }
}

private fun jsonBody(vararg fields: Pair<String, String>): JsonObject = buildJsonObject {
    fields.forEach { (key, value) -> put(key, value) }
}

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)

private fun JsonObject.boolean(key: String): Boolean =
    (this[key] as? JsonPrimitive)?.booleanOrNull ?: false

private fun JsonObject.int(key: String): Int? =
    (this[key] as? JsonPrimitive)?.intOrNull

private fun JsonObject.long(key: String): Long? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
