package ir.coffevista.vista_native.features.chat.presentation.audio

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.compose.runtime.Immutable
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import ir.coffevista.vista_native.core.designsystem.component.resolveMusicRange
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ChatAudioKind { VOICE, MUSIC }

enum class ChatAudioRepeatMode { OFF, ALL, ONE }

/**
 * A playable audio item of a conversation. [id] is the stable key of the owning message so UI cells
 * can map a message to the global player state (Telegram: `MessageObject` identity).
 */
@Immutable
data class ChatAudioTrack(
    val id: String,
    val url: String,
    val kind: ChatAudioKind,
    val title: String,
    val artist: String?,
    val durationMs: Long,
    val senderName: String,
    val conversationId: String,
    val createdAtEpochMillis: Long,
    /** Tried once when [url] (usually a local content/file uri) can no longer be opened. */
    val fallbackUrl: String? = null,
    val conversationTitle: String? = null,
)

@Immutable
data class ChatAudioState(
    val track: ChatAudioTrack? = null,
    /** Playback intent (play button shows "pause" while true, even while buffering). */
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val repeatMode: ChatAudioRepeatMode = ChatAudioRepeatMode.OFF,
    val shuffle: Boolean = false,
    val hasError: Boolean = false,
    val queue: List<ChatAudioTrack> = emptyList(),
) {
    fun isActive(id: String): Boolean = track?.id == id
}

/**
 * Process-wide audio engine for voice messages and music, modelled on Telegram's `MediaController`:
 *  - exactly one active track; starting another one replaces it (no overlapping players),
 *  - position of paused / replaced tracks is remembered and resumed,
 *  - voice messages auto-continue with the following voice messages, music honours repeat/shuffle,
 *  - previous restarts the track when more than 10 seconds were played,
 *  - voice speed and music speed are remembered independently,
 *  - audio focus (ducking / transient loss) and "becoming noisy" are handled by ExoPlayer.
 *
 * The player lives outside any composable, so scrolling a bubble off screen never stops playback.
 * All functions must be called on the main thread.
 */
