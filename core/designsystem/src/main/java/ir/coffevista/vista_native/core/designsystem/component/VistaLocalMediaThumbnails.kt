package ir.coffevista.vista_native.core.designsystem.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

private object VistaLocalThumbnailCache {
    private val cache = LruCache<String, ImageBitmap>(120)

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }
}

private val thumbnailDecodeSemaphore = Semaphore(4)

/**
 * Decodes a thumbnail bitmap for a local gallery image or video URI, with reliable fallbacks
 * (including [MediaMetadataRetriever] for videos when `ContentResolver.loadThumbnail` is unavailable or fails).
 */
suspend fun loadVistaLocalMediaThumbnail(
    context: Context,
    uri: Uri,
    isVideo: Boolean,
    targetSizePx: Int = 320,
): Bitmap? = withContext(Dispatchers.IO) {
    thumbnailDecodeSemaphore.withPermit {
        val safeTarget = targetSizePx.coerceIn(64, 1024)
        if (isVideo) {
            decodeVideoThumbnail(context, uri, safeTarget)
        } else {
            decodeImageThumbnail(context, uri, safeTarget)
        }
    }
}

private fun decodeVideoThumbnail(
    context: Context,
    uri: Uri,
    targetSizePx: Int,
): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val osThumb = runCatching {
            context.contentResolver.loadThumbnail(uri, Size(targetSizePx, targetSizePx), null)
        }.getOrNull()
        if (osThumb != null) return osThumb
    }

    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            runCatching {
                retriever.getScaledFrameAtTime(
                    -1L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    targetSizePx,
                    targetSizePx,
                )
            }.getOrNull()
        } else {
            null
        } ?: runCatching {
            retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }.getOrNull() ?: runCatching {
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }.getOrNull() ?: return null

        return downscaleIfNeeded(rawBitmap, targetSizePx * 2)
    } catch (_: Throwable) {
        return null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun decodeImageThumbnail(
    context: Context,
    uri: Uri,
    targetSizePx: Int,
): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val osThumb = runCatching {
            context.contentResolver.loadThumbnail(uri, Size(targetSizePx, targetSizePx), null)
        }.getOrNull()
        if (osThumb != null) return osThumb
    }

    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetSizePx)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }.getOrNull()
}

private fun calculateInSampleSize(rawWidth: Int, rawHeight: Int, targetSizePx: Int): Int {
    if (rawWidth <= 0 || rawHeight <= 0) return 4
    var inSampleSize = 1
    while ((rawWidth / inSampleSize) > targetSizePx * 2 && (rawHeight / inSampleSize) > targetSizePx * 2) {
        inSampleSize *= 2
    }
    return inSampleSize.coerceAtLeast(1)
}

private fun downscaleIfNeeded(bitmap: Bitmap, maxDimensionPx: Int): Bitmap {
    val maxSide = max(bitmap.width, bitmap.height)
    if (maxSide <= maxDimensionPx || maxDimensionPx <= 0) return bitmap
    val scale = maxDimensionPx.toFloat() / maxSide.toFloat()
    val targetW = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
    val targetH = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    return runCatching {
        Bitmap.createScaledBitmap(bitmap, targetW, targetH, true).also { scaled ->
            if (scaled !== bitmap) bitmap.recycle()
        }
    }.getOrDefault(bitmap)
}

@Composable
fun VistaLocalMediaThumbnailImage(
    uri: Uri,
    isVideo: Boolean,
    targetSizePx: Int = 320,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cacheKey = remember(uri, isVideo, targetSizePx) {
        "${uri}_${if (isVideo) "v" else "i"}_$targetSizePx"
    }
    var bitmap by remember(cacheKey) { mutableStateOf(VistaLocalThumbnailCache.get(cacheKey)) }

    LaunchedEffect(cacheKey) {
        if (bitmap != null) return@LaunchedEffect
        val loaded = loadVistaLocalMediaThumbnail(
            context = context,
            uri = uri,
            isVideo = isVideo,
            targetSizePx = targetSizePx,
        )?.asImageBitmap()
        if (loaded != null) {
            VistaLocalThumbnailCache.put(cacheKey, loaded)
            bitmap = loaded
        }
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF1E2632).copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            if (isVideo) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
