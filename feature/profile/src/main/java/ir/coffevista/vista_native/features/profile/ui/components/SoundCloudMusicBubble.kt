package ir.coffevista.vista_native.features.profile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ir.coffevista.vista_native.core.designsystem.audio.VistaAudioTrack
import ir.coffevista.vista_native.core.designsystem.component.VistaPostMusicBubble

/**
 * Resolves optional start/end ms from musicUrl fragment (#t=start,end) or query params.
 */
fun resolveMusicRange(musicUrl: String): Pair<Long, Long?> =
    ir.coffevista.vista_native.core.designsystem.component.resolveMusicRange(musicUrl)

/**
 * کارت پخش موسیقی پست‌های پروفایل متصل به موتور سراسری پخش صدا و مینی‌پلیر ویستا.
 */
@Composable
fun SoundCloudMusicBubble(
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
    VistaPostMusicBubble(
        musicUrl = musicUrl,
        musicTitle = musicTitle,
        modifier = modifier,
        subtitle = subtitle,
        trackId = trackId,
        artist = artist,
        postId = postId,
        artworkUrl = artworkUrl,
        playlistTitle = playlistTitle,
        musicQueue = musicQueue,
        trailingAction = trailingAction,
        onCardClick = onCardClick,
    )
}


