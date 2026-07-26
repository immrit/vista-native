package ir.coffevista.vista_native.features.auth.data

import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.AuthSession
import ir.coffevista.vista_native.core.model.auth.AuthUser
import ir.coffevista.vista_native.core.model.auth.IdentifierLookup
import ir.coffevista.vista_native.core.model.auth.OtpChallenge
import ir.coffevista.vista_native.core.model.auth.OtpVerification
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

    @POST("v1/auth/2fa/setup")
    suspend fun setPassword(
        @Header("Authorization") authorization: String,
        @Body body: JsonObject,
    ): Response<Unit>

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: JsonObject): Response<JsonObject>
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
            return OtpVerification.PasswordChallenge(token)
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

    override suspend fun setPassword(accessToken: String, password: String) {
        executeUnit(
            api.setPassword(
                authorization = "Bearer $accessToken",
                body = jsonBody("password" to password),
            ),
        )
    }

    override suspend fun refresh(refreshToken: String): AuthPayload =
        parseAuthPayload(
            execute(api.refresh(jsonBody("refresh_token" to refreshToken))),
        )

    override suspend fun isMaintenanceMode(): Boolean =
        bootstrapStatusClient.fetch().maintenance

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
        return AuthPayload(
            user = AuthUser(
                id = user.string("id")
                    ?: throw kotlinx.serialization.SerializationException("user id missing"),
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
