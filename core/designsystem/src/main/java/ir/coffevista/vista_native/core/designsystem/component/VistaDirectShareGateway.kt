package ir.coffevista.vista_native.core.designsystem.component

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Immutable
data class VistaShareRecipient(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val conversationId: String? = null,
    val isVerified: Boolean = false,
    val verificationType: String? = null,
    val role: String? = null,
)

@Immutable
data class VistaSharePostPayload(
    val postId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatar: String? = null,
    val content: String = "",
    val mediaUrls: List<String> = emptyList(),
    val postVideoUrl: String? = null,
    val musicUrl: String? = null,
    val musicTitle: String? = null,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val createdAt: String,
    val isVerified: Boolean = false,
    val verificationType: String = "none",
    val role: String? = null,
    val hashtags: List<String> = emptyList(),
    val authorId: String? = null,
)

interface VistaDirectShareGateway {
    fun observeFrequentRecipients(limit: Int = 18): Flow<List<VistaShareRecipient>> = flowOf(emptyList())
    suspend fun refreshFrequentRecipients() {}
    suspend fun searchRecipients(query: String): List<VistaShareRecipient> = emptyList()
    suspend fun sendPost(recipient: VistaShareRecipient, post: VistaSharePostPayload): Boolean = false
    suspend fun sendText(recipient: VistaShareRecipient, text: String): Boolean = false
    suspend fun loadAvatarBitmap(context: Context, url: String): ImageBitmap? = null
}

object VistaDirectShareRegistry {
    @Volatile
    var current: VistaDirectShareGateway? = null
}

val LocalVistaDirectShareGateway = staticCompositionLocalOf<VistaDirectShareGateway?> { null }
