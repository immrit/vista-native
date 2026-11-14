package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import ir.coffevista.vista_native.core.designsystem.audio.VistaAudioKind
import ir.coffevista.vista_native.core.designsystem.audio.VistaAudioPlayer
import ir.coffevista.vista_native.core.designsystem.audio.VistaAudioTrack

val LocalMusicPlaylist = compositionLocalOf<List<VistaAudioTrack>> { emptyList() }

fun vistaPostMusicTrack(
    postId: String,
    musicUrl: String,
    title: String?,
    artist: String?,
    artworkUrl: String?,
    playlistTitle: String?,
): VistaAudioTrack {
    val clean = musicUrl.trim()
    val effectiveTitle = title?.takeIf(String::isNotBlank) ?: resolveMusicTitle(null, clean)
    val who = artist?.takeIf(String::isNotBlank)
    return VistaAudioTrack(
        id = "post:$postId",
        url = clean,
        kind = VistaAudioKind.MUSIC,
        title = effectiveTitle,
        artist = who,
        durationMs = 0L,
        senderName = who ?: "ویستا",
        conversationId = "",
        createdAtEpochMillis = 0L,
        conversationTitle = playlistTitle,
        postId = postId,
        artworkUrl = artworkUrl,
    )
}

/**
 * کارت یکپارچه پخش موسیقی پست‌ها (مشترک بین فید، پروفایل، جزئیات پست و ساخت پست)
 * متصل به موتور سراسری [VistaAudioPlayer]، مینی‌پلیر و کنترل نوتیفیکیشن/قفل‌صفحه.
 */
@Composable
fun VistaPostMusicBubble(
    musicUrl: String,
    musicTitle: String? = null,
    modifier: Modifier = Modifier,
    subtitle: String = "موسیقی پست",
    artist: String? = null,
    postId: String? = null,
    trackId: String = postId?.let { "post:$it" } ?: musicUrl.trim(),
    artworkUrl: String? = null,
    playlistTitle: String? = null,
    musicQueue: List<VistaAudioTrack> = emptyList(),
    trailingAction: (@Composable () -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,
) {
    val cleanUrl = musicUrl.trim()
    if (cleanUrl.isEmpty()) return

    val context = LocalContext.current
    LaunchedEffect(context) { VistaAudioPlayer.attach(context) }

    val effectiveTitle = remember(musicTitle, cleanUrl) {
        musicTitle?.takeIf(String::isNotBlank) ?: resolveMusicTitle(null, cleanUrl)
    }
    val resolvedArtist = remember(artist, subtitle) {
        artist?.takeIf(String::isNotBlank)
            ?: subtitle.takeIf { it.isNotBlank() && it != "موسیقی پست" }
    }

    val track = remember(
        trackId,
        cleanUrl,
        effectiveTitle,
        resolvedArtist,
        subtitle,
        playlistTitle,
        postId,
        artworkUrl,
    ) {
        VistaAudioTrack(
            id = trackId,
            url = cleanUrl,
            kind = VistaAudioKind.MUSIC,
            title = effectiveTitle,
            artist = resolvedArtist,
            durationMs = 0L,
            senderName = resolvedArtist ?: subtitle.ifBlank { "ویستا" },
            conversationId = "",
            createdAtEpochMillis = 0L,
            conversationTitle = playlistTitle,
            postId = postId,
            artworkUrl = artworkUrl,
        )
    }

    val playlist = LocalMusicPlaylist.current
    val playerState by VistaAudioPlayer.state.collectAsState()
    val positionMs by VistaAudioPlayer.position.collectAsState()

    val isActive = playerState.isActive(trackId)
    val isPlaying = isActive && playerState.isPlaying
    val isBuffering = isActive && playerState.isBuffering
    val currentPositionMs = if (isActive) positionMs else VistaAudioPlayer.savedPositionMs(trackId)
    val durationMs = if (isActive && playerState.durationMs > 0L) playerState.durationMs else 0L

    val seed = remember(cleanUrl, effectiveTitle) {
        (cleanUrl.hashCode().toLong() shl 16) xor effectiveTitle.hashCode().toLong()
    }
    val progress = if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    VistaMusicPlayerCard(
        title = effectiveTitle,
        subtitle = subtitle,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        progress = progress,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        seed = seed,
        onPlayPauseClick = {
            val queue = musicQueue.takeIf { it.isNotEmpty() } ?: playlist.takeIf { it.isNotEmpty() } ?: listOf(track)
            VistaAudioPlayer.toggle(track, queue)
        },
        onSeek = { fraction ->
            val queue = musicQueue.takeIf { it.isNotEmpty() } ?: playlist.takeIf { it.isNotEmpty() } ?: listOf(track)
            VistaAudioPlayer.seekAndPlay(track, queue, fraction)
        },
        trailingAction = trailingAction,
        onCardClick = onCardClick,
        modifier = modifier,
    )
}


