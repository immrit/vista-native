package ir.coffevista.vista_native.features.chat.presentation.audio

import android.content.Context
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
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * استخراج و کش درون‌حافظه‌ای کاور آلبوم از فایل‌های صوتی محلی/کش‌شده
 * (مشابه `MediaMetadataRetriever.embeddedPicture` در تلگرام).
 */
object AudioCoverArtCache {
    private const val MAX_ENTRIES = 64
    private val cache = ConcurrentHashMap<String, CoverEntry>()

    private data class CoverEntry(val bitmap: ImageBitmap?)

    @Composable
    fun rememberCoverArt(
        url: String?,
        fallbackUrl: String? = null,
    ): ImageBitmap? {
        val context = LocalContext.current
        val key = rememberKey(url, fallbackUrl)
        val bitmap by produceState<ImageBitmap?>(initialValue = key?.let { cache[it]?.bitmap }, key1 = key) {
            if (key == null) {
                value = null
                return@produceState
            }
            cache[key]?.let {
                value = it.bitmap
                return@produceState
            }
            val loaded = withContext(Dispatchers.IO) {
                extractCover(context, url) ?: extractCover(context, fallbackUrl)
            }
            if (cache.size >= MAX_ENTRIES) {
                cache.keys.firstOrNull()?.let { cache.remove(it) }
            }
            cache[key] = CoverEntry(loaded)
            value = loaded
        }
        return bitmap
    }

    private fun rememberKey(url: String?, fallbackUrl: String?): String? {
        val primary = url?.trim()?.takeIf(String::isNotEmpty)
        val secondary = fallbackUrl?.trim()?.takeIf(String::isNotEmpty)
        return when {
            primary != null && secondary != null -> "$primary|$secondary"
            primary != null -> primary
            else -> secondary
        }
    }

    private fun extractCover(context: Context, source: String?): ImageBitmap? {
        val raw = source?.trim()?.takeIf(String::isNotEmpty) ?: return null
        // از فراخوانی شبکه روی ترد پس‌زمینه برای فایل‌های ریموت پرهیز می‌کنیم تا لیست اسکرول کند نشود؛
        // فایل‌های دانلودشده به URI محلی یا مسیر فایل تبدیل می‌شوند.
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
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            var sampleSize = 1
            val targetPx = 256
            while (opts.outWidth / sampleSize > targetPx || opts.outHeight / sampleSize > targetPx) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
            bmp.asImageBitmap()
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
