package ir.coffevista.vista_native.core.designsystem.component

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class VistaGalleryFilter {
    ALL,
    IMAGES,
    VIDEOS,
    GIFS,
}

enum class VistaFileCategoryFilter {
    ALL,
    PDF_ONLY,
    DOWNLOADS,
}

data class VistaGalleryMediaItem(
    val id: Long,
    val uriString: String,
    val isVideo: Boolean,
    val isGif: Boolean = false,
    val mimeType: String = "",
    val displayName: String = "",
    val bucketName: String = "",
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val dateAddedSec: Long = 0L,
) {
    fun toUri(): Uri = Uri.parse(uriString)
}

data class VistaAudioMediaItem(
    val id: Long,
    val uriString: String,
    val albumArtUriString: String?,
    val title: String,
    val artist: String,
    val album: String = "",
    val displayName: String = "",
    val mimeType: String = "",
    val extension: String = "",
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val dateAddedSec: Long = 0L,
) {
    fun toUri(): Uri = Uri.parse(uriString)
    fun toAlbumArtUri(): Uri? = albumArtUriString?.let(Uri::parse)
}

data class VistaDeviceFileItem(
    val id: Long,
    val uriString: String,
    val displayName: String,
    val extension: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateModifiedSec: Long,
    val folderLabel: String = "",
    val isDownload: Boolean = false,
) {
    fun toUri(): Uri = Uri.parse(uriString)
}

/**
 * توابع خالص (بدون وابستگی به Android runtime) برای جستجو، فیلتر و فرمت‌دهی داده‌های رسانه و فایل.
 */
object VistaMediaBrowserHelpers {
    val CHAT_ALLOWED_DOC_EXTENSIONS: Set<String> = setOf("pdf")
    val CHAT_ALLOWED_AUDIO_EXTENSIONS: Set<String> = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac")
    val CHAT_ALLOWED_VIDEO_EXTENSIONS: Set<String> = setOf("mp4", "mov", "webm", "mkv", "3gp", "m4v")
    val CHAT_ALLOWED_IMAGE_EXTENSIONS: Set<String> = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
    val CHAT_ALLOWED_ALL_FILE_EXTENSIONS: Set<String> =
        CHAT_ALLOWED_DOC_EXTENSIONS +
            CHAT_ALLOWED_AUDIO_EXTENSIONS +
            CHAT_ALLOWED_VIDEO_EXTENSIONS +
            CHAT_ALLOWED_IMAGE_EXTENSIONS

    private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

    fun toPersianDigits(text: String): String = buildString(text.length) {
        for (ch in text) {
            if (ch in '0'..'9') append(PERSIAN_DIGITS[ch - '0']) else append(ch)
        }
    }

    fun extensionFromFileName(fileName: String): String {
        val trimmed = fileName.trim().lowercase(Locale.ROOT)
        val dot = trimmed.lastIndexOf('.')
        return if (dot >= 0 && dot + 1 < trimmed.length) trimmed.substring(dot + 1) else ""
    }

    fun normalizeExtension(nameOrExt: String): String {
        val trimmed = nameOrExt.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return ""
        val dot = trimmed.lastIndexOf('.')
        return if (dot >= 0 && dot + 1 < trimmed.length) {
            trimmed.substring(dot + 1)
        } else if ('.' !in trimmed) {
            trimmed
        } else {
            ""
        }
    }

    fun inferExtension(displayName: String, mimeType: String): String {
        val fromName = extensionFromFileName(displayName)
        if (fromName.isNotEmpty()) return fromName
        return when (mimeType.trim().lowercase(Locale.ROOT).substringBefore(';')) {
            "application/pdf" -> "pdf"
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/mp4", "audio/m4a", "audio/x-m4a" -> "m4a"
            "audio/aac" -> "aac"
            "audio/wav", "audio/x-wav", "audio/vnd.wave" -> "wav"
            "audio/ogg", "application/ogg" -> "ogg"
            "audio/flac", "audio/x-flac" -> "flac"
            "video/mp4" -> "mp4"
            "video/quicktime" -> "mov"
            "video/webm" -> "webm"
            "video/x-matroska" -> "mkv"
            "video/3gpp" -> "3gp"
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> ""
        }
    }

