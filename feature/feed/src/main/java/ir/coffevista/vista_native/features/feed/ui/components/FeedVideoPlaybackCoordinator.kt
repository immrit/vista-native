package ir.coffevista.vista_native.features.feed.ui.components

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

@OptIn(UnstableApi::class)
object FeedVideoPlaybackCoordinator {
    private val _activePlayingVideoUrl = MutableStateFlow<String?>(null)
    val activePlayingVideoUrl: StateFlow<String?> = _activePlayingVideoUrl.asStateFlow()

    @Volatile
    private var simpleCache: SimpleCache? = null

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
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(8000)
            .setUserAgent("VistaNativeAndroid")
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, upstreamFactory)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
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
