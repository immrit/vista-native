package ir.coffevista.vista_native.features.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @GET("v1/presence/{userId}")
    suspend fun presence(@Path("userId") userId: String): PresenceDto
    @GET("v1/chat/conversations")
    suspend fun conversations(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String?,
    ): ConversationPageDto

    @GET("v1/chat/conversations/{conversationId}")
    suspend fun conversation(
        @Path("conversationId") conversationId: String,
    ): ConversationDto

    @DELETE("v1/chat/conversations/{conversationId}")
    suspend fun deleteConversation(@Path("conversationId") conversationId: String)

    @GET("v1/chat/conversations/{conversationId}/messages")
    suspend fun messages(
        @Path("conversationId") conversationId: String,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String?,
    ): MessagePageDto

    @POST("v1/chat/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body request: SendMessageRequest,
    ): MessageDto

    @POST("v1/chat/conversations/{conversationId}/read")
    suspend fun markRead(@Path("conversationId") conversationId: String)

    @POST("v1/chat/conversations/{conversationId}/typing")
    suspend fun typing(@Path("conversationId") conversationId: String)

    @POST("v1/chat/conversations/{conversationId}/active")
    suspend fun setActive(
        @Path("conversationId") conversationId: String,
        @Body request: ActiveConversationRequest,
    )

    @POST("v1/chat/conversations/{conversationId}/{flag}")
    suspend fun toggleConversationFlag(
        @Path("conversationId") conversationId: String,
        @Path("flag") flag: String,
    ): ConversationDto

    @POST("v1/chat/conversations/{conversationId}/accept")
    suspend fun acceptMessageRequest(@Path("conversationId") conversationId: String)

    @POST("v1/chat/conversations/{conversationId}/reject")
    suspend fun rejectMessageRequest(@Path("conversationId") conversationId: String)

    @GET("v1/chat/conversations/{conversationId}/search")
    suspend fun searchMessages(
        @Path("conversationId") conversationId: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 50,
    ): MessagePageDto

    @PUT("v1/chat/messages/{messageId}")
    suspend fun editMessage(
        @Path("messageId") messageId: String,
        @Body request: EditMessageRequest,
    )

    @POST("v1/chat/messages/{messageId}/pin")
    suspend fun pinMessage(@Path("messageId") messageId: String)

    @DELETE("v1/chat/messages/{messageId}/pin")
    suspend fun unpinMessage(@Path("messageId") messageId: String)

    @GET("v1/chat/conversations/{conversationId}/pinned")
    suspend fun getPinnedMessages(
        @Path("conversationId") conversationId: String,
    ): MessagePageDto

    @DELETE("v1/chat/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("messageId") messageId: String,
        @Query("for_everyone") forEveryone: Boolean,
    )

    @POST("v1/chat/messages/{messageId}/forward")
    suspend fun forwardMessage(
        @Path("messageId") messageId: String,
        @Body request: ForwardMessageRequest,
    ): MessageDto

    @POST("v1/chat/messages/{messageId}/reactions")
    suspend fun toggleReaction(
        @Path("messageId") messageId: String,
        @Body request: ReactionRequest,
    ): ReactionUpdateDto

    @POST("v1/chat/conversations")
    suspend fun createConversation(@Body request: CreateConversationRequest): ConversationDto

    @POST("v1/chat/groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): GroupCreatedDto

    @GET("v1/chat/groups/{conversationId}")
    suspend fun group(@Path("conversationId") conversationId: String): GroupInfoDto

    @PATCH("v1/chat/groups/{conversationId}")
    suspend fun updateGroup(
        @Path("conversationId") conversationId: String,
        @Body request: UpdateGroupRequest,
    )

    @GET("v1/chat/groups/{conversationId}/members")
    suspend fun groupMembers(@Path("conversationId") conversationId: String): GroupMembersResponse

    @POST("v1/chat/groups/{conversationId}/members")
    suspend fun addGroupMembers(
        @Path("conversationId") conversationId: String,
        @Body request: GroupMembersRequest,
    ): AddedMembersResponse

    @DELETE("v1/chat/groups/{conversationId}/members/{memberId}")
    suspend fun removeGroupMember(
        @Path("conversationId") conversationId: String,
        @Path("memberId") memberId: String,
    )

    @POST("v1/chat/groups/{conversationId}/members/{memberId}/admin")
    suspend fun setGroupAdmin(
        @Path("conversationId") conversationId: String,
        @Path("memberId") memberId: String,
        @Body request: SetGroupAdminRequest,
    )

    @GET("v1/chat/groups/{conversationId}/invite")
    suspend fun groupInvite(@Path("conversationId") conversationId: String): GroupInviteDto

    @POST("v1/chat/groups/{conversationId}/invite")
    suspend fun updateGroupInvite(
        @Path("conversationId") conversationId: String,
        @Body request: GroupInviteRequest,
    ): GroupInviteDto

    @POST("v1/me/block")
    suspend fun blockUser(@Body request: TargetUserRequest)

    @POST("v1/me/unblock")
    suspend fun unblockUser(@Body request: TargetUserRequest)

    @GET("v1/me/block-status/{userId}")
    suspend fun blockStatus(@Path("userId") userId: String): BlockStatusDto

    @POST("v1/profiles/report")
    suspend fun reportUser(@Body request: ReportUserRequest)

    @POST("v1/chat/groups/join/{code}")
    suspend fun joinGroup(@Path("code") code: String): GroupCreatedDto

    @POST("v1/chat/groups/{conversationId}/leave")
    suspend fun leaveGroup(@Path("conversationId") conversationId: String)

    @HTTP(method = "DELETE", path = "v1/chat/groups/{conversationId}", hasBody = false)
    suspend fun deleteGroup(@Path("conversationId") conversationId: String)

    @POST("v1/uploads/presign")
    suspend fun presignUpload(@Body request: PresignUploadRequest): PresignUploadResponse

    @POST("v1/uploads/delete")
    suspend fun deleteUpload(@Body request: DeleteUploadRequest): UploadDeleteResponse

    /** Existing profile contract used by Flutter to resolve 15MB/100MB/unlimited upload limits. */
    @GET("v1/me/profile")
    suspend fun ownUploadProfile(): UploadPrivilegeProfileDto

    @POST("v1/profiles/batch")
    suspend fun profilesBatch(@Body request: ProfileBatchRequest): ProfileBatchResponse

    @GET("v1/profiles/{userId}")
    suspend fun profile(
        @Path("userId") userId: String,
    ): ProfileDto

    @GET("v1/profiles/by-username/{username}")
    suspend fun profileByUsername(
        @Path("username") username: String,
    ): ProfileDto

    @GET("v1/profiles/search")
    suspend fun searchProfiles(
        @Query("q") query: String,
        @Query("limit") limit: Int = 50,
    ): ProfileBatchResponse

    @GET("v1/me/note")
    suspend fun ownProfileNote(): ProfileNoteDto

    @POST("v1/me/note")
    suspend fun upsertOwnProfileNote(@Body request: UpsertProfileNoteRequest): ProfileNoteDto

    @DELETE("v1/me/note")
    suspend fun deleteOwnProfileNote()

    @POST("v1/profiles/notes/batch")
    suspend fun profileNotesBatch(
        @Body request: ProfileBatchRequest,
    ): Map<String, ProfileNoteDto>
}

