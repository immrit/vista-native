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

    suspend fun getUserHighlights(profileUserId: String): List<ProfileHighlightDto> = emptyList()

    suspend fun createHighlight(
        title: String,
        coverUrl: String? = null,
        storyIds: List<String> = emptyList(),
    ): Result<ProfileHighlightDto> = Result.failure(UnsupportedOperationException())

    suspend fun updateHighlight(
        highlightId: String,
        title: String? = null,
        coverUrl: String? = null,
        storyIds: List<String>? = null,
    ): Result<ProfileHighlightDto> = Result.failure(UnsupportedOperationException())

    suspend fun deleteHighlight(highlightId: String): Result<Unit> = Result.failure(UnsupportedOperationException())

    suspend fun getOwnNote(): Result<ProfileNoteDto?> = Result.success(null)

    suspend fun upsertNote(content: String): Result<ProfileNoteDto> = Result.failure(UnsupportedOperationException())

    suspend fun deleteNote(): Result<Unit> = Result.success(Unit)

    suspend fun getBatchNotes(userIds: List<String>): Result<Map<String, ProfileNoteDto>> = Result.success(emptyMap())

    suspend fun reportProfile(
        profileId: String,
        reason: String,
        additionalDetails: String? = null,
    ): Result<Unit>
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