    fun formatMediaDuration(durationMs: Long): String {
        if (durationMs <= 0L) return toPersianDigits("0:00")
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val formatted = if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
        return toPersianDigits(formatted)
    }

    fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0L) return toPersianDigits("0 کیلوبایت")
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        val raw = when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f گیگابایت", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f مگابایت", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f کیلوبایت", kb)
            else -> "$sizeBytes بایت"
        }
        return toPersianDigits(raw)
    }

    fun extractAlbumNames(items: List<VistaGalleryMediaItem>): List<String> =
        items.asSequence()
            .map { it.bucketName.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .toList()

    fun filterGalleryItems(
        items: List<VistaGalleryMediaItem>,
        filter: VistaGalleryFilter = VistaGalleryFilter.ALL,
        albumName: String? = null,
        query: String = "",
    ): List<VistaGalleryMediaItem> {
        val q = query.trim()
        val album = albumName?.trim().orEmpty()
        return items.filter { item ->
            val typeOk = when (filter) {
                VistaGalleryFilter.ALL -> true
                VistaGalleryFilter.IMAGES -> !item.isVideo
                VistaGalleryFilter.VIDEOS -> item.isVideo
                VistaGalleryFilter.GIFS -> !item.isVideo && (item.isGif || extensionFromFileName(item.displayName) == "gif")
            }
            val albumOk = album.isEmpty() || item.bucketName.equals(album, ignoreCase = true)
            val queryOk = q.isEmpty() ||
                item.displayName.contains(q, ignoreCase = true) ||
                item.bucketName.contains(q, ignoreCase = true)
            typeOk && albumOk && queryOk
        }
    }

    fun filterAudioItems(
        items: List<VistaAudioMediaItem>,
        query: String,
    ): List<VistaAudioMediaItem> {
        val q = query.trim()
        if (q.isEmpty()) return items
        return items.filter { item ->
            item.title.contains(q, ignoreCase = true) ||
                item.artist.contains(q, ignoreCase = true) ||
                item.album.contains(q, ignoreCase = true) ||
                item.displayName.contains(q, ignoreCase = true)
        }
    }

    fun filterFileItems(
        items: List<VistaDeviceFileItem>,
        query: String,
        category: VistaFileCategoryFilter = VistaFileCategoryFilter.ALL,
    ): List<VistaDeviceFileItem> {
        val q = query.trim()
        return items.filter { item ->
            val categoryOk = when (category) {
                VistaFileCategoryFilter.ALL -> true
                VistaFileCategoryFilter.PDF_ONLY -> item.extension.equals("pdf", ignoreCase = true)
                VistaFileCategoryFilter.DOWNLOADS -> item.isDownload
            }
            val queryOk = q.isEmpty() ||
                item.displayName.contains(q, ignoreCase = true) ||
                item.extension.contains(q, ignoreCase = true) ||
                item.folderLabel.contains(q, ignoreCase = true)
            categoryOk && queryOk
        }
    }
}

