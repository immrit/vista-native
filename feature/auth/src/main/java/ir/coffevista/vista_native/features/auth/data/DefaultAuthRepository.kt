package ir.coffevista.vista_native.features.auth.data

import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.common.DefaultDispatcherProvider
import ir.coffevista.vista_native.core.common.DispatcherProvider
import ir.coffevista.vista_native.core.common.FoundationSignal
import ir.coffevista.vista_native.core.common.FoundationTelemetry
import ir.coffevista.vista_native.core.common.LogLevel
import ir.coffevista.vista_native.core.common.LogSink
import ir.coffevista.vista_native.core.common.NoOpFoundationTelemetry
import ir.coffevista.vista_native.core.common.RedactingLogger
import ir.coffevista.vista_native.core.common.SecureLogger
import ir.coffevista.vista_native.core.network.ErrorClassifier
import ir.coffevista.vista_native.features.auth.domain.AuthRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultAuthRepository @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val telemetry: FoundationTelemetry = NoOpFoundationTelemetry,
    private val logger: SecureLogger = RedactingLogger(false, LogSink { }),
) : AuthRepository {
    override suspend fun lookupIdentifier(identifier: String) =
        call("بررسی شناسه ورود") { remote.lookupIdentifier(identifier) }

    override suspend fun login(identifier: String, password: String) =
        call("ورود") { remote.login(identifier, password) }

    override suspend fun sendOtp(phoneNumber: String) =
        call("ارسال کد تایید") { remote.sendOtp(phoneNumber) }

    override suspend fun verifyOtp(phoneNumber: String, code: String) =
        call("تایید کد") { remote.verifyOtp(phoneNumber, code) }

    override suspend fun verifyTwoFactor(token: String, password: String) =
        call("تایید دو مرحله‌ای") { remote.verifyTwoFactor(token, password) }

    override suspend fun setPassword(accessToken: String, password: String) =
        call("تعیین رمز عبور") { remote.setPassword(accessToken, password) }

    override suspend fun recoveryOptions(identifier: String) =
        call("دریافت گزینه‌های بازیابی") { remote.recoveryOptions(identifier) }

    override suspend fun sendRecoveryCode(optionId: String) =
        call("ارسال کد بازیابی") { remote.sendRecoveryCode(optionId) }

    override suspend fun verifyRecoveryCode(optionId: String, code: String) =
        call("تایید کد بازیابی") { remote.verifyRecoveryCode(optionId, code) }

    override suspend fun completeRecovery(token: String, newPassword: String) =
        call("تکمیل بازیابی") { remote.completeRecovery(token, newPassword) }

    override suspend fun refresh(refreshToken: String) =
        call("تمدید نشست") { remote.refresh(refreshToken) }

    override suspend fun maintenanceMode() =
        call("بررسی وضعیت سیستم") { remote.isMaintenanceMode() }

    private suspend fun <T> call(contextFa: String, block: suspend () -> T): Outcome<T> {
        return withContext(dispatchers.io) {
            try {
                Outcome.Success(block())
            } catch (throwable: Throwable) {
                val error = ErrorClassifier.classify(throwable, contextFa)
                telemetry.record(
                    FoundationSignal(
                        name = "foundation.auth.request",
                        outcome = "failure",
                        attributes = mapOf("kind" to error.kind.name),
                    ),
                )
                logger.log(
                    level = LogLevel.WARN,
                    event = "auth_request_failed",
                    fields = mapOf(
                        "kind" to error.kind.name,
                        "code" to error.code,
                        "cause_type" to error.causeType,
                    ),
                )
                Outcome.Failure(error)
            }
        }
    }
}
