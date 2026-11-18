package ir.coffevista.vista_native.features.chat.data.repository

import ir.coffevista.vista_native.features.chat.data.local.ConversationEntity
import ir.coffevista.vista_native.features.chat.data.local.MessageEntity
import ir.coffevista.vista_native.features.chat.data.local.applyMessageSummaryToConversation
import ir.coffevista.vista_native.features.chat.data.remote.ConversationDto
import ir.coffevista.vista_native.features.chat.data.remote.GroupInviteDto
import ir.coffevista.vista_native.features.chat.presentation.components.PendingSendFlight
import ir.coffevista.vista_native.features.chat.presentation.components.isEligibleSendFlightCandidate
import androidx.compose.ui.geometry.Rect
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `merging older message preserves newest preview checkmark and conversation ordering`() {
        val newestPreview = byteArrayOf(1, 2, 3)
        val olderPreview = byteArrayOf(9, 9, 9)
        val base = ConversationEntity(
            accountId = "me",
            id = "conv-1",
            type = "PRIVATE",
            title = "Ali",
            avatarUrl = null,
            peerId = "peer-1",
            lastMessageCiphertext = newestPreview,
            lastMessageAtEpochMillis = 2_000L,
            unreadCount = 0,
            isArchived = true,
            isPinned = false,
            isMuted = false,
            requestStatus = "accepted",
            lastSyncedAtEpochMillis = 2_000L,
            lastMessageType = "image",
            lastMessageIsMine = true,
            lastMessageStatus = "DELIVERED",
        )
        val olderMessage = MessageEntity(
            accountId = "me",
            conversationId = "conv-1",
            clientId = "m-old",
            serverId = "m-old",
            senderId = "peer-1",
            contentCiphertext = byteArrayOf(5),
            contentKind = "text",
            createdAtEpochMillis = 1_000L,
            editedAtEpochMillis = null,
            deletedAtEpochMillis = null,
            status = "READ",
            replyToMessageId = null,
            replyToContentCiphertext = null,
            isMine = false,
            lastMutationAtEpochMillis = 1_000L,
            messageType = "text",
        )

        val afterOlder = applyMessageSummaryToConversation(
            current = base,
            base = base,
            fallbackConversation = base,
            finalMessage = olderMessage,
            previewCiphertext = olderPreview,
            incrementUnread = false,
            mutationAtEpochMillis = 999_999L,
            allowUnarchive = false,
        )

        // Old paginated/fetched message must never overwrite newest preview, flip isMine, unarchive, or bump time to now().
        assertTrue(afterOlder.lastMessageCiphertext.contentEquals(newestPreview))
        assertEquals(2_000L, afterOlder.lastMessageAtEpochMillis)
        assertEquals("image", afterOlder.lastMessageType)
        assertTrue(afterOlder.lastMessageIsMine)
        assertEquals("DELIVERED", afterOlder.lastMessageStatus)
        assertTrue(afterOlder.isArchived)

        // Same-age REST sync on an existing conversation must not use wall-clock mutationAtEpochMillis either.
        val newerMine = olderMessage.copy(
            clientId = "m-new",
            serverId = "m-new",
            senderId = "me",
            isMine = true,
            createdAtEpochMillis = 2_000L,
            status = "DELIVERED",
            messageType = "image",
        )
        val afterSameAgeRest = applyMessageSummaryToConversation(
            current = base,
            base = base,
            fallbackConversation = base,
            finalMessage = newerMine,
            previewCiphertext = newestPreview,
            incrementUnread = false,
            mutationAtEpochMillis = 999_999L,
            allowUnarchive = false,
        )
        assertEquals(2_000L, afterSameAgeRest.lastMessageAtEpochMillis)
        assertTrue(afterSameAgeRest.isArchived)
    }

    @Test
    fun `remote conversation merge preserves checkmark when server omits sender and delivery metadata`() {
        val local = ConversationEntity(
            accountId = "me",
            id = "conv-1",
            type = "PRIVATE",
            title = "Sara",
            avatarUrl = null,
            peerId = "peer-1",
            lastMessageCiphertext = byteArrayOf(7, 8),
            lastMessageAtEpochMillis = 5_000L,
            unreadCount = 0,
            isArchived = false,
            isPinned = false,
            isMuted = false,
            requestStatus = "accepted",
            lastSyncedAtEpochMillis = 5_000L,
            lastMessageType = "voice",
            lastMessageIsMine = true,
            lastMessageStatus = "DELIVERED",
        )
        val legacyRemoteEntity = local.copy(
            lastMessageCiphertext = null,
            lastMessageType = null,
            lastMessageIsMine = false,
            lastMessageStatus = "SENT",
        )
        val legacyDto = ConversationDto(id = "conv-1", peerId = "peer-1")

        val preserved = mergeRemoteConversationSummary(local, legacyRemoteEntity, legacyDto)
        assertTrue(preserved.lastMessageIsMine)
        assertEquals("DELIVERED", preserved.lastMessageStatus)
        assertEquals("voice", preserved.lastMessageType)
        assertTrue(preserved.lastMessageCiphertext.contentEquals(byteArrayOf(7, 8)))

        val explicitDto = ConversationDto(
            id = "conv-1",
            peerId = "peer-1",
            lastMessageSenderId = "me",
            isLastMessageFromMe = true,
            lastMessageType = "voice",
            lastMessageDeliveryStatus = "read",
            lastMessageIsRead = true,
        )
        val explicitEntity = local.copy(lastMessageIsMine = true, lastMessageStatus = "READ")
        val updated = mergeRemoteConversationSummary(local, explicitEntity, explicitDto)
        assertTrue(updated.lastMessageIsMine)
        assertEquals("READ", updated.lastMessageStatus)
    }

    @Test
    fun `self read receipts never mark own outgoing messages as read by peer`() {
        assertFalse(isOtherParticipantReadReceipt("me", "me"))
        assertFalse(isOtherParticipantReadReceipt("ME", "me"))
        assertFalse(isOtherParticipantReadReceipt(null, "me"))
        assertTrue(isOtherParticipantReadReceipt("peer-1", "me"))
    }

    @Test
    fun `telegram send flight eligibility matches only newly pending own message once`() {
        val pending = PendingSendFlight(
            flightId = 1L,
            text = "سلام خوبی؟",
            replyPreview = null,
            startBoundsLocal = Rect(10f, 100f, 200f, 140f),
            createdAtMs = 1_000L,
        )
        assertTrue(
            isEligibleSendFlightCandidate(
                pending = pending,
                candidateStableKey = "c-1",
                candidateText = "سلام خوبی؟",
                candidateIsMine = true,
                candidateIsPending = true,
                flownMessageKeys = emptySet(),
            ),
        )
        assertFalse(
            isEligibleSendFlightCandidate(
                pending = pending,
                candidateStableKey = "c-1",
                candidateText = "سلام خوبی؟",
                candidateIsMine = true,
                candidateIsPending = true,
                flownMessageKeys = setOf("c-1"),
            ),
        )
        assertFalse(
            isEligibleSendFlightCandidate(
                pending = pending,
                candidateStableKey = "c-2",
                candidateText = "سلام خوبی؟",
                candidateIsMine = false,
                candidateIsPending = true,
                flownMessageKeys = emptySet(),
            ),
        )
    }
}
