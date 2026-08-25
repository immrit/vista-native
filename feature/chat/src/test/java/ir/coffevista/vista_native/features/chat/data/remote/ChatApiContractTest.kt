package ir.coffevista.vista_native.features.chat.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.DELETE
import retrofit2.http.GET

class ChatApiContractTest {
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
    fun groupPermissionUsesBackendCreatorOrMemberAdminInsteadOfMissingAlias() {
        val creatorInfo = GroupInfoDto(id = "g", name = "group", createdBy = "owner")
        assertTrue(creatorInfo.isAdminFor("owner", emptyList()))

        val memberInfo = GroupInfoDto(id = "g", name = "group")
        val members = listOf(GroupMemberDto(userId = "admin", isAdmin = true))
        assertTrue(memberInfo.isAdminFor("admin", members))
        assertFalse(memberInfo.isAdminFor("member", members))
    }
}
