package ir.coffevista.vista_native.features.feed.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class PostMentionsTest {
    @Test fun `lookup normalizes names deduplicates ids and omits unknowns`() = runTest {
        val queried = mutableListOf<String>()
        val api = object : PostMentionsApi {
            override suspend fun profile(username: String): PostMentionProfileDto {
                queried += username
                if (username == "unknown") throw IllegalStateException()
                return PostMentionProfileDto(userId = " id-1 ")
            }
            override suspend fun add(postId: String, request: PostMentionsRequestDto) = Unit
        }
        assertEquals(listOf("id-1"), PostMentions(api).resolve(listOf("Alice", "ALICE", "alias", "unknown")))
        assertEquals(listOf("alice", "alias", "unknown"), queried)
    }

    @Test fun `attachment skips missing post or ids and failure is non fatal`() = runTest {
        var calls = 0
        val api = object : PostMentionsApi {
            override suspend fun profile(username: String) = PostMentionProfileDto()
            override suspend fun add(postId: String, request: PostMentionsRequestDto) {
                calls++
                assertEquals("post-1", postId)
                assertEquals(listOf("id-1"), request.userIds)
                throw IllegalStateException()
            }
        }
        val mentions = PostMentions(api)
        mentions.attach("", listOf("id-1"))
        mentions.attach("post-1", emptyList())
        mentions.attach("post-1", listOf("id-1"))
        assertEquals(1, calls)
    }

    @Test fun `lookup and attachment propagate cancellation`() = runTest {
        val api = object : PostMentionsApi {
            override suspend fun profile(username: String): PostMentionProfileDto = throw CancellationException()
            override suspend fun add(postId: String, request: PostMentionsRequestDto): Unit = throw CancellationException()
        }
        val mentions = PostMentions(api)
        try { mentions.resolve(listOf("alice")); fail("Cancellation swallowed") } catch (_: CancellationException) { }
        try { mentions.attach("post-1", listOf("id-1")); fail("Cancellation swallowed") } catch (_: CancellationException) { }
    }
}
