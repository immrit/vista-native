package ir.coffevista.vista_native.features.feed.ui

import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import ir.coffevista.vista_native.features.feed.data.Comment
import ir.coffevista.vista_native.features.feed.data.CommentRepository
import ir.coffevista.vista_native.features.feed.data.CommentSnapshot
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CommentsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCommentRepository
    private lateinit var viewModel: CommentsViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCommentRepository()
        viewModel = CommentsViewModel(repository, SignedInAuth("account-a"), FakeOwnProfileDao())
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun createCommentReconcilesOptimisticItemWithCanonicalResponse() = runTest(dispatcher) {
        viewModel.loadComments("post-1")
        advanceUntilIdle()

        viewModel.submitComment("  سلام  ")
        assertTrue(viewModel.uiState.value.isSubmitting)
        assertTrue(viewModel.uiState.value.comments.single().id.startsWith("local-"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertEquals("server-comment", state.comments.single().id)
        assertEquals("سلام", state.comments.single().content)
        assertEquals(1L, state.submissionVersion)
        assertEquals("نظر با موفقیت ثبت شد", state.notice)
    }

    @Test fun createFailureRollsBackOptimisticItem() = runTest(dispatcher) {
        repository.createFailure = IOException("offline")
        viewModel.loadComments("post-1")
        advanceUntilIdle()

        viewModel.submitComment("سلام")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.comments.isEmpty())
        assertEquals("offline", viewModel.uiState.value.error)
    }

    @Test fun deleteFailureRestoresComment() = runTest(dispatcher) {
        repository.comments = listOf(comment("existing"))
        repository.deleteFailure = IOException("delete failed")
        viewModel.loadComments("post-1")
        advanceUntilIdle()

        viewModel.deleteComment("existing")
        assertTrue(viewModel.uiState.value.comments.isEmpty())
        advanceUntilIdle()

        assertEquals("existing", viewModel.uiState.value.comments.single().id)
        assertEquals("delete failed", viewModel.uiState.value.error)
    }

    @Test fun rejectsBlankAndOverLimitBeforeRepositoryCall() = runTest(dispatcher) {
        viewModel.loadComments("post-1")
        advanceUntilIdle()

        viewModel.submitComment("   ")
        assertTrue(viewModel.uiState.value.error!!.contains("خالی"))
        viewModel.submitComment("a".repeat(2_201))
        assertTrue(viewModel.uiState.value.error!!.contains("۲۲۰۰"))
        assertEquals(0, repository.createCalls)
    }

    @Test fun nestedRepliesAndFixtureTreeBuilding() = runTest(dispatcher) {
        // Authenticated runtime fixture: sara84 post text with zizi_81 root and sara84 reply جانممم
        val root1 = comment("c1", "سلام به همه", authorUserId = "user-zizi", authorUsername = "zizi_81", createdAt = "2026-08-13T10:00:00Z")
        val reply1 = comment("c2", "جانممم", authorUserId = "account-sara", authorUsername = "sara84", parentCommentId = "c1", createdAt = "2026-08-13T10:05:00Z")
        val root2 = comment("c3", "عالیه", authorUserId = "user-3", authorUsername = "ali", createdAt = "2026-08-13T09:00:00Z")

        repository.comments = listOf(root1, reply1, root2)
        viewModel.loadComments("post-sara")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.comments.size) // 2 roots: c1 and c3
        val c1Root = state.comments.first { it.id == "c1" }
        assertEquals(1, c1Root.replies.size)
        assertEquals("c2", c1Root.replies.first().id)
        assertEquals("جانممم", c1Root.replies.first().content)
        assertEquals("sara84", c1Root.replies.first().authorUsername)
    }

    @Test fun replyTargetAndCancel() = runTest(dispatcher) {
        val root = comment("c1", "کامنت اصلی", authorUserId = "user-zizi", authorUsername = "zizi_81")
        repository.comments = listOf(root)
        viewModel.loadComments("post-1")
        advanceUntilIdle()

        viewModel.setReplyingTo(root)
        assertEquals("c1", viewModel.uiState.value.replyingTo?.id)

        // Cancel reply
        viewModel.setReplyingTo(null)
        assertNull(viewModel.uiState.value.replyingTo)

        // Reply again and submit
        viewModel.setReplyingTo(root)
        viewModel.submitComment("پاسخ به کامنت")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.replyingTo)
        val updatedRoot = viewModel.uiState.value.comments.first { it.id == "c1" }
        assertEquals(1, updatedRoot.replies.size)
        assertEquals("server-comment", updatedRoot.replies.first().id)
    }

    @Test fun editCommentUpdatesContentInTree() = runTest(dispatcher) {
        val root = comment("c1", "متن قبلی", authorUserId = "account-a")
        repository.comments = listOf(root)
        viewModel.loadComments("post-1")
        advanceUntilIdle()

        viewModel.setEditingComment(root)
        assertEquals("c1", viewModel.uiState.value.editingComment?.id)

        viewModel.submitComment("متن جدید ویرایش شده")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingComment)
        assertEquals("متن جدید ویرایش شده", viewModel.uiState.value.comments.single().content)
        assertEquals("دیدگاه ویرایش شد", viewModel.uiState.value.notice)
    }

    @Test fun inlineReplyEditUpdatesTheExistingReplyInsteadOfCreatingAComment() = runTest(dispatcher) {
        val root = comment("root", "ریشه", authorUserId = "account-b")
        val reply = comment("reply", "متن قدیمی", authorUserId = "account-a", parentCommentId = "root")
        repository.comments = listOf(root, reply)
        viewModel.loadComments("post-1")
        advanceUntilIdle()

        viewModel.updateComment("reply", "متن ویرایش‌شده")
        advanceUntilIdle()

        val loadedRoot = viewModel.uiState.value.comments.single()
        assertEquals(1, loadedRoot.replies.size)
        assertEquals("reply", loadedRoot.replies.single().id)
        assertEquals("متن ویرایش‌شده", loadedRoot.replies.single().content)
        assertEquals(0, repository.createCalls)
    }

    @Test fun entitlementsCheck() = runTest(dispatcher) {
        val ownComment = comment(
            "c1",
            "متن من",
            authorUserId = "account-a",
            postOwnerId = "post-owner-1",
        ).copy(authorIsVerified = true)
        val ownUnverifiedComment = ownComment.copy(id = "c1-unverified", authorIsVerified = false)
        val otherComment = comment("c2", "متن دیگری", authorUserId = "account-b", postOwnerId = "post-owner-1")

        assertTrue(viewModel.canEdit(ownComment))
        assertFalse(viewModel.canEdit(ownUnverifiedComment))
        assertFalse(viewModel.canEdit(otherComment))

        assertTrue(viewModel.canDelete(ownComment, postOwnerId = "post-owner-1"))
        assertFalse(viewModel.canDelete(otherComment, postOwnerId = "post-owner-1"))

        // Post owner can delete comments on their own post
        val postOwnerAuth = SignedInAuth("post-owner-1")
        val postOwnerVm = CommentsViewModel(repository, postOwnerAuth, FakeOwnProfileDao())
        advanceUntilIdle()
        assertTrue(postOwnerVm.canDelete(otherComment, postOwnerId = "post-owner-1"))

        // Other users can report, author cannot
        assertTrue(viewModel.canReport(otherComment))
        assertFalse(viewModel.canReport(ownComment))
    }

    @Test fun paginationAccumulatesAndMergesFlatComments() = runTest(dispatcher) {
        val page1 = listOf(
            comment("c1", "کامنت ۱", createdAt = "2026-08-13T10:00:00Z"),
            comment("c2", "کامنت ۲", createdAt = "2026-08-13T09:00:00Z"),
        )
        val page2 = listOf(
            comment("c3", "پاسخ به کامنت ۱ در صفحه دوم", parentCommentId = "c1", createdAt = "2026-08-13T10:30:00Z"),
            comment("c4", "کامنت ۴", createdAt = "2026-08-13T08:00:00Z"),
        )

        repository.comments = page1
        repository.hasMore = true
        viewModel.loadComments("post-1")
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.comments.size)
        assertTrue(viewModel.uiState.value.hasMore)

        repository.comments = page2
        repository.hasMore = false
        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.hasMore)
        // c3 should be nested under c1!
        val c1 = state.comments.first { it.id == "c1" }
        assertEquals(1, c1.replies.size)
        assertEquals("c3", c1.replies.first().id)
        assertEquals(3, state.comments.size) // roots: c1, c2, c4
    }

    @Test fun loadingMoreRepliesReplacesPartialThreadWithFullServerSubtree() = runTest(dispatcher) {
        val root = comment("root", "ریشه")
        val firstReply = comment("reply-1", "پاسخ اولیه", parentCommentId = "root")
        val nestedReply = comment("reply-2", "پاسخ تو در تو", parentCommentId = "reply-1")
        repository.comments = listOf(root, firstReply)
        repository.repliesByParent["root"] = listOf(firstReply, nestedReply)
        viewModel.loadComments("post-1")
        advanceUntilIdle()

        viewModel.loadReplies("root")
        assertTrue("loading state must be visible while the server reload is pending", "root" in viewModel.uiState.value.loadingReplyIds)
        advanceUntilIdle()

        val loadedRoot = viewModel.uiState.value.comments.single()
        assertFalse("loading state must clear after success", "root" in viewModel.uiState.value.loadingReplyIds)
        assertEquals(listOf("reply-1"), loadedRoot.replies.map { it.id })
        assertEquals(listOf("reply-2"), loadedRoot.replies.single().replies.map { it.id })
    }

    @Test fun loadingMoreRepliesHidesRawTransportFailureFromTheComposer() = runTest(dispatcher) {
        val root = comment("root", "ریشه")
        repository.comments = listOf(root, comment("reply-1", parentCommentId = "root"))
        repository.repliesFailure = IOException("unauthorized")
        viewModel.loadComments("post-1")
        advanceUntilIdle()

        viewModel.loadReplies("root")
        advanceUntilIdle()

        assertFalse("root" in viewModel.uiState.value.loadingReplyIds)
        assertEquals(
            "مشکلی در بارگذاری پاسخ‌ها پیش آمد. لطفا دوباره تلاش کنید.",
            viewModel.uiState.value.error,
        )
    }

    private fun comment(
        id: String,
        content: String = "سلام",
        authorUserId: String = "account-a",
        authorUsername: String = "vista",
        authorFullName: String = "Vista User",
        parentCommentId: String? = null,
        postOwnerId: String = "post-owner",
        createdAt: String = "2026-08-12T00:00:00Z",
    ) = Comment(
        id = id,
        postId = "post-1",
        content = content,
        createdAt = createdAt,
        authorUserId = authorUserId,
        authorUsername = authorUsername,
        authorFullName = authorFullName,
        authorAvatarUrl = null,
        authorIsVerified = false,
        authorVerificationType = null,
        authorRole = null,
        postOwnerId = postOwnerId,
        parentCommentId = parentCommentId,
    )

    private inner class FakeCommentRepository : CommentRepository {
        var comments = emptyList<Comment>()
        var hasMore = false
        var createFailure: Exception? = null
        var deleteFailure: Exception? = null
        var repliesFailure: Exception? = null
        var createCalls = 0
        val repliesByParent = mutableMapOf<String, List<Comment>>()
        override suspend fun getComments(postId: String, offset: Int, limit: Int) = CommentSnapshot(comments, hasMore)
        override suspend fun getReplies(postId: String, parentCommentId: String): List<Comment> {
            repliesFailure?.let { throw it }
            return repliesByParent[parentCommentId].orEmpty()
        }
        override suspend fun createComment(postId: String, content: String, parentCommentId: String?): Comment {
            createCalls++
            createFailure?.let { throw it }
            return comment("server-comment", content, parentCommentId = parentCommentId)
        }
        override suspend fun deleteComment(postId: String, commentId: String) { deleteFailure?.let { throw it } }
        override suspend fun updateComment(commentId: String, content: String) = comment(commentId, content)
        override suspend fun reportComment(commentId: String, reason: String, additionalDetails: String?) = Unit
        override suspend fun addMentions(commentId: String, userIds: List<String>) = Unit
    }

    private class SignedInAuth(userId: String = "account-a") : AuthenticationStateProvider {
        override val state: StateFlow<AuthenticationState> = MutableStateFlow(
            AuthenticationState.SignedIn(
                AuthenticatedContext(userId, true, false, false, "Vista User"),
            ),
        )
    }

    private class FakeOwnProfileDao : OwnProfileDao {
        override fun getOwnProfile(userId: String) = MutableStateFlow<OwnProfileEntity?>(null)
        override suspend fun insertOrUpdate(profile: OwnProfileEntity) = Unit
        override suspend fun deleteProfile(userId: String) = Unit
        override suspend fun deleteAll() = Unit
    }
}