fun hasVistaGalleryPermission(
    context: Context,
    filter: VistaGalleryFilter = VistaGalleryFilter.ALL,
): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hasImages = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES,
        ) == PackageManager.PERMISSION_GRANTED
        val hasVideos = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VIDEO,
        ) == PackageManager.PERMISSION_GRANTED
        val hasSelectedPhotos = if (Build.VERSION.SDK_INT >= 34) {
            ContextCompat.checkSelfPermission(
                context,
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        when (filter) {
            VistaGalleryFilter.IMAGES, VistaGalleryFilter.GIFS -> hasImages || hasSelectedPhotos
            VistaGalleryFilter.VIDEOS -> hasVideos || hasSelectedPhotos
            VistaGalleryFilter.ALL -> hasImages || hasVideos || hasSelectedPhotos
        }
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

fun requiredVistaGalleryPermissions(
    filter: VistaGalleryFilter = VistaGalleryFilter.ALL,
): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        when (filter) {
            VistaGalleryFilter.IMAGES, VistaGalleryFilter.GIFS -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            VistaGalleryFilter.VIDEOS -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            VistaGalleryFilter.ALL -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        }
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

fun hasVistaAudioPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

fun requiredVistaAudioPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

fun hasVistaFilesReadPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        hasVistaGalleryPermission(context) || hasVistaAudioPermission(context)
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

fun requiredVistaFilesReadPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

fun openVistaAppSettings(context: Context) {
    runCatching {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/**
 * تبدیل Uri فایل مستقیم (`file://`) به Uri قابل خواندن از طریق ContentResolver و FileProvider.
 */
suspend fun ensureShareableContentUri(
    context: Context,
    uri: Uri,
    fallbackDisplayName: String = "",
): Uri = withContext(Dispatchers.IO) {
    if (uri.scheme != "file") return@withContext uri
    val path = uri.path ?: return@withContext uri
    val file = File(path)
    if (!file.exists() || !file.canRead()) return@withContext uri

    val authority = "${context.packageName}.files"
    // اگر فایل درون مسیرهای پوشه‌بندی FileProvider اپ باشد، مستقیماً تبدیل شود
    runCatching {
        val rootMatches = listOfNotNull(
            context.filesDir,
            context.cacheDir,
            context.getExternalFilesDir(null),
            context.externalCacheDir,
        ).any { root ->
            file.canonicalPath.startsWith(root.canonicalPath)
        }
        if (rootMatches) {
            return@withContext FileProvider.getUriForFile(context, authority, file)
        }
    }

    // در غیر این صورت برای تضمین دسترسی OpenableColumns در آپلود چت به کش امن کپی می‌شود
    return@withContext runCatching {
        val safeName = (fallbackDisplayName.ifBlank { file.name }).replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destDir = File(context.cacheDir, "chat_media").apply { mkdirs() }
        val destFile = File(destDir, "${System.currentTimeMillis()}_$safeName")
        file.inputStream().use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        FileProvider.getUriForFile(context, authority, destFile)
    }.getOrDefault(uri)
}

suspend fun queryVistaGalleryMedia(
    context: Context,
    filter: VistaGalleryFilter = VistaGalleryFilter.ALL,
    limit: Int = 240,
): List<VistaGalleryMediaItem> = withContext(Dispatchers.IO) {
    runCatching {
        val items = ArrayList<VistaGalleryMediaItem>(limit)
        val resolver = context.contentResolver

        if (filter != VistaGalleryFilter.VIDEOS) {
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            )
            val selection = if (filter == VistaGalleryFilter.GIFS) {
                "${MediaStore.Images.Media.MIME_TYPE} = ? OR LOWER(${MediaStore.Images.Media.DISPLAY_NAME}) LIKE ?"
            } else {
                null
            }
            val selectionArgs = if (filter == VistaGalleryFilter.GIFS) {
                arrayOf("image/gif", "%.gif")
            } else {
                null
            }
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                selection,
                selectionArgs,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                if (idCol >= 0) {
                    val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                    val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                    val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    var count = 0
                    while (cursor.moveToNext() && count < limit) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.safeString(nameCol)
                        val mime = cursor.safeString(mimeCol)
                        val isGif = mime.equals("image/gif", ignoreCase = true) ||
                            VistaMediaBrowserHelpers.extensionFromFileName(name) == "gif"
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        items += VistaGalleryMediaItem(
                            id = id,
                            uriString = uri.toString(),
                            isVideo = false,
                            isGif = isGif,
                            mimeType = mime,
                            displayName = name,
                            bucketName = cursor.safeString(bucketCol),
                            sizeBytes = cursor.safeLong(sizeCol),
                            dateAddedSec = cursor.safeLong(dateCol),
                        )
                        count++
                    }
                }
            }
        }

        if (filter == VistaGalleryFilter.ALL || filter == VistaGalleryFilter.VIDEOS) {
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            )
            resolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                if (idCol >= 0) {
                    val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                    val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                    val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                    val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    var count = 0
                    while (cursor.moveToNext() && count < limit) {
                        val id = cursor.getLong(idCol)
                        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        items += VistaGalleryMediaItem(
                            id = -id - 1_000_000L, // تمایز کلید با تصاویر
                            uriString = uri.toString(),
                            isVideo = true,
                            isGif = false,
                            mimeType = cursor.safeString(mimeCol),
                            displayName = cursor.safeString(nameCol),
                            bucketName = cursor.safeString(bucketCol),
                            durationMs = cursor.safeLong(durCol).coerceAtLeast(0L),
                            sizeBytes = cursor.safeLong(sizeCol).coerceAtLeast(0L),
                            dateAddedSec = cursor.safeLong(dateCol),
                        )
                        count++
                    }
                }
            }
        }

        items.sortedByDescending { it.dateAddedSec }.take(limit)
    }.getOrDefault(emptyList())
}

