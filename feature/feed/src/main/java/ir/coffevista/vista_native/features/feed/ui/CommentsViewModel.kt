package ir.coffevista.vista_native.features.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.feed.data.Comment
import ir.coffevista.vista_native.features.feed.data.CommentRepository
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import java.time.Instant

data class CommentsUiState(
    val isLoading: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val hasMore: Boolean = false,
    val isAppending: Boolean = false,
    val loadingReplyIds: Set<String> = emptySet(),
    val error: String? = null,
    val replyingTo: Comment? = null,
    val editingComment: Comment? = null,
    val isSubmitting: Boolean = false,
    val pendingDeleteIds: Set<String> = emptySet(),
    val notice: String? = null,
    val submissionVersion: Long = 0,
    val currentUserAvatarUrl: String? = null,
    val currentUserId: String? = null,
    val currentUserUsername: String? = null,
    val currentUserFullName: String? = null,
    val currentUserRole: String? = null,
    val currentUserHasBlueBadge: Boolean = false,
)

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val repository: CommentRepository,
    private val authStateProvider: AuthenticationStateProvider,
    private val ownProfileDao: OwnProfileDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()
    
    private var currentPostId: String? = null
    private var currentOffset = 0
    private var currentUserId: String? = null
    private var currentProfile: OwnProfileEntity? = null
    private var profileObservation: Job? = null
    private var flatLoaded: List<Comment> = emptyList()

    init {
        viewModelScope.launch {
            authStateProvider.state.collect { authState ->
                val userId = (authState as? AuthenticationState.SignedIn)?.context?.userId
                currentUserId = userId
                profileObservation?.cancel()
                if (userId == null) {
                    currentProfile = null
                    _uiState.update {
                        it.copy(
                            currentUserAvatarUrl = null,
                            currentUserId = null,
                            currentUserUsername = null,
                            currentUserFullName = null,
                            currentUserRole = null,
                            currentUserHasBlueBadge = false,
                        )
                    }
                } else {
                    profileObservation = viewModelScope.launch {
                        ownProfileDao.getOwnProfile(userId).collectLatest { profile ->
                            currentProfile = profile
                            val isVerified = profile?.isVerified == true
                            val isBlue = profile?.verificationType?.equals("blueTick", ignoreCase = true) == true ||
                                profile?.verificationType?.equals("blue", ignoreCase = true) == true
                            _uiState.update {
                                it.copy(
                                    currentUserAvatarUrl = profile?.avatarUrl,
                                    currentUserId = userId,
                                    currentUserUsername = profile?.username,
                                    currentUserFullName = profile?.fullName,
                                    currentUserRole = profile?.accountType,
                                    currentUserHasBlueBadge = isVerified && isBlue,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun isCurrentUser(userId: String): Boolean = userId == currentUserId

    /** Mirrors Flutter: a user can edit only their own verified/badged comment. */
    fun canEdit(comment: Comment): Boolean {
        if (!isCurrentUser(comment.authorUserId)) return false
        val verification = comment.authorVerificationType.orEmpty().lowercase()
        val hasSpecialBadge = verification.contains("black") ||
            verification.contains("gold") ||
            verification.contains("blue")
        return comment.authorIsVerified || hasSpecialBadge
    }

    fun canDelete(comment: Comment, postOwnerId: String? = null): Boolean {
        val userId = currentUserId ?: return false
        val isOwner = comment.authorUserId == userId
        val isPostOwner = (comment.postOwnerId.isNotBlank() && comment.postOwnerId == userId) ||
            (!postOwnerId.isNullOrBlank() && postOwnerId == userId)
        val hasAdminBlueTick = _uiState.value.currentUserRole == "admin" && _uiState.value.currentUserHasBlueBadge
        return isOwner || isPostOwner || hasAdminBlueTick
    }

    fun canReport(comment: Comment): Boolean = !isCurrentUser(comment.authorUserId)

    fun loadComments(postId: String, force: Boolean = false) {
        if (currentPostId == postId && !force) return
        currentPostId = postId
        currentOffset = 0
        flatLoaded = emptyList()
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                comments = emptyList(),
                replyingTo = null,
                editingComment = null,
            )
        }
        viewModelScope.launch {
            try {
                val snapshot = repository.getComments(postId, currentOffset)
                flatLoaded = snapshot.comments
                val tree = buildCommentTree(flatLoaded)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        comments = tree,
                        hasMore = snapshot.hasMore,
                    )
                }
                currentOffset += snapshot.comments.size
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "مشکلی در بارگذاری کامنت‌ها پیش آمد. لطفا دوباره تلاش کنید.",
                    )
                }
            }
        }
    }

    fun loadMore() {
        val postId = currentPostId ?: return
        val state = _uiState.value
        if (state.isLoading || state.isAppending || !state.hasMore) return
        
        _uiState.update { it.copy(isAppending = true) }
        viewModelScope.launch {
            try {
                val snapshot = repository.getComments(postId, currentOffset)
                val knownIds = flatLoaded.map { it.id }.toSet()
                flatLoaded = flatLoaded + snapshot.comments.filter { it.id !in knownIds }
                val tree = buildCommentTree(flatLoaded)
                _uiState.update {
                    it.copy(
                        isAppending = false,
                        comments = tree,
                        hasMore = snapshot.hasMore,
                    )
                }
                currentOffset += snapshot.comments.size
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAppending = false,
                        error = e.message ?: "مشکلی در بارگذاری پاسخ‌ها پیش آمد. لطفا دوباره تلاش کنید.",
                    )
                }
            }
        }
    }

    /** Reloads a complete reply subtree, matching Flutter's "view more replies" behavior. */
    fun loadReplies(parentCommentId: String) {
        val postId = currentPostId ?: return
        if (parentCommentId in _uiState.value.loadingReplyIds) return
        _uiState.update { it.copy(loadingReplyIds = it.loadingReplyIds + parentCommentId, error = null) }
        viewModelScope.launch {
            try {
                val replies = repository.getReplies(postId, parentCommentId)
                val staleDescendantIds = descendantIdsOf(parentCommentId)
                flatLoaded = (flatLoaded.filterNot { it.id in staleDescendantIds } + flattenComments(replies))
                    .distinctBy(Comment::id)
                _uiState.update {
                    it.copy(
                        comments = buildCommentTree(flatLoaded),
                        loadingReplyIds = it.loadingReplyIds - parentCommentId,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingReplyIds = it.loadingReplyIds - parentCommentId,
                        // Flutter deliberately uses a stable, Persian user-facing error here.
                        // Do not surface raw transport/backend messages in the composer.
                        error = "مشکلی در بارگذاری پاسخ‌ها پیش آمد. لطفا دوباره تلاش کنید.",
                    )
                }
            }
        }
    }

    fun setReplyingTo(comment: Comment?) {
        _uiState.update { it.copy(replyingTo = comment, editingComment = null) }
    }

    fun setEditingComment(comment: Comment?) {
        _uiState.update { it.copy(editingComment = comment, replyingTo = null) }
    }

    /** Inline comment editing must never fall through to the create-comment path. */
    fun updateComment(commentId: String, content: String) {
        val normalized = content.trim()
        if (normalized.isBlank()) {
            _uiState.update { it.copy(error = "متن دیدگاه نمی‌تواند خالی باشد") }
            return
        }
        if (normalized.codePointCount(0, normalized.length) > MAX_COMMENT_LENGTH) {
            _uiState.update { it.copy(error = "دیدگاه نمی‌تواند بیشتر از ۲۲۰۰ نویسه باشد") }
            return
        }
        if (_uiState.value.isSubmitting || flatLoaded.none { it.id == commentId }) return

        val existing = flatLoaded.first { it.id == commentId }
        _uiState.update { it.copy(isSubmitting = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                val serverUpdated = repository.updateComment(commentId, normalized)
                // Some comment PATCH envelopes omit relationship fields. Keep the local
                // parent/post identity so editing a reply never promotes it to a root.
                val updated = serverUpdated.copy(
                    postId = serverUpdated.postId.ifBlank { existing.postId },
                    parentCommentId = serverUpdated.parentCommentId ?: existing.parentCommentId,
                )
                flatLoaded = flatLoaded.map { if (it.id == commentId) updated else it }
                _uiState.update {
                    it.copy(
                        comments = buildCommentTree(flatLoaded),
                        editingComment = null,
                        isSubmitting = false,
                        notice = "دیدگاه ویرایش شد",
                        submissionVersion = it.submissionVersion + 1,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = e.message ?: "مشکلی در ویرایش کامنت پیش آمد. لطفا دوباره تلاش کنید.",
                    )
                }
            }
        }
    }

    fun submitComment(content: String, mentionedUserIds: List<String> = emptyList()) {
        val postId = currentPostId ?: return
        val normalized = content.trim()
        if (normalized.isBlank()) {
            _uiState.update { it.copy(error = "متن دیدگاه نمی‌تواند خالی باشد") }
            return
        }
        if (normalized.codePointCount(0, normalized.length) > MAX_COMMENT_LENGTH) {
            _uiState.update { it.copy(error = "دیدگاه نمی‌تواند بیشتر از ۲۲۰۰ نویسه باشد") }
            return
        }
        if (_uiState.value.isSubmitting) return
        
        val state = _uiState.value
        val editing = state.editingComment
        val replying = state.replyingTo

        val previousFlat = flatLoaded

        if (editing != null) {
            _uiState.update { it.copy(isSubmitting = true, error = null, notice = null) }
            viewModelScope.launch {
                try {
                    val updated = repository.updateComment(editing.id, normalized)
                    flatLoaded = flatLoaded.map { if (it.id == editing.id) updated else it }
                    _uiState.update {
                        it.copy(
                            comments = buildCommentTree(flatLoaded),
                            editingComment = null,
                            isSubmitting = false,
                            notice = "دیدگاه ویرایش شد",
                            submissionVersion = it.submissionVersion + 1,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = e.message ?: "مشکلی در ویرایش کامنت پیش آمد. لطفا دوباره تلاش کنید.",
                        )
                    }
                }
            }
        } else {
            val optimisticId = "local-${System.nanoTime()}"
            val optimistic = Comment(
                id = optimisticId,
                postId = postId,
                content = normalized,
                createdAt = Instant.now().toString(),
                authorUserId = currentUserId.orEmpty(),
                authorUsername = currentProfile?.username,
                authorFullName = currentProfile?.fullName ?: "شما",
                authorAvatarUrl = currentProfile?.avatarUrl,
                authorIsVerified = currentProfile?.isVerified ?: false,
                authorVerificationType = currentProfile?.verificationType,
                parentCommentId = replying?.id,
            )
            flatLoaded = listOf(optimistic) + flatLoaded
            _uiState.update {
                it.copy(
                    comments = buildCommentTree(flatLoaded),
                    isSubmitting = true,
                    error = null,
                    notice = null,
                )
            }
            viewModelScope.launch {
                try {
                    val newComment = repository.createComment(postId, normalized, replying?.id)
                    if (mentionedUserIds.isNotEmpty()) {
                        repository.addMentions(newComment.id, mentionedUserIds)
                    }
                    flatLoaded = flatLoaded.map { if (it.id == optimisticId) newComment else it }
                    _uiState.update {
                        it.copy(
                            comments = buildCommentTree(flatLoaded),
                            replyingTo = null,
                            isSubmitting = false,
                            notice = "نظر با موفقیت ثبت شد",
                            submissionVersion = it.submissionVersion + 1,
                        )
                    }
                    if (replying == null) currentOffset += 1
                } catch (e: Exception) {
                    flatLoaded = previousFlat
                    _uiState.update {
                        it.copy(
                            comments = buildCommentTree(flatLoaded),
                            isSubmitting = false,
                            error = e.message ?: "مشکلی در ارسال کامنت پیش آمد. لطفا دوباره تلاش کنید.",
                        )
                    }
                }
            }
        }
    }

    fun deleteComment(commentId: String) {
        val postId = currentPostId ?: return
        if (commentId in _uiState.value.pendingDeleteIds) return
        val previousFlat = flatLoaded
        flatLoaded = flatLoaded.filter { it.id != commentId && it.parentCommentId != commentId }
        _uiState.update {
            it.copy(
                comments = buildCommentTree(flatLoaded),
                pendingDeleteIds = it.pendingDeleteIds + commentId,
                error = null,
            )
        }
        viewModelScope.launch {
            try {
                repository.deleteComment(postId, commentId)
                _uiState.update {
                    it.copy(
                        pendingDeleteIds = it.pendingDeleteIds - commentId,
                        notice = "دیدگاه حذف شد",
                    )
                }
            } catch (e: Exception) {
                flatLoaded = previousFlat
                _uiState.update {
                    it.copy(
                        comments = buildCommentTree(flatLoaded),
                        pendingDeleteIds = it.pendingDeleteIds - commentId,
                        error = e.message ?: "مشکلی در حذف کامنت پیش آمد. لطفا دوباره تلاش کنید.",
                    )
                }
            }
        }
    }

    fun reportComment(commentId: String, reason: String, additionalDetails: String? = null) {
        viewModelScope.launch {
            try {
                repository.reportComment(commentId, reason, additionalDetails)
                _uiState.update { it.copy(notice = "گزارش دیدگاه ثبت شد", error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "ثبت گزارش ناموفق بود") }
            }
        }
    }

    fun retry() {
        currentPostId?.let { loadComments(it, force = true) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(error = null, notice = null) }
    }

    private fun descendantIdsOf(parentCommentId: String): Set<String> {
        val childrenByParent = flatLoaded.groupBy { it.parentCommentId }
        val ids = mutableSetOf<String>()
        fun collect(parentId: String) {
            childrenByParent[parentId].orEmpty().forEach { child ->
                if (ids.add(child.id)) collect(child.id)
            }
        }
        collect(parentCommentId)
        return ids
    }

    private fun flattenComments(comments: List<Comment>): List<Comment> = buildList {
        fun append(items: List<Comment>) {
            items.forEach { item ->
                add(item.copy(replies = emptyList()))
                append(item.replies)
            }
        }
        append(comments)
    }

    companion object {
        const val MAX_COMMENT_LENGTH = 2_200

        fun buildCommentTree(allComments: List<Comment>): List<Comment> {
            val flatList = mutableListOf<Comment>()
            fun flatten(items: List<Comment>) {
                for (item in items) {
                    flatList.add(item)
                    if (item.replies.isNotEmpty()) flatten(item.replies)
                }
            }
            flatten(allComments)

            val commentMap = flatList.associate { it.id to it.copy(replies = emptyList()) }.toMutableMap()
            val parentToReplies = mutableMapOf<String, MutableList<Comment>>()
            val rootComments = mutableListOf<Comment>()

            for (comment in flatList) {
                val parentId = comment.parentCommentId
                if (parentId == null) {
                    commentMap[comment.id]?.let { rootComments.add(it) }
                } else {
                    val parent = commentMap[parentId]
                    if (parent != null) {
                        val list = parentToReplies.getOrPut(parentId) { mutableListOf() }
                        if (list.none { it.id == comment.id }) {
                            commentMap[comment.id]?.let { list.add(it) }
                        }
                    } else {
                        commentMap[comment.id]?.let { rootComments.add(it) }
                    }
                }
            }

            fun attachReplies(item: Comment): Comment {
                val childReplies = parentToReplies[item.id] ?: emptyList()
                val sortedChildren = childReplies.map { attachReplies(it) }.sortedWith { a, b ->
                    b.createdAt.compareTo(a.createdAt)
                }
                return item.copy(replies = sortedChildren)
            }

            val uniqueRoots = rootComments.distinctBy { it.id }
            return uniqueRoots.map { attachReplies(it) }.sortedWith { a, b ->
                b.createdAt.compareTo(a.createdAt)
            }
        }
    }
}