@MainThread
object ChatAudioPlayer {
    private const val PREFS = "chat_audio_player"
    private const val POSITION_TICK_MS = 50L
    private const val MAX_LISTENED = 600
    val SPEED_STEPS = floatArrayOf(1f, 1.5f, 2f)
    val SPEED_CHOICES = floatArrayOf(0.5f, 1f, 1.2f, 1.5f, 1.7f, 2f)

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null
    private var exo: ExoPlayer? = null
    private val main = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(ChatAudioState())
    val state: StateFlow<ChatAudioState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0L)

    /** High-frequency playback position of the active track. Collect only where it is drawn. */
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _listened = MutableStateFlow<Set<String>>(emptySet())

    /** Ids of received voice messages the user already played (drives the blue "unread" dot). */
    val listened: StateFlow<Set<String>> = _listened.asStateFlow()

    private val _voiceSpeed = MutableStateFlow(1f)
    val voiceSpeedFlow: StateFlow<Float> = _voiceSpeed.asStateFlow()

    private val savedPositions = HashMap<String, Long>()
    private var order: List<ChatAudioTrack> = emptyList()
    private var triedFallback = false

    private var voiceSpeed = 1f
    private var musicSpeed = 1f

    fun attach(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        voiceSpeed = p.getFloat("voice_speed", 1f)
        _voiceSpeed.value = voiceSpeed
        musicSpeed = p.getFloat("music_speed", 1f)
        val repeat = runCatching { ChatAudioRepeatMode.valueOf(p.getString("repeat", "OFF").orEmpty()) }
            .getOrDefault(ChatAudioRepeatMode.OFF)
        _listened.value = p.getStringSet("listened", emptySet()).orEmpty().toSet()
        _state.update { it.copy(repeatMode = repeat, shuffle = p.getBoolean("shuffle", false)) }
    }

    /** Saved resume position (ms) of [id] when it is not the active track. */
    fun savedPositionMs(id: String): Long = savedPositions[id] ?: 0L

    fun isListened(id: String): Boolean = id in _listened.value

    // ---------------------------------------------------------------- transport

    /** Tap on a play button: play / pause the track, or switch to it (resuming its saved position). */
    fun toggle(track: ChatAudioTrack, queue: List<ChatAudioTrack> = listOf(track)) {
        val current = _state.value
        if (current.track?.id == track.id) {
            if (current.isPlaying) pause() else resume()
        } else {
            play(track, queue)
        }
    }

    fun play(track: ChatAudioTrack, queue: List<ChatAudioTrack> = listOf(track), startMs: Long? = null) {
        if (appContext == null) return
        val current = _state.value.track
        if (current != null && current.id != track.id) rememberCurrentPosition()
        val list = queue.takeIf { q -> q.any { it.id == track.id } } ?: (queue + track)
        val shuffle = _state.value.shuffle && track.kind == ChatAudioKind.MUSIC
        order = if (shuffle) shuffledAround(track, list) else list
        _state.update { it.copy(queue = list) }
        load(track, startMs ?: savedPositions[track.id] ?: 0L)
    }

    fun pause() {
        val p = exo ?: return
        p.playWhenReady = false
        rememberCurrentPosition()
        publish()
    }

    fun resume() {
        val p = exo
        val track = _state.value.track ?: return
        if (p == null) {
            load(track, savedPositions[track.id] ?: 0L)
            return
        }
        if (_state.value.hasError || p.playbackState == Player.STATE_IDLE) {
            load(track, savedPositions[track.id] ?: _position.value)
            return
        }
        if (p.playbackState == Player.STATE_ENDED) p.seekTo(0)
        p.playWhenReady = true
        publish()
        startTicker()
    }

    fun stop() {
        rememberCurrentPosition()
        stopTicker()
        exo?.let { p ->
            runCatching { p.stop() }
            runCatching { p.release() }
        }
        exo = null
        order = emptyList()
        _position.value = 0L
        _state.update {
            ChatAudioState(speed = 1f, repeatMode = it.repeatMode, shuffle = it.shuffle)
        }
    }

    fun seekToFraction(fraction: Float) {
        val duration = effectiveDuration()
        if (duration <= 0L) return
        seekToMs((fraction.coerceIn(0f, 1f) * duration).toLong())
    }

    fun seekToMs(positionMs: Long) {
        val p = exo ?: return
        val target = positionMs.coerceIn(0L, effectiveDuration().coerceAtLeast(0L))
        p.seekTo(target)
        _position.value = target
        if (p.playbackState == Player.STATE_ENDED) p.playWhenReady = true
    }

    /** Drag on a bubble that is not the active one: start it from the touched point. */
    fun seekAndPlay(track: ChatAudioTrack, queue: List<ChatAudioTrack>, fraction: Float) {
        val duration = track.durationMs.coerceAtLeast(1L)
        val startMs = (fraction.coerceIn(0f, 1f) * duration).toLong()
        if (_state.value.track?.id == track.id) {
            seekToMs(startMs)
            if (!_state.value.isPlaying) resume()
        } else {
            play(track, queue, startMs)
        }
    }

    fun next() = advance(byEnd = false, direction = 1)

    /** Telegram: more than 10 s played restarts the track instead of jumping back. */
    fun previous() {
        val track = _state.value.track ?: return
        if (_position.value > 10_000L || order.size <= 1) {
            seekToMs(0)
            return
        }
        advance(byEnd = false, direction = -1)
        if (_state.value.track?.id == track.id) seekToMs(0)
    }

    fun playQueueItem(track: ChatAudioTrack) {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        if (_state.value.track?.id != track.id) rememberCurrentPosition()
        load(track, savedPositions[track.id] ?: 0L)
    }

    // ---------------------------------------------------------------- options

    fun setSpeed(speed: Float) {
        val track = _state.value.track
        val clamped = speed.coerceIn(0.5f, 2f)
        when (track?.kind) {
            ChatAudioKind.MUSIC -> musicSpeed = clamped
            else -> {
                voiceSpeed = clamped
                _voiceSpeed.value = clamped
            }
        }
        prefs?.edit()?.putFloat("voice_speed", voiceSpeed)?.putFloat("music_speed", musicSpeed)?.apply()
        exo?.setPlaybackSpeed(clamped)
        _state.update { it.copy(speed = clamped) }
    }

    /** 1x → 1.5x → 2x → 1x, the Telegram speed button cycle. */
    fun cycleSpeed() {
        val current = _state.value.speed
        val next = SPEED_STEPS.firstOrNull { it > current + 0.05f } ?: SPEED_STEPS.first()
        setSpeed(next)
    }

    fun cycleRepeat() {
        val next = when (_state.value.repeatMode) {
            ChatAudioRepeatMode.OFF -> ChatAudioRepeatMode.ALL
            ChatAudioRepeatMode.ALL -> ChatAudioRepeatMode.ONE
            ChatAudioRepeatMode.ONE -> ChatAudioRepeatMode.OFF
        }
        prefs?.edit()?.putString("repeat", next.name)?.apply()
        _state.update { it.copy(repeatMode = next) }
    }

    fun toggleShuffle() {
        val enabled = !_state.value.shuffle
        prefs?.edit()?.putBoolean("shuffle", enabled)?.apply()
        val track = _state.value.track
        val queue = _state.value.queue
        order = if (enabled && track != null && track.kind == ChatAudioKind.MUSIC) {
            shuffledAround(track, queue)
        } else {
            queue
        }
        _state.update { it.copy(shuffle = enabled) }
    }

    // ---------------------------------------------------------------- internals

    private fun player(): ExoPlayer {
        exo?.let { return it }
        val context = checkNotNull(appContext) { "ChatAudioPlayer.attach(context) was not called" }
        val created = ExoPlayer.Builder(context).build().apply {
            setHandleAudioBecomingNoisy(true)
            addListener(listener)
        }
        exo = created
        return created
    }

    private fun load(track: ChatAudioTrack, startMs: Long, useFallback: Boolean = false) {
        val p = player()
        triedFallback = useFallback
        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(if (track.kind == ChatAudioKind.VOICE) C.AUDIO_CONTENT_TYPE_SPEECH else C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        p.setAudioAttributes(attributes, true)
        val url = if (useFallback) track.fallbackUrl ?: track.url else track.url
        p.setMediaItem(buildMediaItem(url), startMs.coerceAtLeast(0L))
        val speed = if (track.kind == ChatAudioKind.MUSIC) musicSpeed else voiceSpeed
        p.setPlaybackSpeed(speed)
        p.prepare()
        p.playWhenReady = true
        _position.value = startMs.coerceAtLeast(0L)
        _state.update {
            it.copy(
                track = track,
                isPlaying = true,
                isBuffering = true,
                durationMs = track.durationMs,
                speed = speed,
                hasError = false,
            )
        }
        if (track.kind == ChatAudioKind.VOICE) markListened(track.id)
        startTicker()
    }

    private fun buildMediaItem(url: String): MediaItem {
        val clean = url.substringBefore('#')
        val uri = if (clean.startsWith("/")) Uri.fromFile(File(clean)) else Uri.parse(clean)
        val builder = MediaItem.Builder().setUri(uri)
        if (url.contains('#') || url.contains("startMs=") || url.contains("t=")) {
            val (start, end) = resolveMusicRange(url)
            if (start > 0L || (end != null && end > start)) {
                builder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(start)
                        .apply { if (end != null && end > start) setEndPositionMs(end) }
                        .build(),
                )
            }
        }
        return builder.build()
    }

    private fun publish() {
        val p = exo ?: return
        val track = _state.value.track ?: return
        val duration = p.duration.takeIf { it > 0L && it != C.TIME_UNSET } ?: track.durationMs
        _state.update {
            it.copy(
                isPlaying = p.playWhenReady && p.playbackState != Player.STATE_ENDED,
                isBuffering = p.playWhenReady && p.playbackState == Player.STATE_BUFFERING,
                durationMs = duration,
            )
        }
    }

    private fun effectiveDuration(): Long {
        val p = exo
        val fromPlayer = p?.duration?.takeIf { it > 0L && it != C.TIME_UNSET }
        return fromPlayer ?: _state.value.durationMs
    }

    private fun rememberCurrentPosition() {
        val track = _state.value.track ?: return
        val p = exo
        val position = p?.currentPosition?.takeIf { p.playbackState != Player.STATE_ENDED } ?: _position.value
        val duration = effectiveDuration()
        if (position <= 0L || (duration > 0L && position >= duration - 300L)) {
            savedPositions.remove(track.id)
        } else {
            savedPositions[track.id] = position
        }
    }

    private fun markListened(id: String) {
        if (id in _listened.value) return
        val next = (_listened.value + id).let { set ->
            if (set.size > MAX_LISTENED) set.drop(set.size - MAX_LISTENED).toSet() else set
        }
        _listened.value = next
        prefs?.edit()?.putStringSet("listened", next)?.apply()
    }

    private fun shuffledAround(current: ChatAudioTrack, list: List<ChatAudioTrack>): List<ChatAudioTrack> {
        val rest = list.filter { it.id != current.id }.shuffled()
        return listOf(current) + rest
    }

    /** Voice → following voice message, music → repeat / shuffle aware neighbour. */
    private fun advance(byEnd: Boolean, direction: Int) {
        val track = _state.value.track ?: return
        val list = order.ifEmpty { _state.value.queue }
        val index = list.indexOfFirst { it.id == track.id }
        if (index < 0) return
        val repeat = _state.value.repeatMode
        var target = index + direction
        val wrapped = target !in list.indices
        if (wrapped) {
            if (track.kind == ChatAudioKind.VOICE && byEnd) {
                finishVoiceQueue(track)
                return
            }
            if (byEnd && repeat == ChatAudioRepeatMode.OFF) {
                finishMusicQueue(track)
                return
            }
            target = if (direction > 0) 0 else list.lastIndex
        }
        val next = list.getOrNull(target) ?: return
        if (next.id != track.id) rememberCurrentPosition()
        if (byEnd) savedPositions.remove(track.id)
        load(next, if (byEnd) 0L else savedPositions[next.id] ?: 0L)
    }

    private fun finishVoiceQueue(track: ChatAudioTrack) {
        savedPositions.remove(track.id)
        stop()
    }

    private fun finishMusicQueue(track: ChatAudioTrack) {
        savedPositions.remove(track.id)
        val p = exo ?: return
        p.playWhenReady = false
        p.seekTo(0)
        _position.value = 0L
        stopTicker()
        publish()
    }

    private fun onEnded() {
        val track = _state.value.track ?: return
        if (track.kind == ChatAudioKind.MUSIC && _state.value.repeatMode == ChatAudioRepeatMode.ONE) {
            exo?.let { it.seekTo(0); it.playWhenReady = true }
            return
        }
        advance(byEnd = true, direction = 1)
    }

    private val tick = object : Runnable {
        override fun run() {
            val p = exo ?: return
            _position.value = p.currentPosition.coerceAtLeast(0L)
            if (_state.value.isPlaying) main.postDelayed(this, POSITION_TICK_MS)
        }
    }

    private fun startTicker() {
        main.removeCallbacks(tick)
        main.post(tick)
    }

    private fun stopTicker() = main.removeCallbacks(tick)

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                onEnded()
            } else {
                publish()
                if (playbackState == Player.STATE_READY) startTicker()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            publish()
            if (playWhenReady) startTicker()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publish()
            if (isPlaying) startTicker() else rememberCurrentPosition()
        }

        override fun onPlayerError(error: PlaybackException) {
            val track = _state.value.track ?: return
            if (!triedFallback && !track.fallbackUrl.isNullOrBlank() && track.fallbackUrl != track.url) {
                load(track, savedPositions[track.id] ?: _position.value, useFallback = true)
                return
            }
            stopTicker()
            _state.update { it.copy(isPlaying = false, isBuffering = false, hasError = true) }
        }
    }
}
