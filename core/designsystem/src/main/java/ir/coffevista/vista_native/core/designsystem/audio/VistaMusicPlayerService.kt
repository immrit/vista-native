package ir.coffevista.vista_native.core.designsystem.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ir.coffevista.vista_native.core.designsystem.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * سرویس نوتیفیکیشن، قفل‌صفحه و MediaSession پخش صدا و موسیقی ویستا
 * (پیاده‌سازی مستقیم بر اساس `MusicPlayerService` تلگرام):
 * - نوار جست‌وجوی تعاملی (SeekBar) روی نوتیفیکیشن و قفل‌صفحه برای عقب و جلو کردن موزیک
 * - دکمه‌های پخش/مکث، آهنگ قبلی، آهنگ بعدی و بستن
 * - پشتیبانی از دکمه‌های بلوتوث/هدست و قطع خودکار هنگام جدا شدن هدفون (`ACTION_AUDIO_BECOMING_NOISY`)
 */
class VistaMusicPlayerService : Service() {

    private var mediaSession: MediaSession? = null
    private var albumArtPlaceholder: Bitmap? = null
    private var lastLoadedArtworkKey: String? = null
    private var lastLoadedArtwork: Bitmap? = null
    private var foregroundStarted = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var artworkLoadJob: Job? = null

