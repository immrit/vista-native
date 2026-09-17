package ir.coffevista.vista_native.features.chat.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

class ChatApiContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun deleteConversationUsesFlutterBackendContract() {
        val method = ChatApi::class.java.declaredMethods.single { it.name == "deleteConversation" }
        val annotation = requireNotNull(method.getAnnotation(DELETE::class.java))

        assertEquals("v1/chat/conversations/{conversationId}", annotation.value)
    }

    @Test
    fun presenceUsesExistingBackendContract() {
        val method = ChatApi::class.java.declaredMethods.single { it.name == "presence" }
        val annotation = requireNotNull(method.getAnnotation(GET::class.java))

        assertEquals("v1/presence/{userId}", annotation.value)
    }

    @Test
    fun suggestedUserFallbackUsesFlutterFollowGraphContracts() {
        val following = ChatApi::class.java.declaredMethods.single { it.name == "followingProfiles" }
        val followers = ChatApi::class.java.declaredMethods.single { it.name == "followerProfiles" }

        assertEquals(
            "v1/profiles/following/{userId}",
            requireNotNull(following.getAnnotation(GET::class.java)).value,
        )
        assertEquals(
            "v1/profiles/followers/{userId}",
            requireNotNull(followers.getAnnotation(GET::class.java)).value,
        )
    }

    @Test
    fun profileListsAcceptBothFlutterAndNativeResponseEnvelopes() {
        val flutterEnvelope = json.decodeFromString(
            ProfileBatchResponse.serializer(),
            """{"profiles":[{"user_id":"flutter-user"}]}""",
        )
        val nativeEnvelope = json.decodeFromString(
            ProfileBatchResponse.serializer(),
            """{"users":[{"user_id":"native-user"}]}""",
        )

        assertEquals("flutter-user", flutterEnvelope.resolvedProfiles.single().resolvedUserId)
        assertEquals("native-user", nativeEnvelope.resolvedProfiles.single().resolvedUserId)
    }

    @Test
    fun messageRequestActionsUseFlutterBackendContracts() {
        val accept = ChatApi::class.java.declaredMethods.single { it.name == "acceptMessageRequest" }
        val reject = ChatApi::class.java.declaredMethods.single { it.name == "rejectMessageRequest" }

        assertEquals(
            "v1/chat/conversations/{conversationId}/accept",
            requireNotNull(accept.getAnnotation(POST::class.java)).value,
        )
        assertEquals(
            "v1/chat/conversations/{conversationId}/reject",
            requireNotNull(reject.getAnnotation(POST::class.java)).value,
        )
    }

    @Test
    fun missingHasMoreFallsBackToNextCursorLikeFlutter() {
        assertTrue(MessagePageDto(nextCursor = "cursor-2").hasMoreForPagination())
        assertFalse(MessagePageDto(nextCursor = null).hasMoreForPagination())
    }

    @Test
    fun explicitHasMoreOverridesCursorFallback() {
        assertFalse(MessagePageDto(nextCursor = "cursor-2", hasMore = false).hasMoreForPagination())
        assertTrue(MessagePageDto(nextCursor = null, hasMore = true).hasMoreForPagination())
    }

    @Test
    fun FlutterMessageRequestWireAliasesAreBothAccepted() {
        val canonical = json.decodeFromString(
            ConversationDto.serializer(),
            """{"id":"canonical","is_message_request":true,"message_request_status":"pending"}""",
        )
        val legacy = json.decodeFromString(
            ConversationDto.serializer(),
            """{"id":"legacy","message_request":true,"request_status":"pending"}""",
        )

        assertTrue(canonical.isMessageRequest)
        assertEquals("pending", canonical.messageRequestStatus)
        assertTrue(legacy.messageRequest)
        assertEquals("pending", legacy.requestStatus)
    }

    @Test
    fun inboxDeliveryMetadataUsesFlutterWireNames() {
        val conversation = json.decodeFromString(
            ConversationDto.serializer(),
            """{"id":"delivery","last_message_sender_id":"account","is_last_message_from_me":true,"last_message_delivery_status":"read","last_message_is_read":true}""",
        )

        assertEquals("account", conversation.lastMessageSenderId)
        assertEquals(true, conversation.isLastMessageFromMe)
        assertEquals("read", conversation.lastMessageDeliveryStatus)
        assertTrue(conversation.lastMessageIsRead)
    }

    @Test
    fun groupPermissionUsesBackendCreatorOrMemberAdminInsteadOfMissingAlias() {
        val creatorInfo = GroupInfoDto(id = "g", name = "group", createdBy = "owner")
        assertTrue(creatorInfo.isAdminFor("owner", emptyList()))

        val memberInfo = GroupInfoDto(id = "g", name = "group")
        val members = listOf(GroupMemberDto(userId = "admin", isAdmin = true))
        assertTrue(memberInfo.isAdminFor("admin", members))
        assertFalse(memberInfo.isAdminFor("member", members))
    }
}