@Serializable
data class PresenceDto(
    @SerialName("user_id") val userId: String,
    val status: String = "offline",
    @SerialName("last_online_at") val lastOnlineAt: String? = null,
    val visibility: String = "everyone",
    @SerialName("can_view_last_seen") val canViewLastSeen: Boolean = false,
)

@Serializable
data class ConversationPageDto(
    val conversations: List<ConversationDto> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ConversationDto(
    val id: String,
    @SerialName("peer_id") val peerId: String? = null,
    @SerialName("conversation_type") val conversationType: String = "private",
    val type: String? = null,
    val name: String? = null,
    val image: String? = null,
    @SerialName("peer_username") val peerUsername: String? = null,
    @SerialName("other_user_username") val otherUserUsername: String? = null,
    val username: String? = null,
    @SerialName("peer_full_name") val peerFullName: String? = null,
    @SerialName("other_user_full_name") val otherUserFullName: String? = null,
    @SerialName("peer_avatar_url") val peerAvatarUrl: String? = null,
    @SerialName("other_user_avatar") val otherUserAvatar: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("conversation_participants")
    val conversationParticipants: List<ConversationParticipantDto> = emptyList(),
    val participants: List<ConversationParticipantDto> = emptyList(),
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("last_message_time") val lastMessageTime: String? = null,
    @SerialName("last_message_text") val lastMessageText: String? = null,
    @SerialName("last_message") val lastMessage: String? = null,
    @SerialName("last_message_type") val lastMessageType: String? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("is_muted") val isMuted: Boolean = false,
    @SerialName("is_secret") val isSecret: Boolean = false,
    val status: String? = null,
    @SerialName("request_status") val requestStatus: String? = null,
)

@Serializable
data class ConversationParticipantDto(
    @SerialName("user_id") val userId: String? = null,
    val id: String? = null,
    @SerialName("unread_count") val unreadCount: Int? = null,
    val profiles: JsonElement? = null,
    val profile: JsonElement? = null,
)

@Serializable
data class MessagePageDto(
    val messages: List<MessageDto> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean? = null,
)

internal fun MessagePageDto.hasMoreForPagination(): Boolean =
    hasMore ?: !nextCursor.isNullOrBlank()

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("message_type") val messageType: String = "text",
    val content: String = "",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("attachment_file_name") val attachmentFileName: String? = null,
    @SerialName("attachment_mime_type") val attachmentMimeType: String? = null,
    @SerialName("attachment_size_bytes") val attachmentSizeBytes: Long? = null,
    @SerialName("audio_title") val audioTitle: String? = null,
    @SerialName("audio_artist") val audioArtist: String? = null,
    @SerialName("audio_album") val audioAlbum: String? = null,
    @SerialName("media_group_id") val mediaGroupId: String? = null,
    val duration: Int? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("is_sent") val isSent: Boolean = true,
    @SerialName("is_delivered") val isDelivered: Boolean = false,
    @SerialName("is_seen") val isSeen: Boolean = false,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("is_secret") val isSecret: Boolean = false,
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerialName("reply_to_content") val replyToContent: String? = null,
    @SerialName("reply_to_sender_name") val replyToSenderName: String? = null,
    @SerialName("reply_to_kind") val replyToKind: String? = null,
    @SerialName("is_forwarded") val isForwarded: Boolean = false,
    @SerialName("original_sender_id") val originalSenderId: String? = null,
    @SerialName("original_message_id") val originalMessageId: String? = null,
    @SerialName("forwarded_from_sender_name") val forwardedFromSenderName: String? = null,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    val reactions: List<ReactionDto> = emptyList(),
)