    private val headsetPlugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action) {
                VistaAudioPlayer.pause()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        VistaAudioPlayer.attach(applicationContext)
        ensureChannel()
        albumArtPlaceholder = createPlaceholderArtwork()

        mediaSession = MediaSession(this, "VistaAudioPlayer").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = VistaAudioPlayer.resume()
                override fun onPause() = VistaAudioPlayer.pause()
                override fun onSkipToNext() = VistaAudioPlayer.next()
                override fun onSkipToPrevious() = VistaAudioPlayer.previous()
                override fun onSeekTo(pos: Long) {
                    VistaAudioPlayer.seekToMs(pos)
                    updateMediaSessionPlaybackState()
                }
                override fun onSetPlaybackSpeed(speed: Float) = VistaAudioPlayer.setSpeed(speed)
                override fun onStop() = VistaAudioPlayer.stop()
                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    val keyEvent: KeyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    } ?: return super.onMediaButtonEvent(mediaButtonIntent)
                    if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_HEADSETHOOK,
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                if (VistaAudioPlayer.state.value.isPlaying) {
                                    VistaAudioPlayer.pause()
                                } else {
                                    VistaAudioPlayer.resume()
                                }
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                VistaAudioPlayer.resume()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                VistaAudioPlayer.pause()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                VistaAudioPlayer.next()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                VistaAudioPlayer.previous()
                                return true
                            }
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }
            })
            isActive = true
        }

        ContextCompat.registerReceiver(
            this,
            headsetPlugReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            foregroundStarted = false
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val state = VistaAudioPlayer.state.value
        val track = state.track
        if (track == null) {
            val fallbackNotif = buildEmptyStoppingNotification()
            promoteForeground(fallbackNotif)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
            stopSelf()
            return START_NOT_STICKY
        }

        val refreshMetadata = intent?.getBooleanExtra(EXTRA_REFRESH_METADATA, false) ?: false
        createOrUpdateNotification(state, track, refreshMetadata)
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(headsetPlugReceiver) }
        artworkLoadJob?.cancel()
        serviceScope.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        mediaSession?.let { session ->
            runCatching { session.isActive = false }
            runCatching { session.release() }
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun createOrUpdateNotification(
        state: VistaAudioState,
        track: VistaAudioTrack,
        refreshMetadata: Boolean,
    ) {
        val session = mediaSession ?: return
        val artKey = listOfNotNull(track.url, track.fallbackUrl, track.artworkUrl).joinToString("|")

        if (artKey != lastLoadedArtworkKey) {
            lastLoadedArtworkKey = artKey
            lastLoadedArtwork = VistaAudioCoverArtCache.getCachedBitmap(track.url, track.fallbackUrl, track.artworkUrl)
            if (lastLoadedArtwork == null) {
                artworkLoadJob?.cancel()
                artworkLoadJob = serviceScope.launch {
                    val bmp = VistaAudioCoverArtCache.loadCoverBitmap(
                        context = applicationContext,
                        url = track.url,
                        fallbackUrl = track.fallbackUrl,
                        artworkUrl = track.artworkUrl,
                    )
                    if (bmp != null && lastLoadedArtworkKey == artKey) {
                        lastLoadedArtwork = bmp
                        val currentState = VistaAudioPlayer.state.value
                        val currentTrack = currentState.track
                        if (currentTrack != null && currentTrack.id == track.id) {
                            createOrUpdateNotification(currentState, currentTrack, refreshMetadata = true)
                        }
                    }
                }
            }
        }

        val isPlaying = state.isPlaying
        val isMusic = track.kind == VistaAudioKind.MUSIC
        val durationMs = state.durationMs.takeIf { it > 0L } ?: track.durationMs
        val artwork = lastLoadedArtwork?.takeUnless { it.isRecycled } ?: albumArtPlaceholder

        updateMediaSessionPlaybackState(state)

        if (refreshMetadata || lastLoadedArtworkKey == artKey) {
            val contentText = track.artist?.takeIf(String::isNotBlank)
                ?: if (isMusic) track.senderName else "پیام صوتی"
            val metaBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, contentText)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, contentText)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs.coerceAtLeast(0L))
            track.conversationTitle?.takeIf(String::isNotBlank)?.let { album ->
                metaBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM, album)
            }
            if (artwork != null && !artwork.isRecycled) {
                metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
            }
            session.setMetadata(metaBuilder.build())
        }

        val contentIntent = buildContentPendingIntent()
        val pendingPrev = buildReceiverPendingIntent(ACTION_PREVIOUS, 101)
        val pendingPlayPause = buildReceiverPendingIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY, 102)
        val pendingNext = buildReceiverPendingIntent(ACTION_NEXT, 103)
        val pendingClose = buildReceiverPendingIntent(ACTION_CLOSE, 104)

        val mediaStyle = Notification.MediaStyle()
            .setMediaSession(session.sessionToken)
        if (isMusic) {
            mediaStyle.setShowActionsInCompactView(0, 1, 2)
        } else {
            mediaStyle.setShowActionsInCompactView(0, 1)
        }

        val subtitle = track.artist?.takeIf(String::isNotBlank)
            ?: if (isMusic) track.senderName else "پیام صوتی"

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(isPlaying)
            .setContentTitle(track.title)
            .setContentText(subtitle)
            .setContentIntent(contentIntent)
            .setDeleteIntent(pendingClose)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(mediaStyle)

        track.conversationTitle?.takeIf(String::isNotBlank)?.let { builder.setSubText(it) }
        if (artwork != null && !artwork.isRecycled) {
            builder.setLargeIcon(artwork)
        }

        if (isMusic) {
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_player_prev),
                    "قبلی",
                    pendingPrev,
                ).build(),
            )
        }
        builder.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(this, if (isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play),
                if (isPlaying) "مکث" else "پخش",
                pendingPlayPause,
            ).build(),
        )
        if (isMusic) {
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_player_next),
                    "بعدی",
                    pendingNext,
                ).build(),
            )
        }
        builder.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_player_close),
                "بستن",
                pendingClose,
            ).build(),
        )

        val notification = builder.build()
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!foregroundStarted || isPlaying) {
                promoteForeground(notification)
            } else {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                foregroundStarted = false
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        } else {
            if (isPlaying) {
                promoteForeground(notification)
            } else {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                foregroundStarted = false
                notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun updateMediaSessionPlaybackState(state: VistaAudioState = VistaAudioPlayer.state.value) {
        val session = mediaSession ?: return
        val track = state.track ?: return
        val positionMs = VistaAudioPlayer.position.value.coerceAtLeast(0L)
        val isMusic = track.kind == VistaAudioKind.MUSIC

        val pbState = when {
            state.isBuffering -> PlaybackState.STATE_BUFFERING
            state.isPlaying -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_PAUSED
        }

        var actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SEEK_TO or
            PlaybackState.ACTION_STOP
        if (isMusic) {
            actions = actions or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SKIP_TO_NEXT
        }

        val speed = if (state.isPlaying && !state.isBuffering) state.speed else 0f
        val stateBuilder = PlaybackState.Builder()
            .setState(pbState, positionMs, speed, SystemClock.elapsedRealtime())
            .setActions(actions)

        session.setPlaybackState(stateBuilder.build())
    }

    private fun promoteForeground(notification: Notification) {
        foregroundStarted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildEmptyStoppingNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("پخش‌کننده ویستا")
            .build()
    }

    private fun buildContentPendingIntent(): PendingIntent {
        val launchIntent = (packageManager.getLaunchIntentForPackage(packageName) ?: Intent()).apply {
            action = ACTION_OPEN_PLAYER
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            100,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildReceiverPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(action).apply {
            component = ComponentName(applicationContext, VistaMusicPlayerReceiver::class.java)
        }
        return PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "پخش‌کننده صدا و موسیقی",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "کنترل پخش موزیک و پیام صوتی در نوتیفیکیشن و قفل‌صفحه"
                    setShowBadge(false)
                    setSound(null, null)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun createPlaceholderArtwork(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                size.toFloat(),
                size.toFloat(),
                intArrayOf(0xFF4FA6E8.toInt(), 0xFF338CD1.toInt()),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(Rect(0, 0, size, size), bgPaint)
        ContextCompat.getDrawable(this, R.drawable.vista_ic_music)?.let { drawable ->
            val iconSize = (size * 0.48f).toInt()
            val left = (size - iconSize) / 2
            val top = (size - iconSize) / 2
            drawable.setTint(0xFFFFFFFF.toInt())
            drawable.setBounds(left, top, left + iconSize, top + iconSize)
            drawable.draw(canvas)
        }
        return bmp
    }

    companion object {
        private const val CHANNEL_ID = "vista_music_player"
        private const val NOTIFICATION_ID = 5055
        private const val EXTRA_REFRESH_METADATA = "refresh_metadata"
        private const val ACTION_STOP_SERVICE = "ir.coffevista.vista_native.audio.STOP_SERVICE"

        const val ACTION_OPEN_PLAYER = "ir.coffevista.vista_native.audio.OPEN_PLAYER"
        const val ACTION_PLAY = "ir.coffevista.vista_native.audio.PLAY"
        const val ACTION_PAUSE = "ir.coffevista.vista_native.audio.PAUSE"
        const val ACTION_NEXT = "ir.coffevista.vista_native.audio.NEXT"
        const val ACTION_PREVIOUS = "ir.coffevista.vista_native.audio.PREVIOUS"
        const val ACTION_CLOSE = "ir.coffevista.vista_native.audio.CLOSE"
        const val ACTION_REPEAT = "ir.coffevista.vista_native.audio.REPEAT"
        const val ACTION_SHUFFLE = "ir.coffevista.vista_native.audio.SHUFFLE"

        fun syncPlayback(context: Context, refreshMetadata: Boolean = false) {
            if (VistaAudioPlayer.state.value.track == null) {
                stopService(context)
                return
            }
            val intent = Intent(context, VistaMusicPlayerService::class.java).apply {
                putExtra(EXTRA_REFRESH_METADATA, refreshMetadata)
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure {
                    // Fallback when background start isn't allowed
                    runCatching { context.startService(intent) }
                }
        }

        fun stopService(context: Context) {
            runCatching {
                context.stopService(Intent(context, VistaMusicPlayerService::class.java))
            }
        }
    }
}
