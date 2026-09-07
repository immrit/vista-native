package ir.coffevista.vista_native.features.profile.data

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.ErrorKind
import ir.coffevista.vista_native.core.database.profile.PublicProfileDao
import ir.coffevista.vista_native.core.database.profile.PublicProfileEntity
import ir.coffevista.vista_native.core.network.BackendErrorParser
import ir.coffevista.vista_native.core.network.ErrorClassifier
import ir.coffevista.vista_native.core.network.RemoteFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import retrofit2.Response
import javax.inject.Inject

class OfflineFirstUserProfileRepository @Inject constructor(
    private val api: PublicProfileApi,
    private val dao: PublicProfileDao,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) : UserProfileRepository {
    private val mutationMutex = Mutex()

    override fun observeProfile(
        viewerAccountId: String,
        profileUserId: String,
    ): Flow<PublicProfile?> =
        dao.observe(viewerAccountId, profileUserId).map { it?.toDomain() }

    override suspend fun refreshProfile(
        viewerAccountId: String,
        profileUserId: String,
    ): ProfileRefreshResult {
        if (viewerAccountId == profileUserId) return ProfileRefreshResult.SelfProfile
        val hadCache = dao.get(viewerAccountId, profileUserId) != null
        return try {
            val isUuid = try {
                java.util.UUID.fromString(profileUserId)
                true
            } catch (_: Exception) {
                false
            }
            val initialResponse = if (isUuid) {
                api.fetchPublicProfile(profileUserId)
            } else {
                api.fetchProfileByUsername(profileUserId)
            }
            val response = if (!initialResponse.isSuccessful && isUuid && (initialResponse.code() == 400 || initialResponse.code() == 404)) {
                api.fetchProfileByUsername(profileUserId)
            } else {
                initialResponse
            }

            if (!response.isSuccessful) {
                val statusCode = response.code()
                if (!hadCache && (statusCode == 401 || statusCode == 403 || statusCode == 404)) {
                    return ProfileRefreshResult.Failure(
                        error = ir.coffevista.vista_native.core.common.AppError(
                            kind = ir.coffevista.vista_native.core.common.ErrorKind.VALIDATION,
                            messageFa = "کاربر یافت نشد",
                            code = "PROFILE_NOT_FOUND",
                        ),
                        hadCache = false,
                    )
                }
                ProfileRefreshResult.Failure(
                    error = response.toAppError("دریافت نمایه کاربر"),
                    hadCache = hadCache,
                )
            } else {
                val body = response.body()
                    ?: return ProfileRefreshResult.Failure(
                        error = malformed("پاسخ نمایه خالی بود"),
                        hadCache = hadCache,
                    )
                dao.upsert(body.toEntity(viewerAccountId, nowEpochMillis()))
                ProfileRefreshResult.Success
            }
        } catch (error: Exception) {
            ProfileRefreshResult.Failure(
                error = ErrorClassifier.classify(error, "دریافت نمایه کاربر"),
                hadCache = hadCache,
            )
        }
    }

    override suspend fun follow(
        viewerAccountId: String,
        profileUserId: String,
    ): FollowMutationResult = mutate(
        viewerAccountId = viewerAccountId,
        profileUserId = profileUserId,
        following = true,
    )

    override suspend fun unfollow(
        viewerAccountId: String,
        profileUserId: String,
    ): FollowMutationResult = mutate(
        viewerAccountId = viewerAccountId,
        profileUserId = profileUserId,
        following = false,
    )

    override suspend fun clearAccount(viewerAccountId: String) {
        dao.clearViewer(viewerAccountId)
    }

    private suspend fun mutate(
        viewerAccountId: String,
        profileUserId: String,
        following: Boolean,
    ): FollowMutationResult {
        if (viewerAccountId == profileUserId) return FollowMutationResult.Unavailable
        if (!mutationMutex.tryLock()) return FollowMutationResult.IgnoredConcurrent
        try {
            val original = dao.get(viewerAccountId, profileUserId)
                ?: return FollowMutationResult.Failure(
                    AppError(
                        kind = ErrorKind.VALIDATION,
                        messageFa = "ابتدا نمایه کاربر را دوباره بارگذاری کنید",
                    ),
                )
            if (original.isBlocked) return FollowMutationResult.Unavailable

            val wasFollowing = original.followStatus == FollowState.Following.wireName
            val optimistic = if (following) {
                original.copy(
                    followStatus = FollowState.Following.wireName,
                    followerCount = original.followerCount + if (wasFollowing) 0 else 1,
                )
            } else {
                original.copy(
                    followStatus = FollowState.NotFollowing.wireName,
                    followerCount = (original.followerCount - if (wasFollowing) 1 else 0)
                        .coerceAtLeast(0),
                )
            }
            dao.upsert(optimistic)

            return try {
                if (following) {
                    reconcileFollow(original, optimistic, profileUserId)
                } else {
                    reconcileUnfollow(original, optimistic, profileUserId)
                }
            } catch (error: Exception) {
                dao.upsert(original)
                FollowMutationResult.Failure(
                    ErrorClassifier.classify(error, "تغییر وضعیت دنبال کردن"),
                )
            }
        } finally {
            mutationMutex.unlock()
        }
    }

    private suspend fun reconcileFollow(
        original: PublicProfileEntity,
        optimistic: PublicProfileEntity,
        profileUserId: String,
    ): FollowMutationResult {
        val response = api.follow(FollowActionRequestDto(profileUserId))
        if (!response.isSuccessful) {
            dao.upsert(original)
            return FollowMutationResult.Failure(response.toAppError("دنبال کردن کاربر"))
        }
        val body = response.body()
            ?: run {
                dao.upsert(original)
                return FollowMutationResult.Failure(malformed("پاسخ Follow خالی بود"))
            }
        val canonical = FollowState.fromWireName(body.status)
        val reconciled = optimistic.copy(
            followStatus = canonical.wireName,
            followerCount = if (canonical == FollowState.Following) {
                optimistic.followerCount
            } else {
                original.followerCount
            },
        )
        dao.upsert(reconciled)
        return FollowMutationResult.Success(canonical)
    }

    private suspend fun reconcileUnfollow(
        original: PublicProfileEntity,
        optimistic: PublicProfileEntity,
        profileUserId: String,
    ): FollowMutationResult {
        val response = api.unfollow(FollowActionRequestDto(profileUserId))
        if (!response.isSuccessful) {
            dao.upsert(original)
            return FollowMutationResult.Failure(response.toAppError("لغو دنبال کردن کاربر"))
        }
        val body = response.body()
        if (body == null || body.status != "unfollowed") {
            dao.upsert(original)
            return FollowMutationResult.Failure(malformed("پاسخ Unfollow نامعتبر بود"))
        }
        dao.upsert(optimistic.copy(followStatus = FollowState.NotFollowing.wireName))
        return FollowMutationResult.Success(FollowState.NotFollowing)
    }

    private fun Response<*>.toAppError(contextFa: String): AppError {
        val parsed = BackendErrorParser.parse(
            rawBody = errorBody()?.string().orEmpty(),
            retryAfterHeader = headers()["Retry-After"],
        )
        return ErrorClassifier.classify(
            RemoteFailure(
                statusCode = code(),
                code = parsed.code,
                message = parsed.message,
                retryAfterSeconds = parsed.retryAfterSeconds,
            ),
            contextFa,
        )
    }

    private fun malformed(detail: String): AppError = AppError(
        kind = ErrorKind.MALFORMED_RESPONSE,
        messageFa = "پاسخ سرور قابل پردازش نبود. لطفاً دوباره تلاش کنید",
        causeType = detail,
    )
}
