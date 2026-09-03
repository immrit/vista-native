package ir.coffevista.vista_native.features.feed.ui

import ir.coffevista.vista_native.features.feed.data.Comment
import org.junit.Assert.assertEquals
import org.junit.Test

class CommentMentionCandidatesTest {
    @Test
    fun `candidates retain stable user ids and include nested replies once`() {
        val reply = comment(id = "reply", userId = "user-b", username = "b")
        val root = comment(id = "root", userId = "user-a", username = "a", replies = listOf(reply))
        val duplicate = comment(id = "duplicate", userId = "user-a", username = "a")

        assertEquals(
            listOf(CommentMention("user-a", "a"), CommentMention("user-b", "b")),
            commentMentionCandidates(listOf(root, duplicate)),
        )
    }

    @Test
    fun `blank usernames cannot become mention candidates`() {
        assertEquals(emptyList<CommentMention>(), commentMentionCandidates(listOf(comment(username = " "))))
    }

    private fun comment(
        id: String = "comment",
        userId: String = "user",
        username: String? = "username",
        replies: List<Comment> = emptyList(),
    ) = Comment(
        id = id,
        postId = "post",
        content = "content",
        createdAt = "2026-09-02T00:00:00Z",
        authorUserId = userId,
        authorUsername = username,
        authorFullName = "User",
        authorAvatarUrl = null,
        authorIsVerified = false,
        authorVerificationType = null,
        parentCommentId = null,
        replies = replies,
    )
}
