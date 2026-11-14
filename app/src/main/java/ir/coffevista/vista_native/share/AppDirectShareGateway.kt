package ir.coffevista.vista_native.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import ir.coffevista.vista_native.core.designsystem.component.VistaDirectShareGateway
import ir.coffevista.vista_native.core.designsystem.component.VistaSharePostPayload
import ir.coffevista.vista_native.core.designsystem.component.VistaShareRecipient
import ir.coffevista.vista_native.features.chat.domain.model.ChatUser
import ir.coffevista.vista_native.features.chat.domain.model.SharedPostDraft
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.chat.domain.repository.ChatResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class AppDirectShareGateway @Inject constructor(
    private val chatRepository: ChatRepository,
) : VistaDirectShareGateway {

    override fun observeFrequentRecipients(limit: Int): Flow<List<VistaShareRecipient>> =
        chatRepository.observeFrequentShareTargets(limit).map { users ->
            users.map { it.toShareRecipient() }
        }

    override suspend fun refreshFrequentRecipients() {
        runCatching { chatRepository.refreshSuggestedUsers() }
    }

    override suspend fun searchRecipients(query: String): List<VistaShareRecipient> {
        val result = chatRepository.searchUsers(query)
        return when (result) {
            is ChatResult.Success -> result.value.map { it.toShareRecipient() }
            is ChatResult.Failure -> emptyList()
        }
    }

    override suspend fun sendPost(
        recipient: VistaShareRecipient,
        post: VistaSharePostPayload,
    ): Boolean {
        val conversationId = resolveConversationId(recipient) ?: return false
        val draft = SharedPostDraft(
            postId = post.postId,
            authorName = post.authorName,
            authorUsername = post.authorUsername,
            authorAvatar = post.authorAvatar,
            content = post.content,
            mediaUrls = post.mediaUrls,
            postVideoUrl = post.postVideoUrl,
            musicUrl = post.musicUrl,
            musicTitle = post.musicTitle,
            likesCount = post.likesCount,
            commentsCount = post.commentsCount,
            createdAt = post.createdAt,
            isVerified = post.isVerified,
            verificationType = post.verificationType,
            role = post.role,
            hashtags = post.hashtags,
            authorId = post.authorId,
        )
        return chatRepository.sendSharedPost(conversationId, draft) is ChatResult.Success
    }

    override suspend fun sendText(
        recipient: VistaShareRecipient,
        text: String,
    ): Boolean {
        val conversationId = resolveConversationId(recipient) ?: return false
        return chatRepository.sendText(conversationId, text) is ChatResult.Success
    }

    override suspend fun loadAvatarBitmap(context: Context, url: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(160)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request) as? SuccessResult ?: return@runCatching null
                val drawable = result.drawable
                if (drawable is BitmapDrawable && drawable.bitmap != null) {
                    drawable.bitmap.asImageBitmap()
                } else {
                    val w = drawable.intrinsicWidth.coerceAtLeast(96)
                    val h = drawable.intrinsicHeight.coerceAtLeast(96)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, w, h)
                    drawable.draw(canvas)
                    bmp.asImageBitmap()
                }
            }.getOrNull()
        }

    private suspend fun resolveConversationId(recipient: VistaShareRecipient): String? {
        val existingId = recipient.conversationId?.trim()?.takeIf(String::isNotEmpty)
        if (existingId != null) return existingId
        val found = chatRepository.findConversationByPeer(recipient.id, isSecret = false)?.id
        if (!found.isNullOrBlank()) return found
        return when (val created = chatRepository.createConversation(recipient.id, isSecret = false)) {
            is ChatResult.Success -> created.value.id
            is ChatResult.Failure -> null
        }
    }
}

private fun ChatUser.toShareRecipient(): VistaShareRecipient = VistaShareRecipient(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    conversationId = conversationId,
    isVerified = isVerified,
    verificationType = verificationType,
    role = role,
)
