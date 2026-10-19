package ir.coffevista.vista_native.share

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Copies untrusted, temporary share grants into bounded app-owned cache files. */
internal class IncomingShareCache(private val context: Context) {
    suspend fun materialize(intent: Intent): SharedIncomingContent? = withContext(Dispatchers.IO) {
        val action = intent.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return@withContext null

        val mimeType = intent.type
            ?.trim()
            ?.takeIf(::isSafeMimeType)
            ?: DEFAULT_MIME_TYPE
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            ?.toString()
            ?.take(MAX_TEXT_CHARS)
            ?.takeIf(String::isNotBlank)
        val incomingUris = incomingUris(intent)
        val sourceUris = incomingUris
            .asSequence()
            .filter { it.scheme == ContentResolver.SCHEME_CONTENT }
            .distinct()
            .take(MAX_ITEMS)
            .toList()

        pruneExpiredFiles()
        var totalBytes = 0L
        var rejectedCount = incomingUris.distinct().count() - sourceUris.size
        val cachedUris = buildList {
            sourceUris.forEach { sourceUri ->
                val remainingTotal = MAX_TOTAL_BYTES - totalBytes
                val declaredSize = querySize(sourceUri)
                if (remainingTotal <= 0L || declaredSize != null && declaredSize > minOf(MAX_ITEM_BYTES, remainingTotal)) {
                    rejectedCount += 1
                    return@forEach
                }
                val cached = copyBounded(
                    sourceUri = sourceUri,
                    mimeType = context.contentResolver.getType(sourceUri)?.takeIf(::isSafeMimeType) ?: mimeType,
                    maxBytes = minOf(MAX_ITEM_BYTES, remainingTotal),
                )
                if (cached == null) {
                    rejectedCount += 1
                } else {
                    totalBytes += cached.length()
                    add(
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.files",
                            cached,
                        ),
                    )
                }
            }
        }

        if (text == null && cachedUris.isEmpty()) return@withContext null
        SharedIncomingContent(
            requestId = UUID.randomUUID().toString(),
            mimeType = mimeType,
            text = text,
            uris = cachedUris,
            rejectedItemCount = rejectedCount.coerceAtLeast(0),
        )
    }

    private fun incomingUris(intent: Intent): List<Uri> {
        val extras = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(streamUri(intent))
            Intent.ACTION_SEND_MULTIPLE -> streamUris(intent)
            else -> emptyList()
        }
        val clipped = intent.clipData?.let { clip ->
            buildList {
                repeat(clip.itemCount.coerceAtMost(MAX_ITEMS)) { index ->
                    clip.getItemAt(index).uri?.let(::add)
                }
            }
        }.orEmpty()
        return (extras + clipped).distinct().take(MAX_ITEMS + 1)
    }

    private fun streamUri(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

    private fun streamUris(intent: Intent): List<Uri> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        }

    private fun querySize(uri: Uri): Long? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index < 0 || cursor.isNull(index)) null else cursor.getLong(index).takeIf { it >= 0L }
        }
    }.getOrNull()

    private fun copyBounded(sourceUri: Uri, mimeType: String, maxBytes: Long): File? {
        val shareDirectory = File(context.cacheDir, CACHE_DIRECTORY).apply { mkdirs() }
        val destination = File(shareDirectory, "share_${UUID.randomUUID()}${extensionFor(mimeType)}")
        return runCatching {
            val input = context.contentResolver.openInputStream(sourceUri) ?: return null
            input.use { source ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        copied += read
                        if (copied > maxBytes) error("Incoming share exceeds the allowed cache size")
                        output.write(buffer, 0, read)
                    }
                }
            }
            destination.takeIf { it.isFile && it.length() > 0L }
                ?: error("Incoming share was empty")
        }.getOrElse {
            destination.delete()
            null
        }
    }

    private fun pruneExpiredFiles() {
        val cutoff = System.currentTimeMillis() - CACHE_RETENTION_MILLIS
        File(context.cacheDir, CACHE_DIRECTORY).listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) file.delete()
        }
    }

    internal companion object {
        const val MAX_ITEMS = 10
        const val MAX_TEXT_CHARS = 32_768
        const val MAX_ITEM_BYTES = 100L * 1024L * 1024L
        const val MAX_TOTAL_BYTES = 250L * 1024L * 1024L
        const val CACHE_DIRECTORY = "shared_incoming"
        private const val CACHE_RETENTION_MILLIS = 48L * 60L * 60L * 1000L
        private const val DEFAULT_MIME_TYPE = "application/octet-stream"
        private val SAFE_MIME_TYPE = Regex("^[a-zA-Z0-9!#$&^_.+*-]+/[a-zA-Z0-9!#$&^_.+*-]+$")

        internal fun isSafeMimeType(value: String): Boolean =
            value.length <= 127 && SAFE_MIME_TYPE.matches(value)

        internal fun extensionFor(mimeType: String): String = when {
            mimeType.equals("image/jpeg", ignoreCase = true) -> ".jpg"
            mimeType.equals("image/png", ignoreCase = true) -> ".png"
            mimeType.equals("image/gif", ignoreCase = true) -> ".gif"
            mimeType.equals("image/webp", ignoreCase = true) -> ".webp"
            mimeType.equals("video/mp4", ignoreCase = true) -> ".mp4"
            mimeType.startsWith("image/", ignoreCase = true) -> ".jpg"
            mimeType.startsWith("video/", ignoreCase = true) -> ".mp4"
            mimeType.startsWith("text/", ignoreCase = true) -> ".txt"
            else -> ".bin"
        }
    }
}
