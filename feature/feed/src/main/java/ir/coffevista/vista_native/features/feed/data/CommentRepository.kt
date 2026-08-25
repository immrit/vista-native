package ir.coffevista.vista_native.features.feed.data

import ir.coffevista.vista_native.core.database.feed.FeedDao
import ir.coffevista.vista_native.core.network.BackendErrorParser
import ir.coffevista.vista_native.core.network.RemoteFailure
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

data class CommentSnapshot(
    val comments: List<Comment>,
    val hasMore: Boolean,
)

interface CommentRepository {
    suspend fun getComments(postId: String, offset: Int, limit: Int = 20): CommentSnapshot
    /** Mirrors Flutter's getReplies: reload the complete server-side thread for one parent. */
    suspend fun getReplies(postId: String, parentCommentId: String): List<Comment>
    suspend fun createComment(postId: String, content: String, parentCommentId: String? = null): Comment
    suspend fun deleteComment(postId: String, commentId: String)
    suspend fun updateComment(commentId: String, content: String): Comment
    suspend fun reportComment(commentId: String, reason: String, additionalDetails: String? = null)
    suspend fun addMentions(commentId: String, userIds: List<String>)
}

@Singleton
class DefaultCommentRepository @Inject constructor(
    private val commentApi: CommentApi,
    private val feedDao: FeedDao,
) : CommentRepository {

    override suspend fun getComments(postId: String, offset: Int, limit: Int): CommentSnapshot {
        val response = remote { commentApi.getComments(postId, limit, offset) }
        return CommentSnapshot(
            comments = response.comments.map { it.asExternalModel() },
            hasMore = response.hasMore,
        )
    }

    override suspend fun getReplies(postId: String, parentCommentId: String): List<Comment> {
        // Flutter reloads up to 1000 comments for this post before rebuilding the requested
        // subtree. A paged screen list alone can otherwise make "مشاهده پاسخ‌های بیشتر"
        // reveal only the replies that happened to be in its first page.
        val all = remote { commentApi.getComments(postId, limit = 1_000, offset = 0) }
            .comments
            .map { it.asExternalModel() }
        val childrenByParent = all.groupBy { it.parentCommentId }

        fun descendants(parentId: String): List<Comment> = childrenByParent[parentId]
            .orEmpty()
            .map { item -> item.copy(replies = descendants(item.id)) }
            .sortedByDescending(Comment::createdAt)

        return descendants(parentCommentId)
    }

    override suspend fun createComment(postId: String, content: String, parentCommentId: String?): Comment {
        requireCommentContent(content)
        val response = remote {
            commentApi.createComment(CreateCommentRequestDto(postId, content.trim(), parentCommentId))
        }
        feedDao.updateCommentCount(postId, 1L)
        return response.asExternalModel()
    }

    override suspend fun deleteComment(postId: String, commentId: String) {
        remote { commentApi.deleteComment(commentId) }
        feedDao.updateCommentCount(postId, -1L)
    }

    override suspend fun updateComment(commentId: String, content: String): Comment {
        requireCommentContent(content)
        val response = remote { commentApi.updateComment(commentId, UpdateCommentRequestDto(content.trim())) }
        return response.asExternalModel()
    }

    override suspend fun reportComment(commentId: String, reason: String, additionalDetails: String?) {
        require(reason.isNotBlank()) { "دلیل گزارش را انتخاب کنید" }
        remote {
            commentApi.reportComment(
                commentId,
                ReportCommentRequestDto(reason.trim(), additionalDetails?.trim()?.takeIf(String::isNotEmpty)),
            )
        }
    }

    override suspend fun addMentions(commentId: String, userIds: List<String>) {
        val normalized = userIds.map(String::trim).filter(String::isNotEmpty).distinct()
        if (normalized.isEmpty()) return
        remote { commentApi.addMentions(commentId, CommentMentionsRequestDto(normalized)) }
    }

    private fun requireCommentContent(content: String) {
        require(content.isNotBlank()) { "متن دیدگاه نمی‌تواند خالی باشد" }
        require(content.trim().codePointCount(0, content.trim().length) <= MAX_COMMENT_LENGTH) {
            "دیدگاه نمی‌تواند بیشتر از ۲۲۰۰ نویسه باشد"
        }
    }

    private suspend fun <T> remote(block: suspend () -> T): T = try {
        block()
    } catch (error: HttpException) {
        val parsed = BackendErrorParser.parse(
            rawBody = error.response()?.errorBody()?.string().orEmpty(),
            retryAfterHeader = error.response()?.headers()?.get("Retry-After"),
        )
        throw RemoteFailure(
            statusCode = error.code(),
            code = parsed.code,
            message = localizedCommentError(parsed.code, parsed.message),
            retryAfterSeconds = parsed.retryAfterSeconds,
            cause = error,
        )
    }

    private fun localizedCommentError(code: String?, fallback: String?): String = when (code) {
        "COMMENT_EMPTY" -> "متن دیدگاه نمی‌تواند خالی باشد"
        "COMMENT_TOO_LONG" -> "دیدگاه نمی‌تواند بیشتر از ۲۲۰۰ نویسه باشد"
        "COMMENT_NOT_FOUND" -> "این دیدگاه دیگر در دسترس نیست"
        "POST_NOT_VISIBLE" -> "امکان ثبت دیدگاه برای این پست وجود ندارد"
        "REASON_REQUIRED" -> "دلیل گزارش را انتخاب کنید"
        "UNAUTHORIZED" -> "کاربر وارد نشده است"
        "FORBIDDEN" -> "شما اجازه ویرایش این کامنت را ندارید"
        else -> fallback?.takeIf(String::isNotBlank) ?: "ارتباط با سرور ناموفق بود"
    }

    private companion object {
        const val MAX_COMMENT_LENGTH = 2_200
    }
}
