package ir.coffevista.vista_native.features.chat.presentation.components

import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.Attachment
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.TransferState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageContextActionPolicyTest {

    @Test
    fun `confirmed peer text exposes remote actions but not edit`() {
        val policy = message(isMine = false).contextActionPolicy()

        assertTrue(policy.canReact)
        assertTrue(policy.canReply)
        assertTrue(policy.canCopy)
        assertTrue(policy.canForward)
        assertTrue(policy.canTogglePinned)
        assertTrue(policy.canSelect)
        assertTrue(policy.canViewInfo)
        assertTrue(policy.canDelete)
        assertFalse(policy.canEdit)
    }

    @Test
    fun `failed local message cannot expose remote actions`() {
        val policy = message(serverId = null, status = MessageStatus.FAILED).contextActionPolicy()

        assertFalse(policy.canReact)
        assertFalse(policy.canReply)
        assertFalse(policy.canForward)
        assertFalse(policy.canTogglePinned)
        assertFalse(policy.canEdit)
        assertFalse(policy.canViewInfo)
        assertTrue(policy.canCopy)
        assertTrue(policy.canSelect)
        assertTrue(policy.canDelete)
    }

    @Test
    fun `deleted message exposes no destructive or peer actions`() {
        val policy = message(content = MessageContent.Deleted, deletedAt = 12L).contextActionPolicy()

        assertFalse(policy.canReact)
        assertFalse(policy.canReply)
        assertFalse(policy.canCopy)
        assertFalse(policy.canForward)
        assertFalse(policy.canTogglePinned)
        assertFalse(policy.canEdit)
        assertFalse(policy.canSelect)
        assertTrue(policy.canViewInfo)
        assertFalse(policy.canDelete)
    }

    @Test
    fun `secret conversation never exposes forwarding or pinning`() {
        val policy = message().contextActionPolicy(
            MessageContextCapabilities(conversationType = ConversationType.SECRET),
        )

        assertFalse(policy.canForward)
        assertFalse(policy.canTogglePinned)
        assertTrue(policy.canReply)
        assertTrue(policy.canReact)
    }

    @Test
    fun `route capabilities can suppress server-gated actions`() {
        val policy = message().contextActionPolicy(
            MessageContextCapabilities(
                allowsReactions = false,
                allowsForwarding = false,
                allowsPinning = false,
            ),
        )

        assertFalse(policy.canReact)
        assertFalse(policy.canForward)
        assertFalse(policy.canTogglePinned)
    }

    @Test
    fun `incomplete attachment transfer states cannot expose remote actions`() {
        listOf(
            TransferState.QUEUED,
            TransferState.UPLOADING,
            TransferState.FAILED,
            TransferState.CANCELLED,
        ).forEach { transferState ->
            val policy = message(
                attachment = Attachment(
                    kind = AttachmentKind.IMAGE,
                    remoteUrl = null,
                    localUri = "content://local/image",
                    fileName = "image.jpg",
                    mimeType = "image/jpeg",
                    sizeBytes = 42L,
                    progress = 0.4f,
                    transferState = transferState,
                ),
            ).contextActionPolicy()

            assertFalse("$transferState must not react", policy.canReact)
            assertFalse("$transferState must not reply", policy.canReply)
            assertFalse("$transferState must not forward", policy.canForward)
            assertFalse("$transferState must not pin", policy.canTogglePinned)
            assertFalse("$transferState must not edit", policy.canEdit)
            assertTrue(policy.canSelect)
            assertTrue(policy.canViewInfo)
            assertTrue(policy.canDelete)
        }
    }

    @Test
    fun `completed attachment preserves remote actions`() {
        val policy = message(
            attachment = Attachment(
                kind = AttachmentKind.IMAGE,
                remoteUrl = "https://media.example.test/image.jpg",
                localUri = "content://local/image",
                fileName = "image.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 42L,
                progress = 1f,
                transferState = TransferState.COMPLETE,
            ),
        ).contextActionPolicy()

        assertTrue(policy.canReact)
        assertTrue(policy.canReply)
        assertTrue(policy.canForward)
        assertTrue(policy.canTogglePinned)
        assertFalse(policy.canEdit)
        assertTrue(policy.canSelect)
        assertTrue(policy.canViewInfo)
        assertTrue(policy.canDelete)
    }

    private fun message(
        isMine: Boolean = true,
        serverId: String? = "server-id",
        status: MessageStatus = MessageStatus.READ,
        content: MessageContent = MessageContent.Text("context fixture"),
        deletedAt: Long? = null,
        attachment: Attachment? = null,
    ) = Message(
        accountId = "account",
        conversationId = "conversation",
        clientId = "client-id",
        serverId = serverId,
        senderId = if (isMine) "account" else "peer",
        content = content,
        createdAtEpochMillis = 1L,
        deletedAtEpochMillis = deletedAt,
        status = status,
        attachment = attachment,
        isMine = isMine,
    )
}
