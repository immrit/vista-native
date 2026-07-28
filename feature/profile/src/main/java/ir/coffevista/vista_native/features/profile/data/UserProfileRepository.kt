package ir.coffevista.vista_native.features.profile.data

import ir.coffevista.vista_native.core.common.AppError
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeProfile(
        viewerAccountId: String,
        profileUserId: String,
    ): Flow<PublicProfile?>

    suspend fun refreshProfile(
        viewerAccountId: String,
        profileUserId: String,
    ): ProfileRefreshResult

    suspend fun follow(
        viewerAccountId: String,
        profileUserId: String,
    ): FollowMutationResult

    suspend fun unfollow(
        viewerAccountId: String,
        profileUserId: String,
    ): FollowMutationResult

    suspend fun clearAccount(viewerAccountId: String)
}

sealed interface ProfileRefreshResult {
    data object Success : ProfileRefreshResult
    data object SelfProfile : ProfileRefreshResult
    data class Failure(
        val error: AppError,
        val hadCache: Boolean,
    ) : ProfileRefreshResult
}

sealed interface FollowMutationResult {
    data class Success(val state: FollowState) : FollowMutationResult
    data class Failure(val error: AppError) : FollowMutationResult
    data object IgnoredConcurrent : FollowMutationResult
    data object Unavailable : FollowMutationResult
}
