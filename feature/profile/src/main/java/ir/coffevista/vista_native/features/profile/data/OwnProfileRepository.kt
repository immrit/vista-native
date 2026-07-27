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

class OwnProfileRepository @Inject constructor(
    private val api: ProfileApi,
    private val dao: OwnProfileDao
) {
    fun getOwnProfileFlow(userId: String): Flow<OwnProfileEntity?> {
        return dao.getOwnProfile(userId)
    }

    suspend fun fetchAndCacheOwnProfile(userId: String): Outcome<Unit> {
        return try {
            val response = api.fetchOwnProfile()
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val entity = OwnProfileEntity(
                        userId = dto.userId, // use dto.userId to be safe, should match userId
                        username = dto.username,
                        fullName = dto.fullName,
                        bio = dto.bio,
                        avatarUrl = dto.avatarUrl,
                        isVerified = dto.isVerified,
                        accountType = dto.accountType,
                        postCount = dto.postCount,
                        followerCount = dto.followerCount,
                        followingCount = dto.followingCount,
                        updatedAt = dto.updatedAt
                    )
                    dao.insertOrUpdate(entity)
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

    suspend fun clearProfileData() {
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
}
