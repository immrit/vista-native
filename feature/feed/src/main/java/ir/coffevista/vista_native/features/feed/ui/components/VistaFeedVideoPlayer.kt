package ir.coffevista.vista_native.features.feed.ui.components

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.datastore.MediaDataPolicy
import ir.coffevista.vista_native.core.designsystem.glass.LocalVistaGlassState
import ir.coffevista.vista_native.features.feed.R

@OptIn(UnstableApi::class)
@Composable
fun VistaFeedVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    thumbnailUrl: String? = null,
    aspectRatio: Float? = null,
    autoPlay: Boolean = true,
    dataSaver: Boolean = false,
    initiallyMuted: Boolean = true,
    showMuteToggle: Boolean = true,
    onVideoClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val shouldAutoPlay = autoPlay && !dataSaver

    var hasInitiatedPlayback by remember(videoUrl, shouldAutoPlay) { mutableStateOf(shouldAutoPlay) }
    var isMuted by remember { mutableStateOf(initiallyMuted) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember(videoUrl, shouldAutoPlay) { mutableStateOf(shouldAutoPlay) }
    var hasRenderedFirstFrame by remember(videoUrl) { mutableStateOf(false) }
    var hasPlaybackError by remember(videoUrl) { mutableStateOf(false) }
    var lastErrorCode by remember(videoUrl) { mutableStateOf<String?>(null) }
    var retryCount by remember(videoUrl) { mutableIntStateOf(0) }

    val activePlayingUrl by FeedVideoPlaybackCoordinator.activePlayingVideoUrl.collectAsStateWithLifecycle()

    val exoPlayer = remember(videoUrl, dataSaver) {
        val mediaSourceFactory = DefaultMediaSourceFactory(
            FeedVideoPlaybackCoordinator.buildCacheDataSourceFactory(context)
        )
        val builder = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
        if (dataSaver) {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(2_500, 6_000, 1_000, 2_000)
                .build()
            val trackSelector = DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setMaxVideoBitrate(MediaDataPolicy.DATA_SAVER_MAX_VIDEO_BITRATE)
                )
            }
            builder.setLoadControl(loadControl).setTrackSelector(trackSelector)
        }
        builder.build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ALL
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            volume = if (initiallyMuted) 0f else 1f
            if (shouldAutoPlay) {
                prepare()
                playWhenReady = true
            } else {
                playWhenReady = false
            }
        }
    }

    LaunchedEffect(shouldAutoPlay, exoPlayer) {
        if (!shouldAutoPlay && exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else if (shouldAutoPlay && !hasInitiatedPlayback) {
            hasInitiatedPlayback = true
            hasPlaybackError = false
            isBuffering = true
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        }
    }

    LaunchedEffect(hasPlaybackError, exoPlayer) {
        if (hasPlaybackError && retryCount < 1) {
            kotlinx.coroutines.delay(450L)
            retryCount += 1
            FeedVideoPlaybackCoordinator.evictVideoCache(context, videoUrl)
            hasPlaybackError = false
            isBuffering = true
            exoPlayer.seekTo(0L)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = shouldAutoPlay
            if (shouldAutoPlay) {
                exoPlayer.play()
            }
        }
    }

    LaunchedEffect(activePlayingUrl, exoPlayer) {
        if (activePlayingUrl != null && activePlayingUrl != videoUrl) {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    hasPlaybackError = false
                    lastErrorCode = null
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    FeedVideoPlaybackCoordinator.requestPlay(videoUrl)
                }
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
                isBuffering = false
            }

            override fun onPlayerError(error: PlaybackException) {
                val diagnostic = FeedVideoPlaybackCoordinator.formatPlaybackErrorDiagnostic(error)
                Log.w("VistaFeedVideoPlayer", "Playback failed for $videoUrl: $diagnostic", error)
                FeedVideoPlaybackCoordinator.evictVideoCache(context, videoUrl)
                isPlaying = false
                isBuffering = false
                hasPlaybackError = true
                lastErrorCode = diagnostic
            }
        }
        exoPlayer.addListener(listener)

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (shouldAutoPlay && hasInitiatedPlayback) exoPlayer.play()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            FeedVideoPlaybackCoordinator.release(videoUrl)
        }
    }

    var currentProgress by remember { mutableStateOf(0f) }
    val glassState = LocalVistaGlassState.current

    LaunchedEffect(isPlaying, glassState, exoPlayer) {
        while (isPlaying) {
            val duration = exoPlayer.duration
            if (duration > 0) {
                currentProgress = (exoPlayer.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }
            glassState?.invalidate()
            kotlinx.coroutines.delay(66)
        }
    }

    val finalAspectRatio = aspectRatio ?: (4f / 5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(finalAspectRatio)
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(
                    R.layout.vista_feed_texture_player_view,
                    null,
                    false,
                ) as PlayerView).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { playerView ->
                if (playerView.player !== exoPlayer) {
                    playerView.player = exoPlayer
                }
            },
            onRelease = { playerView ->
                playerView.player = null
            },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onVideoClick == null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!hasInitiatedPlayback || hasPlaybackError || exoPlayer.playerError != null || exoPlayer.playbackState == Player.STATE_IDLE) {
                                hasInitiatedPlayback = true
                                hasPlaybackError = false
                                isBuffering = true
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                                exoPlayer.play()
                            } else if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        }
                    } else {
                        Modifier
                    }
                ),
        )

        // Static Thumbnail until the first frame is rendered
        if (!hasRenderedFirstFrame && !thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Buffering Indicator
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = Color.White.copy(alpha = 0.85f),
                strokeWidth = 3.dp,
            )
        }

        // Play/Pause Overlay indicator when paused, waiting for manual start under data saver, or recovering from error
        AnimatedVisibility(
            visible = !isPlaying && !isBuffering && (hasRenderedFirstFrame || !hasInitiatedPlayback || hasPlaybackError),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.58f))
                        .clickable {
                            if (!hasInitiatedPlayback || hasPlaybackError || exoPlayer.playerError != null || exoPlayer.playbackState == Player.STATE_IDLE) {
                                hasInitiatedPlayback = true
                                if (hasPlaybackError || exoPlayer.playerError != null) {
                                    FeedVideoPlaybackCoordinator.evictVideoCache(context, videoUrl)
                                    exoPlayer.seekTo(0L)
                                }
                                hasPlaybackError = false
                                lastErrorCode = null
                                isBuffering = true
                                exoPlayer.prepare()
                            }
                            exoPlayer.playWhenReady = true
                            exoPlayer.play()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "پخش ویدیو",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }
                if (hasPlaybackError && retryCount >= 1) {
                    androidx.compose.material3.Text(
                        text = lastErrorCode?.let { "خطا در پخش ($it) · لمس برای تلاش مجدد" }
                            ?: "خطا در پخش ویدیو · لمس برای تلاش مجدد",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.68f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        // Mute / Unmute Toggle Button in bottom corner
        if (showMuteToggle) {
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "صدای غیرفعال" else "صدا فعال",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Video progress indicator
        if (hasRenderedFirstFrame && currentProgress > 0f) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { currentProgress },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.2f),
            )
        }
    }
}
