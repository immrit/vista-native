package ir.coffevista.vista_native.features.profile.data

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import ir.coffevista.vista_native.core.network.BackendErrorParser
import ir.coffevista.vista_native.core.network.ErrorClassifier
import ir.coffevista.vista_native.core.network.RemoteFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import javax.inject.Inject

class OfflineFirstOwnProfileRepository @Inject constructor(
    private val api: ProfileApi,
    private val dao: OwnProfileDao
) : OwnProfileRepository {
    override fun getOwnProfileFlow(userId: String): Flow<OwnProfileEntity?> {
        return dao.getOwnProfile(userId)
    }

    override suspend fun fetchAndCacheOwnProfile(userId: String): Outcome<Unit> {
        return try {
            val response = api.fetchOwnProfile()
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    dao.insertOrUpdate(dto.toEntity())
                } ?: return Outcome.Failure(
                    ErrorClassifier.classify(
                        IllegalStateException("Empty profile response"),
                        "دریافت اطلاعات نمایه"
                    )
                )
                Outcome.Success(Unit)
            } else {
                Outcome.Failure(ErrorClassifier.classify(response.toRemoteFailure(), "دریافت اطلاعات نمایه"))
            }
        } catch (e: Exception) {
            Outcome.Failure(ErrorClassifier.classify(e, "دریافت اطلاعات نمایه"))
        }
    }

    override suspend fun updateOwnProfile(request: ProfileUpdateRequestDto): Outcome<Unit> {
        return try {
            val response = api.updateOwnProfile(request)
            if (response.isSuccessful) {
                response.body()?.let { dao.insertOrUpdate(it.toEntity()) }
                    ?: return Outcome.Failure(
                        ErrorClassifier.classify(
                            IllegalStateException("Empty profile update response"),
                            "ویرایش نمایه",
                        ),
                    )
                Outcome.Success(Unit)
            } else {
                Outcome.Failure(ErrorClassifier.classify(response.toRemoteFailure(), "ویرایش نمایه"))
            }
        } catch (e: Exception) {
            Outcome.Failure(ErrorClassifier.classify(e, "ویرایش نمایه"))
        }
    }

    override suspend fun updateAvatar(avatarUrl: String): Outcome<Unit> {
        return try {
            val response = api.updateAvatar(ProfileAvatarUpdateRequestDto(avatarUrl))
            if (response.isSuccessful) {
                response.body()?.let { dao.insertOrUpdate(it.toEntity()) }
                    ?: return Outcome.Failure(
                        ErrorClassifier.classify(
                            IllegalStateException("Empty avatar update response"),
                            "ویرایش تصویر نمایه",
                        ),
                    )
                Outcome.Success(Unit)
            } else {
                Outcome.Failure(ErrorClassifier.classify(response.toRemoteFailure(), "ویرایش تصویر نمایه"))
            }
        } catch (e: Exception) {
            Outcome.Failure(ErrorClassifier.classify(e, "ویرایش تصویر نمایه"))
        }
    }

    override suspend fun clearProfileData() {
        dao.deleteAll()
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

    private fun ProfileDto.toEntity() = OwnProfileEntity(
        userId = userId,
        username = username,
        fullName = fullName,
        bio = bio,
        email = email,
        phoneNumber = phoneNumber,
        websiteUrl = websiteUrl,
        birthDate = birthDate,
        gender = gender,
        maritalStatus = maritalStatus,
        showEmail = showEmail,
        showBirthDate = showBirthDate,
        showGender = showGender,
        showMaritalStatus = showMaritalStatus,
        avatarUrl = avatarUrl,
        isVerified = isVerified,
        verificationType = verificationType,
        accountType = accountType,
        isPrivate = isPrivate,
        postCount = postCount,
        followerCount = followerCount,
        followingCount = followingCount,
        joinOrder = joinOrder,
        subscriptionPlan = subscriptionPlan,
        premiumDaysRemaining = premiumDaysRemaining,
        messagePrivacy = messagePrivacy,
        allowProfileZoom = allowProfileZoom,
        updatedAt = updatedAt,
        createdAt = createdAt,
    )
}