suspend fun queryVistaAudioTracks(
    context: Context,
    allowedExtensions: Set<String> = emptySet(),
    limit: Int = 250,
): List<VistaAudioMediaItem> = withContext(Dispatchers.IO) {
    runCatching {
        val items = ArrayList<VistaAudioMediaItem>(120)
        val seenKeys = HashSet<String>(120)
        val albumArtBase = Uri.parse("content://media/external/audio/albumart")
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val selection = "${MediaStore.Audio.Media.SIZE} > 1024 OR ${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            if (idCol >= 0) {
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.safeString(nameCol)
                    val mimeType = cursor.safeString(mimeCol)
                    val extension = VistaMediaBrowserHelpers.inferExtension(displayName, mimeType)
                    if (allowedExtensions.isNotEmpty() &&
                        (extension.isEmpty() || extension !in allowedExtensions)
                    ) {
                        continue
                    }

                    val rawTitle = cursor.safeString(titleCol)
                    val rawArtist = cursor.safeString(artistCol)
                    val cleanTitle = rawTitle.ifBlank {
                        displayName.substringBeforeLast('.').ifBlank { "آهنگ ${count + 1}" }
                    }
                    val cleanArtist = if (rawArtist.isBlank() || rawArtist.equals("<unknown>", ignoreCase = true)) {
                        "هنرمند ناشناس"
                    } else {
                        rawArtist
                    }
                    val albumId = cursor.safeLong(albumIdCol)
                    val sizeBytes = cursor.safeLong(sizeCol).coerceAtLeast(0L)
                    val durationMs = cursor.safeLong(durCol).coerceAtLeast(0L)
                    val dateSec = cursor.safeLong(dateCol)

                    val dedupKey = "${displayName.lowercase(Locale.ROOT)}|$sizeBytes|$durationMs"
                    if (!seenKeys.add(dedupKey)) continue

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )
                    val coverUri = if (albumId > 0L) {
                        ContentUris.withAppendedId(albumArtBase, albumId).toString()
                    } else {
                        null
                    }
                    items += VistaAudioMediaItem(
                        id = id,
                        uriString = contentUri.toString(),
                        albumArtUriString = coverUri,
                        title = cleanTitle,
                        artist = cleanArtist,
                        album = cursor.safeString(albumNameCol),
                        displayName = displayName,
                        mimeType = mimeType.ifBlank { "audio/mpeg" },
                        extension = extension.ifBlank { "mp3" },
                        durationMs = durationMs,
                        sizeBytes = sizeBytes,
                        dateAddedSec = dateSec,
                    )
                    count++
                }
            }
        }
        items
    }.getOrDefault(emptyList())
}

