package ir.coffevista.vista_native.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassState
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioKind
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioPlayer
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioPlayerSheet
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatMiniAudioPlayer
import ir.coffevista.vista_native.features.shell.ShellRoutes

private val EXCLUDED_GLOBAL_MINI_PLAYER_ROUTES = setOf(
    ShellRoutes.ChatDetailRoute,
    ShellRoutes.StoryPlayerRoute,
    ShellRoutes.StoryCreate,
    ShellRoutes.StoryCreateRoute,
    ShellRoutes.ReelsRoute,
    ShellRoutes.VideoTrimmerRoute,
    ShellRoutes.QrScanner,
    ShellRoutes.AboutSlideshow,
    ShellRoutes.ServicesWebRoute,
)

/**
 * نوار مینی‌پلیر سراسری با افکت شیشه‌ای لیکویید گلس (`vistaGlass`) زیر اپ‌بار تمام صفحات
 * تا کاربر پس از خروج از صفحه گفتگو هم در هر بخشی از برنامه به کنترل پخش دسترسی داشته باشد.
 */
@Composable
fun GlobalAudioMiniPlayerOverlay(
    glassState: VistaGlassState,
    currentRoute: String?,
    onOpenChat: (conversationId: String, messageId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerState by ChatAudioPlayer.state.collectAsState()
    var fullPlayerVisible by remember { mutableStateOf(false) }

    val activeTrack = playerState.track
    val isExcludedRoute = currentRoute == null ||
        currentRoute in EXCLUDED_GLOBAL_MINI_PLAYER_ROUTES ||
        currentRoute.startsWith("chat_detail")

    if (activeTrack != null && !isExcludedRoute) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 56.dp),
        ) {
            ChatMiniAudioPlayer(
                glassState = glassState,
                onOpenFullPlayer = { fullPlayerVisible = true },
                onJumpToMessage = { messageId ->
                    if (activeTrack.conversationId.isNotBlank()) {
                        onOpenChat(activeTrack.conversationId, messageId)
                    }
                },
            )
        }
    }

    if (fullPlayerVisible && activeTrack != null && activeTrack.kind == ChatAudioKind.MUSIC) {
        ChatAudioPlayerSheet(
            onDismiss = { fullPlayerVisible = false },
            onJumpToMessage = activeTrack.conversationId.takeIf(String::isNotBlank)?.let { convId ->
                { targetMessageId ->
                    fullPlayerVisible = false
                    onOpenChat(convId, targetMessageId)
                }
            },
        )
    }
}
