package ir.coffevista.vista_native.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import ir.coffevista.vista_native.core.designsystem.audio.VistaAudioKind
import ir.coffevista.vista_native.core.designsystem.audio.VistaAudioPlayer
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassState
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioPlayerSheet
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatMiniAudioPlayer

@Composable
fun isGlobalMiniPlayerActive(): Boolean {
    val playerState by VistaAudioPlayer.state.collectAsState()
    return playerState.track != null
}

/**
 * محتوای مینی‌پلیر شیشه‌ای سراسری که درون اپ‌بار یا هدر هر صفحه تزریق می‌شود
 * تا همراه با اپ‌بار اسکرول/محو شود و با لمس نوتیفیکیشن نیز بتواند پلیر کامل را باز کند.
 */
@Composable
fun GlobalMiniPlayerSlotContent(
    glassState: VistaGlassState?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    onOpenChat: (conversationId: String, messageId: String?) -> Unit,
    onOpenPost: (postId: String) -> Unit = {},
) {
    val playerState by VistaAudioPlayer.state.collectAsState()
    val openNotificationRequest by VistaAudioPlayer.openPlayerSheetRequests.collectAsState()
    var fullPlayerVisible by remember { mutableStateOf(false) }

    val activeTrack = playerState.track

    LaunchedEffect(openNotificationRequest) {
        if (openNotificationRequest > 0L) {
            val current = VistaAudioPlayer.state.value.track ?: return@LaunchedEffect
            if (current.kind == VistaAudioKind.VOICE && current.conversationId.isNotBlank()) {
                onOpenChat(current.conversationId, current.id)
            } else {
                fullPlayerVisible = true
            }
        }
    }

    ChatMiniAudioPlayer(
        glassState = glassState,
        onOpenFullPlayer = { fullPlayerVisible = true },
        onJumpToMessage = { messageId ->
            if (activeTrack != null && activeTrack.conversationId.isNotBlank()) {
                onOpenChat(activeTrack.conversationId, messageId)
            } else if (activeTrack?.postId != null) {
                fullPlayerVisible = true
            }
        },
        modifier = modifier,
        shape = shape,
    )

    if (fullPlayerVisible && activeTrack != null && activeTrack.kind == VistaAudioKind.MUSIC) {
        ChatAudioPlayerSheet(
            onDismiss = { fullPlayerVisible = false },
            onJumpToMessage = activeTrack.conversationId.takeIf(String::isNotBlank)?.let { convId ->
                { targetMessageId ->
                    fullPlayerVisible = false
                    onOpenChat(convId, targetMessageId)
                }
            },
            onJumpToPost = { postId ->
                fullPlayerVisible = false
                onOpenPost(postId)
            },
        )
    }
}
