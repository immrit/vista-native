package ir.coffevista.vista_native.features.profile.ui

data class ProfilePostUiModel(
    val id: String,
    val userId: String,
    val authorFullName: String,
    val authorUsername: String?,
    val authorAvatarUrl: String?,
    val authorIsVerified: Boolean,
    val content: String?,
    val imageUrl: String?,
    val videoUrl: String?,
    val aspectRatio: Float?,
    val likeCount: Long,
    val commentCount: Long,
    val hideLikeCount: Boolean,
    val hideCommentCount: Boolean,
    val isLiked: Boolean,
    val isSaved: Boolean,
    val createdAt: String,
)

data class ProfilePostsPresentationState(
    val posts: List<ProfilePostUiModel> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val isOffline: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val appendError: String? = null,
)
