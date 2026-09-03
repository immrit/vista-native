package ir.coffevista.vista_native.core.database.feed

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "feed_post",
    primaryKeys = ["account_id", "id"]
)
data class FeedPostEntity(
    @ColumnInfo(name = "account_id")
    val accountId: String,

    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "content")
    val content: String?,

    @ColumnInfo(name = "image_url")
    val imageUrl: String?,

    @ColumnInfo(name = "image_urls")
    val imageUrls: List<String>,

    @ColumnInfo(name = "video_url")
    val videoUrl: String?,

    @ColumnInfo(name = "music_url")
    val musicUrl: String?,

    @ColumnInfo(name = "aspect_ratio")
    val aspectRatio: String?,

    @ColumnInfo(name = "music_title")
    val musicTitle: String?,

    @ColumnInfo(name = "tags")
    val tags: List<String>,

    @ColumnInfo(name = "like_count")
    val likeCount: Long,

    @ColumnInfo(name = "comment_count")
    val commentCount: Long,

    @ColumnInfo(name = "is_liked")
    val isLiked: Boolean,

    @ColumnInfo(name = "is_saved")
    val isSaved: Boolean,

    @ColumnInfo(name = "hide_like_count")
    val hideLikeCount: Boolean,

    @ColumnInfo(name = "hide_comment_count")
    val hideCommentCount: Boolean,

    @ColumnInfo(name = "author_id")
    val authorId: String,

    @ColumnInfo(name = "author_username")
    val authorUsername: String?,

    @ColumnInfo(name = "author_full_name")
    val authorFullName: String,

    @ColumnInfo(name = "author_avatar_url")
    val authorAvatarUrl: String?,

    @ColumnInfo(name = "author_is_verified")
    val authorIsVerified: Boolean,

    @ColumnInfo(name = "author_verification_type")
    val authorVerificationType: String?,

    @ColumnInfo(name = "author_follow_status")
    val authorFollowStatus: String? = null,

    @ColumnInfo(name = "feed_source")
    val feedSource: String? = null,

    @ColumnInfo(name = "edited_by_vista", defaultValue = "0")
    val editedByVista: Boolean = false,

    @ColumnInfo(name = "moderation_reason")
    val moderationReason: String? = null,

    @ColumnInfo(name = "comments_disabled", defaultValue = "0")
    val commentsDisabled: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Long,
)