@Serializable
data class SendMessageRequest(
    val id: String,
    val content: String,
    @SerialName("message_type") val messageType: String = "text",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerialName("reply_to_content") val replyToContent: String? = null,
    @SerialName("reply_to_sender_name") val replyToSenderName: String? = null,
    @SerialName("reply_to_kind") val replyToKind: String? = null,
    @SerialName("attachment_file_name") val attachmentFileName: String? = null,
    @SerialName("attachment_mime_type") val attachmentMimeType: String? = null,
    @SerialName("attachment_size_bytes") val attachmentSizeBytes: Long? = null,
    @SerialName("audio_title") val audioTitle: String? = null,
    @SerialName("audio_artist") val audioArtist: String? = null,
    @SerialName("audio_album") val audioAlbum: String? = null,
    @SerialName("media_group_id") val mediaGroupId: String? = null,
    val duration: Int? = null,
)

@Serializable data class ActiveConversationRequest(val active: Boolean)
@Serializable data class UnpinMessageRequest(val message_id: String)
@Serializable data class TargetUserRequest(val target_user_id: String)
@Serializable data class ReportUserRequest(
    @SerialName("user_id") val userId: String,
    val reason: String,
    @SerialName("additional_details") val additionalDetails: String? = null,
)
@Serializable data class BlockStatusDto(
    @SerialName("is_blocked") val isBlocked: Boolean = false,
    @SerialName("is_blocked_by") val isBlockedBy: Boolean = false,
    @SerialName("blocked_at") val blockedAt: String? = null,
    @SerialName("blocked_by_at") val blockedByAt: String? = null,
)
@Serializable data class EditMessageRequest(val content: String)
@Serializable data class ForwardMessageRequest(@SerialName("target_conversation_id") val targetConversationId: String)
@Serializable data class ReactionRequest(val emoji: String)
@Serializable data class CreateConversationRequest(@SerialName("peer_id") val peerId: String, @SerialName("is_secret") val isSecret: Boolean = false)
@Serializable data class CreateGroupRequest(
    val name: String,
    @SerialName("member_ids") val memberIds: List<String>,
    @SerialName("image_url") val imageUrl: String? = null,
)
@Serializable data class GroupCreatedDto(val id: String)
@Serializable data class UpdateGroupRequest(
    val name: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
)
@Serializable data class GroupMembersRequest(@SerialName("member_ids") val memberIds: List<String>)
@Serializable data class AddedMembersResponse(val added: Int = 0)
@Serializable data class SetGroupAdminRequest(@SerialName("make_admin") val makeAdmin: Boolean)
@Serializable data class GroupInviteRequest(val enabled: Boolean? = null)
@Serializable data class GroupInviteDto(
    @SerialName("invite_code") val inviteCode: String? = null,
    val enabled: Boolean = true,
    @SerialName("invite_enabled") val inviteEnabled: Boolean? = null,
)
@Serializable data class GroupMembersResponse(val members: List<GroupMemberDto> = emptyList())
@Serializable data class GroupMemberDto(
    @SerialName("user_id") val userId: String,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("joined_at") val joinedAt: String? = null,
)