suspend fun queryVistaDeviceFiles(
    context: Context,
    allowedExtensions: Set<String> = VistaMediaBrowserHelpers.CHAT_ALLOWED_ALL_FILE_EXTENSIONS,
    limit: Int = 250,
): List<VistaDeviceFileItem> = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val items = ArrayList<VistaDeviceFileItem>(limit)
        val seenKeys = HashSet<String>(limit)

        fun tryAdd(item: VistaDeviceFileItem) {
            if (items.size >= limit * 2) return
            if (allowedExtensions.isNotEmpty() && item.extension !in allowedExtensions) return
            if (item.sizeBytes <= 0L) return
            val key = "${item.displayName.lowercase(Locale.ROOT)}|${item.sizeBytes}"
            if (seenKeys.add(key)) {
                items += item
            }
        }

        // ۱. فایل‌های MediaStore.Downloads (اندروید ۱۰ به بالا)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val dlProjection = arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.MIME_TYPE,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads.DATE_MODIFIED,
                MediaStore.Downloads.RELATIVE_PATH,
            )
            runCatching {
                resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    dlProjection,
                    "${MediaStore.Downloads.SIZE} > 0",
                    null,
                    "${MediaStore.Downloads.DATE_MODIFIED} DESC",
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(MediaStore.Downloads._ID)
                    if (idCol >= 0) {
                        val nameCol = cursor.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME)
                        val mimeCol = cursor.getColumnIndex(MediaStore.Downloads.MIME_TYPE)
                        val sizeCol = cursor.getColumnIndex(MediaStore.Downloads.SIZE)
                        val dateCol = cursor.getColumnIndex(MediaStore.Downloads.DATE_MODIFIED)
                        val pathCol = cursor.getColumnIndex(MediaStore.Downloads.RELATIVE_PATH)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            val name = cursor.safeString(nameCol)
                            val mime = cursor.safeString(mimeCol)
                            val ext = VistaMediaBrowserHelpers.inferExtension(name, mime)
                            val rel = cursor.safeString(pathCol).trim('/')
                            val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                            tryAdd(
                                VistaDeviceFileItem(
                                    id = id,
                                    uriString = uri.toString(),
                                    displayName = name.ifBlank { "file_$id.$ext" },
                                    extension = ext,
                                    mimeType = mime.ifBlank { guessMimeForExtension(ext) },
                                    sizeBytes = cursor.safeLong(sizeCol),
                                    dateModifiedSec = cursor.safeLong(dateCol),
                                    folderLabel = rel.ifBlank { "Download" },
                                    isDownload = true,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // ۲. فایل‌ها و اسناد از MediaStore.Files
        val fileColumns = ArrayList<String>(6).apply {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.DISPLAY_NAME)
            add(MediaStore.Files.FileColumns.MIME_TYPE)
            add(MediaStore.Files.FileColumns.SIZE)
            add(MediaStore.Files.FileColumns.DATE_MODIFIED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
            }
        }.toTypedArray()

        runCatching {
            resolver.query(
                MediaStore.Files.getContentUri("external"),
                fileColumns,
                "${MediaStore.Files.FileColumns.SIZE} > 0",
                null,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                if (idCol >= 0) {
                    val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                    val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    val relCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    } else {
                        -1
                    }
                    var scanned = 0
                    while (cursor.moveToNext() && scanned < limit * 4) {
                        scanned++
                        val id = cursor.getLong(idCol)
                        val name = cursor.safeString(nameCol)
                        val mime = cursor.safeString(mimeCol)
                        val ext = VistaMediaBrowserHelpers.inferExtension(name, mime)
                        if (ext.isEmpty()) continue
                        val relPath = cursor.safeString(relCol).trim('/')
                        val isDownload = relPath.contains("Download", ignoreCase = true)
                        val uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                        tryAdd(
                            VistaDeviceFileItem(
                                id = 10_000_000L + id,
                                uriString = uri.toString(),
                                displayName = name.ifBlank { "file_$id.$ext" },
                                extension = ext,
                                mimeType = mime.ifBlank { guessMimeForExtension(ext) },
                                sizeBytes = cursor.safeLong(sizeCol),
                                dateModifiedSec = cursor.safeLong(dateCol),
                                folderLabel = relPath.substringBeforeLast('/').ifBlank { relPath.ifBlank { "حافظه دستگاه" } },
                                isDownload = isDownload,
                            ),
                        )
                    }
                }
            }
        }

        // ۳. اسکن مستقیم پوشه‌های قابل خواندن (دانلودها، اسناد، فایل‌های ذخیره شده برنامه)
        val roots = listOfNotNull(
            runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) }.getOrNull(),
            runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) }.getOrNull(),
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
        )
        var diskId = -1L
        for (root in roots) {
            scanReadableDirectory(
                dir = root,
                depth = 0,
                maxDepth = 2,
                isDownloadRoot = root.name.contains("Download", ignoreCase = true),
            ) { file, isDownload ->
                val ext = VistaMediaBrowserHelpers.extensionFromFileName(file.name)
                if (ext.isEmpty() || (allowedExtensions.isNotEmpty() && ext !in allowedExtensions)) return@scanReadableDirectory
                diskId--
                val folderName = file.parentFile?.name.orEmpty().ifBlank { "حافظه دستگاه" }
                tryAdd(
                    VistaDeviceFileItem(
                        id = diskId,
                        uriString = Uri.fromFile(file).toString(),
                        displayName = file.name,
                        extension = ext,
                        mimeType = guessMimeForExtension(ext),
                        sizeBytes = file.length(),
                        dateModifiedSec = file.lastModified() / 1000L,
                        folderLabel = folderName,
                        isDownload = isDownload,
                    ),
                )
            }
        }

        // اولویت به اسناد PDF و فایل‌های دانلودی اخیر + مرتب‌سازی نزولی بر اساس تاریخ
        items.sortedWith(
            compareByDescending<VistaDeviceFileItem> { it.extension.equals("pdf", ignoreCase = true) }
                .thenByDescending { it.dateModifiedSec }
        ).take(limit)
    }.getOrDefault(emptyList())
}

