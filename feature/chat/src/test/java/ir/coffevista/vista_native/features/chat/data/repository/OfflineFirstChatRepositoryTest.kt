package ir.coffevista.vista_native.features.chat.data.repository

import ir.coffevista.vista_native.features.chat.data.remote.GroupInviteDto
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstChatRepositoryTest {
    @Test
    fun `created group survives a transient details enrichment failure`() = runTest {
        val result = fetchCreatedGroupInfoBestEffort {
            throw IOException("details unavailable")
        }

        assertNull(result)
    }

    @Test
    fun `created group enrichment preserves coroutine cancellation`() = runTest {
        var cancellationPropagated = false

        try {
            fetchCreatedGroupInfoBestEffort {
                throw CancellationException("cancelled")
            }
        } catch (_: CancellationException) {
            cancellationPropagated = true
        }

        assertTrue(cancellationPropagated)
    }

    @Test
    fun `group details survive a transient invite enrichment failure`() = runTest {
        val result = fetchGroupInviteBestEffort {
            throw IOException("invite unavailable")
        }

        assertNull(result)
    }

    @Test
    fun `group invite enrichment preserves coroutine cancellation`() = runTest {
        var cancellationPropagated = false

        try {
            fetchGroupInviteBestEffort {
                throw CancellationException("cancelled")
            }
        } catch (_: CancellationException) {
            cancellationPropagated = true
        }

        assertTrue(cancellationPropagated)
    }

    @Test
    fun `group invite enrichment returns the server contract`() = runTest {
        val invite = GroupInviteDto(inviteCode = "fresh", enabled = false)

        assertTrue(fetchGroupInviteBestEffort { invite } === invite)
    }

    @Test
    fun `parseSingleMessageReactionsJson handles wrapped empty and absent payloads safely`() {
        val direct = parseSingleMessageReactionsJson(
            """{"reactions":[{"user_id":"u1","emoji":"❤️"},{"user_id":"u2","emoji":"❤️"}]}""",
        )
        assertEquals(mapOf("❤️" to setOf("u1", "u2")), direct)

        val emptyExplicit = parseSingleMessageReactionsJson("""{"reactions":[]}""")
        assertEquals(emptyMap<String, Set<String>>(), emptyExplicit)

        val nestedData = parseSingleMessageReactionsJson(
            """{"data":{"reactions":[{"userId":"u3","emoji":"🔥"}]}}""",
        )
        assertEquals(mapOf("🔥" to setOf("u3")), nestedData)

        // An acknowledgment without a `reactions` array must return null so existing reactions are not wiped.
        assertNull(parseSingleMessageReactionsJson("""{"action":"added","success":true}"""))
        assertNull(parseSingleMessageReactionsJson(""))
    }

    @Test
    fun `parseBatchReactionsJson hydrates per-message reaction maps`() {
        val batch = parseBatchReactionsJson(
            """{"reactions":{"m1":[{"user_id":"u1","emoji":"👍"}],"m2":[]}}""",
        )

        assertEquals(mapOf("👍" to setOf("u1")), batch?.get("m1"))
        assertEquals(emptyMap<String, Set<String>>(), batch?.get("m2"))
    }

    @Test
    fun `toggleLocalReactionMap toggles optimistically and case-insensitively`() {
        val added = toggleLocalReactionMap(emptyMap(), "Account-1", "❤️")
        assertEquals(mapOf("❤️" to setOf("Account-1")), added)

        val removed = toggleLocalReactionMap(added, "account-1", "❤️")
        assertTrue(removed.isEmpty())
    }
}
