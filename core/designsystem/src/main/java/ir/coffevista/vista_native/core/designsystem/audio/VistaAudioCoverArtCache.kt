package ir.coffevista.vista_native.core.designsystem.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * استخراج و کش درون‌حافظه‌ای کاور آلبوم (متادیتای ID3 محلی یا تصویر پست/پروفایل)
 * جهت استفاده هم‌زمان در پلیر تمام‌صفحه، حباب چت و نوتیفیکیشن مدیا (مشابه تلگرام).
 */
object VistaAudioCoverArtCache {
    private const val MAX_ENTRIES = 64
    private val cache = ConcurrentHashMap<String, CoverEntry>()

    private data class CoverEntry(val bitmap: Bitmap?, val imageBitmap: ImageBitmap?)

    @Composable
    fun rememberCoverArt(
        url: String?,
        fallbackUrl: String? = null,
        artworkUrl: String? = null,
    ): ImageBitmap? {
        val context = LocalContext.current
        val key = buildKey(url, fallbackUrl, artworkUrl)
        val imageBitmap by produceState<ImageBitmap?>(
            initialValue = key?.let { cache[it]?.imageBitmap },
            key1 = key,
        ) {
            if (key == null) {
                value = null
                return@produceState
            }
            cache[key]?.let {
                value = it.imageBitmap
                return@produceState
            }
            val loaded = loadCoverBitmap(
                context = context,
                url = url,
                fallbackUrl = fallbackUrl,
                artworkUrl = artworkUrl,
            )
            value = loaded?.asImageBitmap()
        }
        return imageBitmap
    }

    suspend fun loadCoverBitmap(
        context: Context,
        url: String?,
        fallbackUrl: String? = null,
        artworkUrl: String? = null,
    ): Bitmap? {
        val key = buildKey(url, fallbackUrl, artworkUrl) ?: return null
        cache[key]?.let { return it.bitmap }

        val loaded = withContext(Dispatchers.IO) {
            extractEmbeddedCover(context, url)
                ?: extractEmbeddedCover(context, fallbackUrl)
                ?: loadImageBitmap(context, artworkUrl)
        }
        if (cache.size >= MAX_ENTRIES) {
            cache.keys.firstOrNull()?.let { cache.remove(it) }
        }
        cache[key] = CoverEntry(loaded, loaded?.asImageBitmap())
        return loaded
    }

    fun getCachedBitmap(
        url: String?,
        fallbackUrl: String? = null,
        artworkUrl: String? = null,
    ): Bitmap? {
        val key = buildKey(url, fallbackUrl, artworkUrl) ?: return null
        return cache[key]?.bitmap
    }

    private fun buildKey(url: String?, fallbackUrl: String?, artworkUrl: String?): String? {
        val parts = listOfNotNull(
            url?.trim()?.takeIf(String::isNotEmpty),
            fallbackUrl?.trim()?.takeIf(String::isNotEmpty),
            artworkUrl?.trim()?.takeIf(String::isNotEmpty),
        )
        return parts.takeIf { it.isNotEmpty() }?.joinToString("|")
    }

    private fun extractEmbeddedCover(context: Context, source: String?): Bitmap? {
        val raw = source?.substringBefore('#')?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val isLocal = raw.startsWith("content://") || raw.startsWith("file://") || raw.startsWith("/")
        if (!isLocal) return null

        val retriever = MediaMetadataRetriever()
        return try {
            when {
                raw.startsWith("content://") || raw.startsWith("file://") ->
                    retriever.setDataSource(context, Uri.parse(raw))
                File(raw).exists() -> retriever.setDataSource(raw)
                else -> return null
            }
            val bytes = retriever.embeddedPicture ?: return null
            decodeSampledBitmap(bytes, targetPx = 384)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun loadImageBitmap(context: Context, source: String?): Bitmap? {
        val raw = source?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return try {
            when {
                raw.startsWith("http://") || raw.startsWith("https://") -> {
                    val connection = (URL(raw).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                        instanceFollowRedirects = true
                    }
                    try {
                        if (connection.responseCode in 200..299) {
                            val bytes = connection.inputStream.use { it.readBytes() }
                            decodeSampledBitmap(bytes, targetPx = 384)
                        } else null
                    } finally {
                        connection.disconnect()
                    }
                }
                raw.startsWith("content://") || raw.startsWith("file://") -> {
                    context.contentResolver.openInputStream(Uri.parse(raw))?.use { stream ->
                        decodeSampledBitmap(stream.readBytes(), targetPx = 384)
                    }
                }
                raw.startsWith("/") && File(raw).exists() -> {
                    decodeSampledBitmap(File(raw).readBytes(), targetPx = 384)
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeSampledBitmap(bytes: ByteArray, targetPx: Int): Bitmap? {
        if (bytes.isEmpty()) return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        var sampleSize = 1
        while (opts.outWidth / sampleSize > targetPx || opts.outHeight / sampleSize > targetPx) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
    }
}
