package ir.coffevista.vista_native.features.feed.ui.create

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.core.network.InternalNetworkModule
import ir.coffevista.vista_native.features.feed.data.FeedAppendResult
import ir.coffevista.vista_native.features.feed.data.FeedKind
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.features.feed.data.FeedRefreshResult
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import ir.coffevista.vista_native.features.feed.data.FeedSnapshot
import ir.coffevista.vista_native.features.feed.ui.components.FeedVideoPlaybackCoordinator
import ir.coffevista.vista_native.features.feed.ui.components.VistaFeedVideoPlayer
import ir.coffevista.vista_native.features.feed.ui.components.VistaOkHttpDataSource
import ir.coffevista.vista_native.features.feed.ui.reels.ReelsViewModel
import ir.coffevista.vista_native.features.feed.ui.reels.ReelsViewerScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class FeedAndReelsPlaybackInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @OptIn(UnstableApi::class)
    @Test
    fun trimPortraitSilentHevc_andPlayThroughFeedAndReelsComposePlayers() {
        runBlocking {
        // Mirror VistaApplication.onCreate() so VistaOkHttpDataSource uses @ExternalMedia client.
        FeedVideoPlaybackCoordinator.configureHttpClient(InternalNetworkModule.provideExternalMediaClient())

        val workDir = File(context.cacheDir, "feed_reels_ui_test_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            // 1. High-resolution portrait (720x1280 or 1080x1920 if supported) with B-frames -> transcode + faststart
            val (portraitW, portraitH) = pickSupportedAvcSize(preferredW = 720, preferredH = 1280, fallbackW = 360, fallbackH = 640)
            val portraitSrc = File(workDir, "src_portrait_bframe.mp4")
            synthesizeMp4(
                outputFile = portraitSrc,
                videoMime = MediaFormat.MIMETYPE_VIDEO_AVC,
                durationMs = 4_500L,
                width = portraitW,
                height = portraitH,
                fps = 30,
                orientationDegrees = 0,
                tryBFrames = true,
                includeAudio = true,
            )
            val trimmedPortrait = trimVideo(
                context = context,
                uri = Uri.fromFile(portraitSrc),
                startMs = 500L,
                endMs = 3_500L,
                maxDurationMs = 60_000L,
            )
            try {
                assertTrue("Portrait trimmed video must be faststart", Mp4FastStart.isFastStart(trimmedPortrait))
                val portraitInspection = inspectFile(trimmedPortrait)
                Log.i(TAG, "PORTRAIT_TRIM: $portraitInspection")
                assertEquals(0, portraitInspection.backwardsPtsCount)
                assertTrue(portraitInspection.videoSamples >= 60)

                // Serve over Range-capable HTTP and verify real Compose UI playback in BOTH Feed & Reels!
                val server = LocalMp4HttpServer(trimmedPortrait, supportRange = true).apply { start() }
                try {
                    verifyFeedAndReelsComposables(server.url)
                } finally {
                    server.close()
                }
            } finally {
                trimmedPortrait.delete()
            }

            // 2. Landscape 1280x720 rotated 90 degrees (typical vertical camera clip) with B-frames
            val (camW, camH) = pickSupportedAvcSize(preferredW = 1280, preferredH = 720, fallbackW = 640, fallbackH = 360)
            val camSrc = File(workDir, "src_cam_rot90.mp4")
            synthesizeMp4(
                outputFile = camSrc,
                videoMime = MediaFormat.MIMETYPE_VIDEO_AVC,
                durationMs = 4_500L,
                width = camW,
                height = camH,
                fps = 30,
                orientationDegrees = 90,
                tryBFrames = true,
                includeAudio = true,
            )
            val trimmedCam = trimVideo(
                context = context,
                uri = Uri.fromFile(camSrc),
                startMs = 500L,
                endMs = 3_500L,
                maxDurationMs = 60_000L,
            )
            try {
                val camInsp = inspectFile(trimmedCam)
                Log.i(TAG, "CAM_ROT90_TRIM: $camInsp")
                assertEquals(90, camInsp.rotation)
                assertEquals(0, camInsp.backwardsPtsCount)
                assertTrue(Mp4FastStart.isFastStart(trimmedCam))
            } finally {
                trimmedCam.delete()
            }

            // 3. Silent video (no audio track) with B-frames
            val silentSrc = File(workDir, "src_silent_bframe.mp4")
            synthesizeMp4(
                outputFile = silentSrc,
                videoMime = MediaFormat.MIMETYPE_VIDEO_AVC,
                durationMs = 4_000L,
                width = 320,
                height = 240,
                fps = 30,
                orientationDegrees = 0,
                tryBFrames = true,
                includeAudio = false,
            )
            val trimmedSilent = trimVideo(
                context = context,
                uri = Uri.fromFile(silentSrc),
                startMs = 500L,
                endMs = 3_000L,
                maxDurationMs = 60_000L,
            )
            try {
                val silentInsp = inspectFile(trimmedSilent)
                Log.i(TAG, "SILENT_TRIM: $silentInsp")
                assertNull(silentInsp.audioMime)
                assertEquals(0, silentInsp.backwardsPtsCount)
                assertTrue(Mp4FastStart.isFastStart(trimmedSilent))
            } finally {
                trimmedSilent.delete()
            }

            // 4. HEVC source video (when device/emulator provides an HEVC encoder)
            if (hasEncoderForMime(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                val hevcSrc = File(workDir, "src_hevc.mp4")
                val synthesizedHevc = runCatching {
                    synthesizeMp4(
                        outputFile = hevcSrc,
                        videoMime = MediaFormat.MIMETYPE_VIDEO_HEVC,
                        durationMs = 4_000L,
                        width = 320,
                        height = 240,
                        fps = 30,
                        orientationDegrees = 0,
                        tryBFrames = false,
                        includeAudio = true,
                    )
                }
                if (synthesizedHevc.isSuccess) {
                    val trimmedHevc = trimVideo(
                        context = context,
                        uri = Uri.fromFile(hevcSrc),
                        startMs = 500L,
                        endMs = 3_000L,
                        maxDurationMs = 60_000L,
                    )
                    try {
                        val hevcInsp = inspectFile(trimmedHevc)
                        Log.i(TAG, "HEVC_TRIM: $hevcInsp")
                        assertEquals(MediaFormat.MIMETYPE_VIDEO_AVC, hevcInsp.videoMime)
                        assertEquals(0, hevcInsp.backwardsPtsCount)
                        assertTrue(Mp4FastStart.isFastStart(trimmedHevc))
                    } finally {
                        trimmedHevc.delete()
                    }
                }
            } else {
                Log.i(TAG, "HEVC_TRIM: skipped (no HEVC encoder on this AVD)")
            }
        } finally {
            workDir.deleteRecursively()
        }
        }
    }

    @OptIn(UnstableApi::class)
    @Test
    fun validateTrimmedMp4_rejectsCorruptNalLengthCaughtByMedia3Extractor() {
        val workDir = File(context.cacheDir, "nal_corrupt_test_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            val validMp4 = File(workDir, "valid.mp4")
            synthesizeMp4(
                outputFile = validMp4,
                videoMime = MediaFormat.MIMETYPE_VIDEO_AVC,
                durationMs = 2_000L,
                width = 360,
                height = 640,
                fps = 30,
                orientationDegrees = 0,
                tryBFrames = false,
                includeAudio = false,
            )
            assertTrue("Baseline MP4 should pass validation", validateTrimmedMp4(validMp4))
            assertTrue("Baseline MP4 should pass Media3 extractor", verifyMp4WithMedia3Extractor(validMp4))

            // Zero out the 4-byte NAL length prefix of the first video chunk in mdat.
            val corruptedMp4 = File(workDir, "corrupted_nal.mp4")
            validMp4.copyTo(corruptedMp4, overwrite = true)
            val firstChunkOffset = readFirstChunkOffset(corruptedMp4)
            assertTrue(
                "Valid stco/co64 chunk offset expected (got $firstChunkOffset)",
                firstChunkOffset > 0L && firstChunkOffset + 4 < corruptedMp4.length(),
            )
            RandomAccessFile(corruptedMp4, "rw").use { raf ->
                raf.seek(firstChunkOffset)
                raf.write(byteArrayOf(0, 0, 0, 0))
            }

            assertFalse(
                "Media3 Mp4Extractor must reject zero NAL length sample",
                verifyMp4WithMedia3Extractor(corruptedMp4),
            )
            assertFalse(
                "validateTrimmedMp4 must reject container with Invalid NAL length",
                validateTrimmedMp4(corruptedMp4),
            )
        } finally {
            workDir.deleteRecursively()
        }
    }

    @OptIn(UnstableApi::class)
    @Test
    fun vistaOkHttpDataSource_rejectsNonMediaContentTypeAndEmpty200() {
        val client = InternalNetworkModule.provideExternalMediaClient()
        val serverSocket = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
        val running = AtomicBoolean(true)
        val acceptThread = Thread {
            while (running.get()) {
                val sock = runCatching { serverSocket.accept() }.getOrNull() ?: break
                Thread {
                    sock.use { s ->
                        val input = BufferedInputStream(s.getInputStream())
                        val reqLine = generateSequence {
                            val sb = StringBuilder()
                            var ch = input.read()
                            while (ch >= 0 && ch != '\n'.code) {
                                if (ch != '\r'.code) sb.append(ch.toChar())
                                ch = input.read()
                            }
                            if (ch < 0 && sb.isEmpty()) null else sb.toString()
                        }.takeWhile { it.isNotEmpty() }.firstOrNull().orEmpty()
                        val out = s.getOutputStream()
                        val responseBytes = if (reqLine.contains("/html")) {
                            val body = "<html><body>WAF block</body></html>".toByteArray(Charsets.UTF_8)
                            ("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" +
                                "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray() + body
                        } else {
                            ("HTTP/1.1 200 OK\r\nContent-Type: video/mp4\r\n" +
                                "Content-Length: 0\r\nConnection: close\r\n\r\n").toByteArray()
                        }
                        out.write(responseBytes)
                        out.flush()
                    }
                }.apply { isDaemon = true }.start()
            }
        }.apply {
            isDaemon = true
            start()
        }

        try {
            val port = serverSocket.localPort
            val htmlSource = VistaOkHttpDataSource.Factory(client, "VistaTest").createDataSource()
            val htmlError = runCatching {
                htmlSource.open(DataSpec(Uri.parse("http://127.0.0.1:$port/html")))
            }.exceptionOrNull()
            assertTrue(
                "Expected InvalidContentTypeException, got $htmlError",
                htmlError is HttpDataSource.InvalidContentTypeException,
            )
            runCatching { htmlSource.close() }

            val emptySource = VistaOkHttpDataSource.Factory(client, "VistaTest").createDataSource()
            val emptyError = runCatching {
                emptySource.open(DataSpec(Uri.parse("http://127.0.0.1:$port/empty")))
            }.exceptionOrNull()
            assertTrue(
                "Expected HttpDataSourceException for empty 200 body, got $emptyError",
                emptyError is HttpDataSource.HttpDataSourceException,
            )
            runCatching { emptySource.close() }
        } finally {
            running.set(false)
            runCatching { serverSocket.close() }
            runCatching { acceptThread.join(1_000L) }
        }
    }

    @OptIn(UnstableApi::class)
    @Test
    fun feedPlayer_evictsPoisonedSimpleCacheOnParseErrorAndRecoversOnRetry() {
        val workDir = File(context.cacheDir, "cache_evict_test_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            FeedVideoPlaybackCoordinator.configureHttpClient(InternalNetworkModule.provideExternalMediaClient())
            val src = File(workDir, "src.mp4")
            synthesizeMp4(
                outputFile = src,
                videoMime = MediaFormat.MIMETYPE_VIDEO_AVC,
                durationMs = 3_500L,
                width = 360,
                height = 640,
                fps = 30,
                orientationDegrees = 0,
                tryBFrames = false,
                includeAudio = true,
            )
            val fastStartMp4 = File(workDir, "fast.mp4")
            if (!Mp4FastStart.relocateMoovToFront(src, fastStartMp4)) {
                src.copyTo(fastStartMp4, overwrite = true)
            }

            LocalMp4HttpServer(fastStartMp4, supportRange = true).use { server ->
                server.start()
                val cache = FeedVideoPlaybackCoordinator.getCache(context)
                cache.removeResource(server.url)

                // Pre-poison SimpleCache under server.url so initial prepare hits ERROR_CODE_PARSING_CONTAINER_MALFORMED.
                val poisonBytes = "<html>corrupted cache entry</html>".toByteArray(Charsets.UTF_8)
                val poisonDs = CacheDataSource(cache, ByteArrayDataSource(poisonBytes))
                try {
                    poisonDs.open(DataSpec(Uri.parse(server.url)))
                    val buf = ByteArray(256)
                    while (poisonDs.read(buf, 0, buf.size) > 0) {
                        // Drain so CacheDataSource writes the full corrupt span into SimpleCache.
                    }
                } finally {
                    runCatching { poisonDs.close() }
                }
                assertTrue(
                    "Poisoned SimpleCache entry should be cached before player starts",
                    cache.getCachedBytes(server.url, 0L, poisonBytes.size.toLong()) > 0L,
                )

                composeRule.setContent {
                    VistaFeedVideoPlayer(
                        videoUrl = server.url,
                        thumbnailUrl = null,
                        aspectRatio = 9f / 16f,
                        autoPlay = true,
                        initiallyMuted = true,
                    )
                }

                val deadline = System.currentTimeMillis() + 18_000L
                var recoveredState = false
                var lastSnapshot = ""
                while (System.currentTimeMillis() < deadline) {
                    composeRule.waitForIdle()
                    var positionMs = 0L
                    var playbackState = Player.STATE_IDLE
                    var errorCode: Int? = null
                    var textureNonBlank = false
                    composeRule.runOnIdle {
                        val pv = findFirstPlayerView(composeRule.activity.window.decorView)
                        val p = pv?.player
                        if (p != null) {
                            playbackState = p.playbackState
                            positionMs = p.currentPosition
                            errorCode = p.playerError?.errorCode
                        }
                        val tv = pv?.videoSurfaceView as? TextureView
                        if (tv != null && tv.isAvailable && tv.width > 0 && tv.height > 0) {
                            val bmp = tv.bitmap
                            textureNonBlank = bmp != null && bmp.getPixel(bmp.width / 2, bmp.height / 2) != android.graphics.Color.BLACK
                            bmp?.recycle()
                        }
                    }
                    lastSnapshot = "state=$playbackState pos=$positionMs err=$errorCode nonBlank=$textureNonBlank"
                    if (playbackState == Player.STATE_READY && errorCode == null && (positionMs > 150L || textureNonBlank)) {
                        recoveredState = true
                        break
                    }
                    Thread.sleep(250L)
                }
                Log.i(TAG, "CACHE_EVICT_RECOVERY: $lastSnapshot")
                assertTrue("Player should evict poisoned SimpleCache and recover: $lastSnapshot", recoveredState)
            }
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun readFirstChunkOffset(file: File): Long {
        val bytes = file.readBytes()
        val stcoTag = "stco".toByteArray(Charsets.US_ASCII)
        for (i in 4 until bytes.size - 16) {
            if (bytes[i] == stcoTag[0] && bytes[i + 1] == stcoTag[1] && bytes[i + 2] == stcoTag[2] && bytes[i + 3] == stcoTag[3]) {
                val entryCount = ByteBuffer.wrap(bytes, i + 8, 4).order(ByteOrder.BIG_ENDIAN).int
                if (entryCount > 0) {
                    return ByteBuffer.wrap(bytes, i + 12, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL
                }
            }
        }
        val co64Tag = "co64".toByteArray(Charsets.US_ASCII)
        for (i in 4 until bytes.size - 20) {
            if (bytes[i] == co64Tag[0] && bytes[i + 1] == co64Tag[1] && bytes[i + 2] == co64Tag[2] && bytes[i + 3] == co64Tag[3]) {
                val entryCount = ByteBuffer.wrap(bytes, i + 8, 4).order(ByteOrder.BIG_ENDIAN).int
                if (entryCount > 0) {
                    return ByteBuffer.wrap(bytes, i + 12, 8).order(ByteOrder.BIG_ENDIAN).long
                }
            }
        }
        return -1L
    }

    @OptIn(UnstableApi::class)
    private fun verifyFeedAndReelsComposables(videoUrl: String) {
        val samplePost = FeedPost(
            id = "reel-test-1",
            userId = "user-1",
            content = "تست ویدیو",
            imageUrl = null,
            imageUrls = emptyList(),
            videoUrl = videoUrl,
            musicUrl = null,
            aspectRatio = "9:16",
            musicTitle = null,
            tags = emptyList(),
            likeCount = 0L,
            commentCount = 0L,
            isLiked = false,
            isSaved = false,
            hideLikeCount = false,
            hideCommentCount = false,
            authorUsername = "testuser",
            authorFullName = "کاربر تست",
            authorAvatarUrl = null,
            authorIsVerified = false,
            authorVerificationType = null,
            createdAt = "2026-11-22T00:00:00Z",
        )
        val fakeRepo = object : FeedRepository {
            override fun observeFeed(accountId: String, kind: FeedKind): Flow<FeedSnapshot> =
                flowOf(FeedSnapshot(listOf(samplePost), hasMore = false, nextOffset = 1))

            override fun getPostById(accountId: String, postId: String): Flow<FeedPost?> =
                flowOf(samplePost)

            override fun observeUserPosts(accountId: String, userId: String): Flow<FeedSnapshot> =
                flowOf(FeedSnapshot(listOf(samplePost), hasMore = false, nextOffset = 1))

            override suspend fun refreshFeed(accountId: String, kind: FeedKind) = FeedRefreshResult(1, false)
            override suspend fun loadMoreFeed(accountId: String, kind: FeedKind) = FeedAppendResult.EndReached
            override suspend fun refreshPost(accountId: String, postId: String) = samplePost
            override suspend fun refreshUserPosts(accountId: String, userId: String) = FeedRefreshResult(1, false)
            override suspend fun loadMoreUserPosts(accountId: String, userId: String) = FeedAppendResult.EndReached
            override suspend fun getHashtagPosts(accountId: String, hashtag: String, offset: Int) = FeedSnapshot(emptyList(), false, 0)
            override suspend fun getLocationPosts(accountId: String, location: String, offset: Int) = FeedSnapshot(emptyList(), false, 0)
            override suspend fun clearAccount(accountId: String) = Unit
            override suspend fun toggleLike(accountId: String, postId: String, ownerId: String, isLiked: Boolean, newLikeCount: Long) = Unit
            override suspend fun toggleSave(accountId: String, postId: String, isSaved: Boolean) = Unit
            override suspend fun updatePost(
                accountId: String,
                postId: String,
                content: String?,
                hideLikeCount: Boolean?,
                hideCommentCount: Boolean?,
                moderationReason: String?,
                clearContent: Boolean?,
                clearImage: Boolean?,
                clearVideo: Boolean?,
                clearMusic: Boolean?,
                notifyUser: Boolean?,
            ) = samplePost

            override suspend fun deletePost(accountId: String, postId: String) = Unit
            override suspend fun reportPost(postId: String, reportedUserId: String, reason: String, additionalDetails: String?) = Unit
            override suspend fun submitAppeal(postId: String, reason: String) = Unit
            override suspend fun trackFeedEvent(postId: String, eventType: String) = Unit
            override suspend fun updateAuthorFollowStatus(authorId: String, followStatus: String) = Unit
        }
        val fakeAuth = object : AuthenticationStateProvider {
            override val state: StateFlow<AuthenticationState> = MutableStateFlow(
                AuthenticationState.SignedIn(
                    AuthenticatedContext(
                        userId = "user-1",
                        profileCompleted = true,
                        passwordRequired = false,
                        offline = false,
                    ),
                ),
            )
        }
        val viewModel = ReelsViewModel(fakeRepo, fakeAuth)
        var showReels by mutableStateOf(false)

        composeRule.setContent {
            if (!showReels) {
                VistaFeedVideoPlayer(
                    videoUrl = videoUrl,
                    thumbnailUrl = null,
                    aspectRatio = 9f / 16f,
                    autoPlay = true,
                    dataSaver = false,
                    initiallyMuted = true,
                    showMuteToggle = true,
                )
            } else {
                ReelsViewerScreen(
                    initialPostId = samplePost.id,
                    onBack = {},
                    viewModel = viewModel,
                )
            }
        }

        val feedSnap = awaitPlayerViewAdvance("FEED_UI", timeoutMs = 8_000L)
        Log.i(TAG, "FEED_UI_PLAYBACK: $feedSnap")
        assertNull("Feed PlayerView error: ${feedSnap.error}", feedSnap.error)
        assertTrue("Feed PlayerView must use TextureView: ${feedSnap.surfaceClass}", feedSnap.isTextureView)
        assertTrue("Feed TextureView surface must be available", feedSnap.textureAvailable)
        assertTrue("Feed playback must advance past 500ms: $feedSnap", feedSnap.positionMs >= 500L)

        composeRule.runOnUiThread {
            showReels = true
        }
        val reelsSnap = awaitPlayerViewAdvance("REELS_UI", timeoutMs = 8_000L)
        Log.i(TAG, "REELS_UI_PLAYBACK: $reelsSnap")
        assertNull("Reels PlayerView error: ${reelsSnap.error}", reelsSnap.error)
        assertTrue("Reels PlayerView must use TextureView: ${reelsSnap.surfaceClass}", reelsSnap.isTextureView)
        assertTrue("Reels TextureView surface must be available", reelsSnap.textureAvailable)
        assertTrue("Reels playback must advance past 500ms: $reelsSnap", reelsSnap.positionMs >= 500L)
    }

    private data class PlayerViewSnapshot(
        val found: Boolean,
        val isTextureView: Boolean,
        val textureAvailable: Boolean,
        val surfaceClass: String?,
        val width: Int,
        val height: Int,
        val playbackState: Int,
        val isPlaying: Boolean,
        val positionMs: Long,
        val error: String?,
    )

    @OptIn(UnstableApi::class)
    private fun awaitPlayerViewAdvance(label: String, timeoutMs: Long): PlayerViewSnapshot {
        val deadline = System.currentTimeMillis() + timeoutMs
        var latest = PlayerViewSnapshot(false, false, false, null, 0, 0, Player.STATE_IDLE, false, 0L, null)
        while (System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            composeRule.runOnUiThread {
                val playerView = findFirstPlayerView(composeRule.activity.window.decorView)
                if (playerView != null) {
                    val surfaceView = playerView.videoSurfaceView
                    val textureView = surfaceView as? TextureView
                    val p = playerView.player
                    latest = PlayerViewSnapshot(
                        found = true,
                        isTextureView = textureView != null,
                        textureAvailable = textureView?.isAvailable == true,
                        surfaceClass = surfaceView?.javaClass?.simpleName,
                        width = playerView.width,
                        height = playerView.height,
                        playbackState = p?.playbackState ?: Player.STATE_IDLE,
                        isPlaying = p?.isPlaying == true,
                        positionMs = p?.currentPosition ?: 0L,
                        error = p?.playerError?.let { "${it.errorCodeName}: ${it.message}" },
                    )
                }
            }
            if (latest.error != null || (latest.found && latest.isTextureView && latest.textureAvailable && latest.positionMs >= 500L)) {
                return latest
            }
            Thread.sleep(150L)
        }
        Log.w(TAG, "$label timed out: $latest")
        return latest
    }

    private fun findFirstPlayerView(view: View): PlayerView? {
        if (view is PlayerView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val hit = findFirstPlayerView(view.getChildAt(i))
                if (hit != null) return hit
            }
        }
        return null
    }

    private fun pickSupportedAvcSize(
        preferredW: Int,
        preferredH: Int,
        fallbackW: Int,
        fallbackH: Int,
    ): Pair<Int, Int> {
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        return try {
            val caps = encoder.codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).videoCapabilities
            if (caps != null && caps.isSizeSupported(preferredW, preferredH)) {
                preferredW to preferredH
            } else {
                fallbackW to fallbackH
            }
        } catch (_: Exception) {
            fallbackW to fallbackH
        } finally {
            runCatching { encoder.release() }
        }
    }

    private fun hasEncoderForMime(mime: String): Boolean {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        return list.codecInfos.any { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
        }
    }

    private data class FileInspect(
        val rotation: Int,
        val videoMime: String?,
        val audioMime: String?,
        val videoSamples: Int,
        val backwardsPtsCount: Int,
        val moovBeforeMdat: Boolean,
    )

    private fun inspectFile(file: File): FileInspect {
        val retriever = MediaMetadataRetriever()
        val rotation = try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        } finally {
            runCatching { retriever.release() }
        }
        val ext = MediaExtractor()
        var vMime: String? = null
        var aMime: String? = null
        var samples = 0
        var backwards = 0
        try {
            ext.setDataSource(file.absolutePath)
            var vTrack = -1
            for (i in 0 until ext.trackCount) {
                val fmt = ext.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true && vTrack < 0) {
                    vTrack = i
                    vMime = mime
                } else if (mime?.startsWith("audio/") == true && aMime == null) {
                    aMime = mime
                }
            }
            if (vTrack >= 0) {
                ext.selectTrack(vTrack)
                var prevPts = Long.MIN_VALUE
                while (true) {
                    val pts = ext.sampleTime
                    if (pts < 0L) break
                    if (prevPts != Long.MIN_VALUE && pts < prevPts) backwards++
                    prevPts = pts
                    samples++
                    if (!ext.advance()) break
                }
            }
        } finally {
            runCatching { ext.release() }
        }
        return FileInspect(
            rotation = rotation,
            videoMime = vMime,
            audioMime = aMime,
            videoSamples = samples,
            backwardsPtsCount = backwards,
            moovBeforeMdat = Mp4FastStart.isFastStart(file),
        )
    }

    private data class Packet(val data: ByteArray, val ptsUs: Long, val flags: Int)

    private fun synthesizeMp4(
        outputFile: File,
        videoMime: String,
        durationMs: Long,
        width: Int,
        height: Int,
        fps: Int,
        orientationDegrees: Int,
        tryBFrames: Boolean,
        includeAudio: Boolean,
    ) {
        val (vFmt, vSamples) = encodeVideoSamples(videoMime, width, height, fps, durationMs, tryBFrames)
        val audioPair = if (includeAudio) encodeAacSamples(durationMs) else null
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var started = false
        try {
            muxer.setOrientationHint(orientationDegrees)
            val vTrack = muxer.addTrack(vFmt)
            val aTrack = if (audioPair != null) muxer.addTrack(audioPair.first) else -1
            muxer.start()
            started = true

            val aSamples = audioPair?.second.orEmpty()
            var vIdx = 0
            var aIdx = 0
            val info = MediaCodec.BufferInfo()
            while (vIdx < vSamples.size || aIdx < aSamples.size) {
                val writeVideo = when {
                    vIdx >= vSamples.size -> false
                    aIdx >= aSamples.size -> true
                    else -> vSamples[vIdx].ptsUs <= aSamples[aIdx].ptsUs
                }
                if (writeVideo) {
                    val s = vSamples[vIdx++]
                    info.set(0, s.data.size, s.ptsUs, s.flags)
                    muxer.writeSampleData(vTrack, ByteBuffer.wrap(s.data), info)
                } else {
                    val s = aSamples[aIdx++]
                    info.set(0, s.data.size, s.ptsUs, s.flags)
                    muxer.writeSampleData(aTrack, ByteBuffer.wrap(s.data), info)
                }
            }
        } finally {
            if (started) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
    }

    private fun encodeVideoSamples(
        videoMime: String,
        width: Int,
        height: Int,
        fps: Int,
        durationMs: Long,
        tryBFrames: Boolean,
    ): Pair<MediaFormat, List<Packet>> {
        val totalFrames = ((durationMs * fps) / 1000L).toInt().coerceAtLeast(10)
        val format = MediaFormat.createVideoFormat(videoMime, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, 1_200_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            if (tryBFrames && videoMime == MediaFormat.MIMETYPE_VIDEO_AVC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain)
                setInteger(MediaFormat.KEY_MAX_B_FRAMES, 2)
                setInteger(MediaFormat.KEY_LATENCY, 4)
            }
        }
        val encoder = MediaCodec.createEncoderByType(videoMime)
        try {
            try {
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            } catch (_: Exception) {
                val fallback = MediaFormat.createVideoFormat(videoMime, width, height).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                    setInteger(MediaFormat.KEY_BIT_RATE, 1_200_000)
                    setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }
                encoder.reset()
                encoder.configure(fallback, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
            encoder.start()
            val samples = ArrayList<Packet>(totalFrames)
            var outFormat: MediaFormat? = null
            var inputFrame = 0
            var inputDone = false
            var outputDone = false
            val info = MediaCodec.BufferInfo()
            val yPlaneBytes = ByteArray(width * height) { 80 }
            val uvPlaneBytes = ByteArray(width * height / 4) { 128.toByte() }
            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = encoder.dequeueInputBuffer(5_000L)
                    if (inIdx >= 0) {
                        if (inputFrame >= totalFrames) {
                            encoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val ptsUs = inputFrame * 1_000_000L / fps
                            val img = runCatching { encoder.getInputImage(inIdx) }.getOrNull()
                            val bytesCount = if (img != null) {
                                try {
                                    val yb = img.planes[0].buffer
                                    val stripe = (inputFrame * 8) % width
                                    for (r in 0 until height) {
                                        val rowOff = r * img.planes[0].rowStride
                                        for (c in 0 until width step 8) {
                                            yb.put(rowOff + c, if (c in stripe..(stripe + 24)) 220.toByte() else 70.toByte())
                                        }
                                    }
                                    width * height * 3 / 2
                                } finally {
                                    img.close()
                                }
                            } else {
                                val buf = encoder.getInputBuffer(inIdx)!!
                                buf.clear()
                                buf.put(yPlaneBytes)
                                buf.put(uvPlaneBytes)
                                buf.put(uvPlaneBytes)
                                yPlaneBytes.size + uvPlaneBytes.size * 2
                            }
                            encoder.queueInputBuffer(inIdx, 0, bytesCount, ptsUs, 0)
                            inputFrame++
                        }
                    }
                }
                val outIdx = encoder.dequeueOutputBuffer(info, 5_000L)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outFormat = encoder.outputFormat
                    outIdx >= 0 -> {
                        val buf = encoder.getOutputBuffer(outIdx)
                        val isConfig = (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        if (!isConfig && info.size > 0 && buf != null) {
                            val copy = ByteArray(info.size)
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            buf.get(copy)
                            val flags = if ((info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                            samples += Packet(copy, info.presentationTimeUs, flags)
                        }
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                        encoder.releaseOutputBuffer(outIdx, false)
                    }
                }
            }
            assertNotNull(outFormat)
            return Pair(outFormat!!, samples)
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
        }
    }

    private fun encodeAacSamples(durationMs: Long): Pair<MediaFormat, List<Packet>> {
        val sampleRate = 44_100
        val channels = 1
        val totalSamples = (durationMs * sampleRate) / 1000L
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 64_000)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            val samples = ArrayList<Packet>()
            var outFormat: MediaFormat? = null
            var writtenSamples = 0L
            var inputDone = false
            var outputDone = false
            val info = MediaCodec.BufferInfo()
            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = encoder.dequeueInputBuffer(5_000L)
                    if (inIdx >= 0) {
                        if (writtenSamples >= totalSamples) {
                            encoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val chunk = minOf(1024L, totalSamples - writtenSamples).toInt()
                            val buf = encoder.getInputBuffer(inIdx)!!.apply {
                                clear()
                                order(ByteOrder.LITTLE_ENDIAN)
                            }
                            val ptsUs = writtenSamples * 1_000_000L / sampleRate
                            for (i in 0 until chunk) {
                                val pcm = (sin(2.0 * PI * 440.0 * (writtenSamples + i) / sampleRate) * 12_000.0).toInt().toShort()
                                buf.putShort(pcm)
                            }
                            encoder.queueInputBuffer(inIdx, 0, chunk * 2, ptsUs, 0)
                            writtenSamples += chunk
                        }
                    }
                }
                val outIdx = encoder.dequeueOutputBuffer(info, 5_000L)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outFormat = encoder.outputFormat
                    outIdx >= 0 -> {
                        val buf = encoder.getOutputBuffer(outIdx)
                        val isConfig = (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        if (!isConfig && info.size > 0 && buf != null) {
                            val copy = ByteArray(info.size)
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            buf.get(copy)
                            samples += Packet(copy, info.presentationTimeUs, 0)
                        }
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                        encoder.releaseOutputBuffer(outIdx, false)
                    }
                }
            }
            assertNotNull(outFormat)
            return Pair(outFormat!!, samples)
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
        }
    }

    private class LocalMp4HttpServer(
        private val file: File,
        private val supportRange: Boolean,
    ) : Closeable {
        private val serverSocket = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))

        @Volatile
        private var running = true
        private var acceptThread: Thread? = null

        val url: String
            get() = "http://127.0.0.1:${serverSocket.localPort}/clip.mp4"

        fun start() {
            acceptThread = Thread {
                while (running) {
                    val client = runCatching { serverSocket.accept() }.getOrNull() ?: break
                    Thread { handleClient(client) }.apply { isDaemon = true }.start()
                }
            }.apply {
                isDaemon = true
                start()
            }
        }

        private fun handleClient(socket: Socket) {
            socket.use { sock ->
                val input = BufferedInputStream(sock.getInputStream())
                val requestLine = readLineAscii(input) ?: return
                val method = requestLine.split(' ').getOrNull(0) ?: return
                var rangeHeader: String? = null
                while (true) {
                    val line = readLineAscii(input) ?: break
                    if (line.isEmpty()) break
                    if (line.startsWith("Range:", ignoreCase = true)) {
                        rangeHeader = line.substringAfter(':').trim()
                    }
                }
                val totalLen = file.length()
                val out = sock.getOutputStream()
                if (supportRange && rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    val spec = rangeHeader.removePrefix("bytes=").substringBefore(',')
                    val bounds = spec.split('-')
                    val start = bounds.getOrNull(0)?.toLongOrNull() ?: 0L
                    val end = bounds.getOrNull(1)?.toLongOrNull()?.coerceAtMost(totalLen - 1) ?: (totalLen - 1)
                    val len = (end - start + 1).coerceAtLeast(0L)
                    val hdr = "HTTP/1.1 206 Partial Content\r\n" +
                        "Content-Type: video/mp4\r\n" +
                        "Accept-Ranges: bytes\r\n" +
                        "Content-Range: bytes $start-$end/$totalLen\r\n" +
                        "Content-Length: $len\r\n" +
                        "Connection: close\r\n\r\n"
                    out.write(hdr.toByteArray(Charsets.US_ASCII))
                    if (!method.equals("HEAD", ignoreCase = true)) streamRange(out, start, len)
                } else {
                    val hdr = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: video/mp4\r\n" +
                        "Accept-Ranges: ${if (supportRange) "bytes" else "none"}\r\n" +
                        "Content-Length: $totalLen\r\n" +
                        "Connection: close\r\n\r\n"
                    out.write(hdr.toByteArray(Charsets.US_ASCII))
                    if (!method.equals("HEAD", ignoreCase = true)) streamRange(out, 0L, totalLen)
                }
                out.flush()
            }
        }

        private fun streamRange(out: java.io.OutputStream, start: Long, length: Long) {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                val buf = ByteArray(32 * 1024)
                var remaining = length
                while (remaining > 0 && running) {
                    val n = raf.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
                    if (n <= 0) break
                    out.write(buf, 0, n)
                    remaining -= n
                }
            }
        }

        private fun readLineAscii(input: BufferedInputStream): String? {
            val sb = StringBuilder()
            while (true) {
                val b = input.read()
                if (b < 0) return if (sb.isEmpty()) null else sb.toString()
                if (b == '\n'.code) {
                    if (sb.endsWith('\r')) sb.setLength(sb.length - 1)
                    return sb.toString()
                }
                sb.append(b.toChar())
            }
        }

        override fun close() {
            running = false
            runCatching { serverSocket.close() }
        }
    }

    private companion object {
        const val TAG = "FeedReelsUiDiag"
    }
}