private fun scanReadableDirectory(
    dir: File?,
    depth: Int,
    maxDepth: Int,
    isDownloadRoot: Boolean,
    onFile: (File, Boolean) -> Unit,
) {
    if (dir == null || !dir.exists() || !dir.isDirectory || !dir.canRead() || depth > maxDepth) return
    val children = runCatching { dir.listFiles() }.getOrNull() ?: return
    var visited = 0
    for (child in children) {
        if (visited > 150) break
        visited++
        val name = child.name
        if (name.startsWith(".")) continue
        if (child.isDirectory) {
            if (!name.equals("Android", ignoreCase = true)) {
                scanReadableDirectory(
                    dir = child,
                    depth = depth + 1,
                    maxDepth = maxDepth,
                    isDownloadRoot = isDownloadRoot || name.contains("Download", ignoreCase = true),
                    onFile = onFile,
                )
            }
        } else if (child.isFile && child.canRead() && child.length() > 0L) {
            onFile(child, isDownloadRoot)
        }
    }
}

private fun guessMimeForExtension(ext: String): String {
    val clean = ext.trim().lowercase(Locale.ROOT)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(clean)
        ?: when (clean) {
            "pdf" -> "application/pdf"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
}

private fun Cursor.safeString(columnIndex: Int): String {
    if (columnIndex < 0) return ""
    return runCatching { getString(columnIndex) }.getOrNull()?.trim().orEmpty()
}

private fun Cursor.safeLong(columnIndex: Int): Long {
    if (columnIndex < 0) return 0L
    return runCatching { getLong(columnIndex) }.getOrDefault(0L)
}
