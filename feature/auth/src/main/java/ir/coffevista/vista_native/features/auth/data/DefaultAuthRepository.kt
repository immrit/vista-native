package ir.coffevista.vista_native.features.auth.data

import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.network.ErrorClassifier
import ir.coffevista.vista_native.features.auth.domain.AuthRepository

class DefaultAuthRepository(
    private val remote: AuthRemoteDataSource,
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

    override suspend fun refresh(refreshToken: String) =
        call("تمدید نشست") { remote.refresh(refreshToken) }

    override suspend fun maintenanceMode() =
        call("بررسی وضعیت سیستم") { remote.isMaintenanceMode() }

    private suspend fun <T> call(contextFa: String, block: suspend () -> T): Outcome<T> {
        return try {
            Outcome.Success(block())
        } catch (throwable: Throwable) {
            Outcome.Failure(ErrorClassifier.classify(throwable, contextFa))
        }
    }
}
