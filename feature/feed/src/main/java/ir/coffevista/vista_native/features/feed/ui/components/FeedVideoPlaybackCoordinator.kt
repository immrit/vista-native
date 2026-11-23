package ir.coffevista.vista_native.features.feed.ui.components

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.ParserException
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import java.io.File
import java.net.Proxy
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
object FeedVideoPlaybackCoordinator {
    private val _activePlayingVideoUrl = MutableStateFlow<String?>(null)
    val activePlayingVideoUrl: StateFlow<String?> = _activePlayingVideoUrl.asStateFlow()

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Volatile
    private var streamingHttpClient: OkHttpClient? = null

    fun configureHttpClient(client: OkHttpClient) {
        streamingHttpClient = client.newBuilder()
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private fun resolveHttpClient(): OkHttpClient {
        return streamingHttpClient ?: synchronized(this) {
            streamingHttpClient ?: OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)
                .callTimeout(0, TimeUnit.MILLISECONDS)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
                .also { streamingHttpClient = it }
        }
    }

    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheDir = File(context.cacheDir, "feed_media3_video_cache")
                val evictor = LeastRecentlyUsedCacheEvictor(250L * 1024 * 1024)
                val dbProvider = StandaloneDatabaseProvider(context)
                SimpleCache(cacheDir, evictor, dbProvider).also { simpleCache = it }
            }
        }
    }

    fun buildCacheDataSourceFactory(context: Context): DataSource.Factory {
        val cache = getCache(context)
        val upstreamFactory = VistaOkHttpDataSource.Factory(
            callFactory = resolveHttpClient(),
            userAgent = "VistaNativeAndroid",
        )
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, upstreamFactory)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun evictVideoCache(context: Context, videoUrl: String) {
        if (videoUrl.isBlank()) return
        runCatching { getCache(context).removeResource(videoUrl) }
    }

    /**
     * Produces a compact diagnostic code that exposes the true underlying cause (e.g.
     * `InvalidContentTypeException`, `UnrecognizedInputFormatException` sniff failure, or
     * `Mp4Extractor` `ParserException` message) when playback fails.
     */
    fun formatPlaybackErrorDiagnostic(error: PlaybackException): String {
        val cause = generateSequence<Throwable>(error.cause) { it.cause }
            .take(8)
            .firstOrNull {
                it is HttpDataSource.InvalidContentTypeException ||
                    it is HttpDataSource.InvalidResponseCodeException ||
                    it is UnrecognizedInputFormatException ||
                    it is ParserException
            }
        return when (cause) {
            is HttpDataSource.InvalidContentTypeException ->
                "${error.errorCodeName}:${cause.contentType.ifBlank { "unknown" }}"
            is HttpDataSource.InvalidResponseCodeException ->
                "${error.errorCodeName}:HTTP_${cause.responseCode}"
            is UnrecognizedInputFormatException -> {
                val sniffTag = cause.sniffFailures.firstOrNull()?.javaClass?.simpleName
                    ?.removeSuffix("SniffFailure")
                    ?.takeIf { it.isNotBlank() }
                    ?: "NO_MATCH"
                "${error.errorCodeName}:UNRECOGNIZED_$sniffTag"
            }
            is ParserException -> {
                val msg = cause.message?.replace('\n', ' ')?.trim()?.take(40)
                if (!msg.isNullOrBlank()) "${error.errorCodeName}:$msg" else error.errorCodeName
            }
            else -> error.errorCodeName
        }
    }

    fun requestPlay(videoUrl: String) {
        _activePlayingVideoUrl.value = videoUrl
    }

    fun release(videoUrl: String) {
        if (_activePlayingVideoUrl.value == videoUrl) {
            _activePlayingVideoUrl.value = null
        }
    }
}