@Serializable
data class ReactionDto(
    @SerialName("user_id") val userId: String,
    val emoji: String,
)

@Serializable
data class ReactionUpdateDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("conversation_id") val conversationId: String,
    val reactions: List<ReactionDto> = emptyList(),
)

@Serializable
data class GroupInfoDto(
    val id: String,
    val name: String,
    val image: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("max_members") val maxMembers: Int = 0,
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("invite_enabled") val inviteEnabled: Boolean = false,
    @SerialName("is_admin") val isAdmin: Boolean = false,
)

internal fun GroupInfoDto.isAdminFor(
    accountId: String,
    members: List<GroupMemberDto>,
): Boolean = isAdmin ||
    createdBy?.trim() == accountId.trim() ||
    members.any { it.userId.trim() == accountId.trim() && it.isAdmin }

@Serializable
data class PresignUploadRequest(
    @SerialName("object_key") val objectKey: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("file_size") val fileSize: Long,
)

@Serializable
data class PresignUploadResponse(
    val url: String,
    val method: String = "PUT",
    val headers: Map<String, String> = emptyMap(),
    @SerialName("object_key") val objectKey: String,
    @SerialName("object_url") val objectUrl: String,
)

@Serializable
data class DeleteUploadRequest(@SerialName("object_key") val objectKey: String)

@Serializable
data class UploadDeleteResponse(val success: Boolean = false)

@Serializable
data class UploadPrivilegeProfileDto(
    val role: String? = null,
    @SerialName("account_type") val accountType: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("verification_type") val verificationType: String? = null,
    @SerialName("premium_days_remaining") val premiumDaysRemaining: Int? = null,
    @SerialName("subscription_expires_at") val subscriptionExpiresAt: String? = null,
)

@Serializable
data class ProfileBatchRequest(@SerialName("user_ids") val userIds: List<String>)

@Serializable
data class ProfileBatchResponse(val profiles: List<ProfileDto> = emptyList())

@Serializable
data class ProfileDto(
    @SerialName("user_id") val userId: String = "",
    val id: String? = null,
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
) {
    val resolvedUserId: String
        get() = userId.trim().ifEmpty { id?.trim().orEmpty() }
}

@Serializable
data class ProfileNoteDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class UpsertProfileNoteRequest(val content: String)
