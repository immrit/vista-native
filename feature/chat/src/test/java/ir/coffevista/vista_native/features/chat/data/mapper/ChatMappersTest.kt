package ir.coffevista.vista_native.features.chat.data.mapper

import ir.coffevista.vista_native.features.chat.data.remote.MessageDto
import ir.coffevista.vista_native.features.chat.data.remote.ConversationDto
import ir.coffevista.vista_native.features.chat.data.remote.ConversationParticipantDto
import ir.coffevista.vista_native.features.chat.data.remote.ProfileDto
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.repository.ChatContentCipher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMappersTest {
    private val cipher = object : ChatContentCipher {
        override suspend fun encrypt(accountId: String, conversationId: String, recordId: String, plaintext: String) =
            plaintext.encodeToByteArray()

        override suspend fun decrypt(accountId: String, conversationId: String, recordId: String, ciphertext: ByteArray) =
            ciphertext.decodeToString()
    }

    @Test
    fun `server read flags map to highest monotonic status`() {
        val message = dto().copy(isRead = true).toDomain("account")
        assertEquals(MessageStatus.READ, message.status)
    }

    @Test
    fun `secret payload never maps ciphertext into UI text`() {
        val message = dto().copy(isSecret = true, content = "VE2E1:ciphertext").toDomain("account")
        assertEquals(MessageContent.EncryptedUnavailable, message.content)
    }

    @Test
    fun `secret conversation preview never exposes ciphertext`() = runTest {
        val entity = ConversationDto(
            id = "conversation-1",
            name = "sara84",
            conversationType = "secret",
            lastMessageText = "VE2E2:ciphertext",
        ).toEntity("account", cipher, 10)

        assertEquals("🔒 پیام محرمانه", entity.toDomain(cipher).lastMessage)
    }

    @Test
    fun `legacy unprefixed base64 conversation preview never reaches inbox`() = runTest {
        val entity = ConversationDto(
            id = "conversation-legacy-cipher",
            name = "mahshid",
            lastMessageText = "8YHOwA4MIJclaktvdNT8hscH0XQ4A+vBEkElz9EC2mBGMH7mQv0X",
        ).toEntity("account", cipher, 10)

        assertEquals("پیام جدید", entity.toDomain(cipher).lastMessage)
    }

    @Test
    fun `document conversation preview matches Flutter media label`() = runTest {
        val entity = ConversationDto(
            id = "conversation-file",
            name = "mahshid",
            lastMessage = "",
            lastMessageType = "document",
        ).toEntity("account", cipher, 10)

        assertEquals("📎 فایل", entity.toDomain(cipher).lastMessage)
    }

    @Test
    fun `secret audio preview exposes type but never content`() = runTest {
        val entity = ConversationDto(
            id = "conversation-secret-audio",
            conversationType = "secret",
            lastMessageText = "VE2E2:ciphertext",
            lastMessageType = "audio",
        ).toEntity("account", cipher, 10)

        assertEquals("🔒 پیام صوتی", entity.toDomain(cipher).lastMessage)
    }

    @Test
    fun `entity round trip uses cipher boundary and preserves identity`() = runTest {
        val original = dto().toDomain("account").copy(isMine = true)
        val restored = original.toEntity(cipher, 10).toDomain(cipher)
        assertEquals(original.clientId, restored.clientId)
        assertEquals(original.serverId, restored.serverId)
        assertTrue(restored.content is MessageContent.Text)
    }

    @Test
    fun `profile identity accepts singular endpoint id alias`() {
        val profile = ProfileDto(id = "peer-1", username = "sara84")

        assertEquals("peer-1", profile.resolvedUserId)
    }

    @Test
    fun `profile identity prefers canonical batch user id`() {
        val profile = ProfileDto(userId = "peer-1", id = "legacy-peer")

        assertEquals("peer-1", profile.resolvedUserId)
    }

    @Test
    fun `relative avatar storage key resolves to Flutter CDN host`() {
        assertEquals(
            "https://s3.coffevista.ir/avatars/sara84.jpg",
            "/avatars/sara84.jpg".resolvedAvatarUrl(),
        )
    }

    @Test
    fun `absolute avatar URL remains unchanged`() {
        val absolute = "https://media.example.test/avatar.jpg"

        assertEquals(absolute, absolute.resolvedAvatarUrl())
    }

    @Test
    fun `Flutter bundled avatar source remains available to Native renderer`() {
        val asset = "lib/utils/images/vistalogo-new.png"

        assertEquals(asset, asset.resolvedAvatarUrl())
    }

    @Test
    fun `Flutter group chat alias maps to group instead of private`() = runTest {
        val entity = ConversationDto(
            id = "group-1",
            name = "گروه نمونه",
            conversationType = "group_chat",
        ).toEntity("account", cipher, 10)

        assertEquals(ConversationType.GROUP, entity.toDomain(cipher).type)
    }

    @Test
    fun `Flutter participant profile aliases hydrate direct peer identity`() = runTest {
        val entity = ConversationDto(
            id = "conversation-service",
            conversationType = "private",
            conversationParticipants = listOf(
                ConversationParticipantDto(userId = "account", unreadCount = 4),
                ConversationParticipantDto(
                    userId = "service-peer",
                    profiles = buildJsonArray {
                        add(buildJsonObject {
                            put("username", "vista_service")
                            put("avatar_url", "/avatars/vista-service.png")
                        })
                    },
                ),
            ),
        ).toEntity("account", cipher, 10)

        val conversation = entity.toDomain(cipher)
        assertEquals("service-peer", conversation.peerId)
        assertEquals("vista_service", conversation.title)
        assertEquals("https://s3.coffevista.ir/avatars/vista-service.png", conversation.avatarUrl)
        assertEquals(4, conversation.unreadCount)
    }

    private fun dto() = MessageDto(
        id = "message-1",
        conversationId = "conversation-1",
        senderId = "account",
        content = "fixture",
        createdAt = "2026-08-06T00:00:00Z",
        isSent = true,
    )
}
