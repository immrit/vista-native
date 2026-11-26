package ir.coffevista.vista_native.features.chat.presentation.components

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

internal data class ChatAttachmentSaveSpec(
    val displayName: String,
    val mimeType: String,
    val relativePath: String,
    val environmentDir: String,
    val successMessage: String,
)

private val SAVE_FALLBACK_HTTP_CLIENT: OkHttpClient by lazy { OkHttpClient() }

private val EXTENSION_BY_MIME = mapOf(
    "image/jpeg" to "jpg",
    "image/jpg" to "jpg",
    "image/png" to "png",
    "image/webp" to "webp",
    "image/gif" to "gif",
    "video/mp4" to "mp4",
    "video/webm" to "webm",
    "video/3gpp" to "3gp",
    "audio/mpeg" to "mp3",
    "audio/mp3" to "mp3",
    "audio/mp4" to "m4a",
    "audio/x-m4a" to "m4a",
    "audio/aac" to "aac",
    "audio/ogg" to "ogg",
    "audio/wav" to "wav",
    "audio/flac" to "flac",
    "application/pdf" to "pdf",
    "application/zip" to "zip",
    "application/x-zip-compressed" to "zip",
    "text/plain" to "txt",
)

private val MIME_BY_EXTENSION = mapOf(
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "png" to "image/png",
    "webp" to "image/webp",
    "gif" to "image/gif",
    "mp4" to "video/mp4",
    "webm" to "video/webm",
    "3gp" to "video/3gpp",
    "mkv" to "video/x-matroska",
    "mov" to "video/quicktime",
    "mp3" to "audio/mpeg",
    "m4a" to "audio/mp4",
    "aac" to "audio/aac",
    "ogg" to "audio/ogg",
    "wav" to "audio/wav",
    "flac" to "audio/flac",
    "pdf" to "application/pdf",
    "zip" to "application/zip",
    "rar" to "application/vnd.rar",
    "7z" to "application/x-7z-compressed",
    "txt" to "text/plain",
    "doc" to "application/msword",
    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "xls" to "application/vnd.ms-excel",
    "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "ppt" to "application/vnd.ms-powerpoint",
    "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "apk" to "application/vnd.android.package-archive",
)

internal fun isSecretLocalUri(uriOrPath: String?): Boolean {
    val value = uriOrPath?.trim()?.lowercase(Locale.US).orEmpty()
    if (value.isEmpty()) return false
    return value.contains("sec_up_") ||
        value.contains("enc_chat_") ||
        value.contains("dec_enc_chat_") ||
        value.contains("temp_dec_") ||
        value.contains("/dec_") ||
        value.contains("secret_pending_uploads")
}

internal fun buildAttachmentSaveSpec(
    kind: AttachmentKind,
    fileName: String?,
    mimeType: String?,
    sourceUrl: String?,
    epochMillis: Long = System.currentTimeMillis(),
): ChatAttachmentSaveSpec {
    val sanitizedRawName = sanitizeSaveFileName(fileName)
    val normalizedMime = mimeType?.trim()?.lowercase(Locale.US)?.substringBefore(';')?.takeIf(String::isNotBlank)
    val extFromName = extractValidExtension(sanitizedRawName)
    val extFromUrl = extractValidExtension(extractUrlFileName(sourceUrl))
    val extFromMime = normalizedMime?.let { mime ->
        EXTENSION_BY_MIME[mime] ?: runCatching {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        }.getOrNull()
    }?.lowercase(Locale.US)?.takeIf(::isValidExtension)

    val defaultExt = when (kind) {
        AttachmentKind.GIF -> "gif"
        AttachmentKind.IMAGE -> "jpg"
        AttachmentKind.VIDEO -> "mp4"
        AttachmentKind.AUDIO -> "mp3"
        AttachmentKind.VOICE -> "m4a"
        AttachmentKind.DOCUMENT, AttachmentKind.UNKNOWN -> "bin"
    }

    val resolvedExt = extFromName ?: extFromMime ?: extFromUrl ?: defaultExt
    val baseName = sanitizedRawName
        ?.let { name ->
            val dotIdx = name.lastIndexOf('.')
            if (dotIdx > 0) name.substring(0, dotIdx) else name
        }
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: defaultBaseName(kind, epochMillis)

    val displayName = if (resolvedExt.isNotBlank()) "$baseName.$resolvedExt" else baseName

    val resolvedMime = normalizedMime
        ?: MIME_BY_EXTENSION[resolvedExt]
        ?: runCatching { MimeTypeMap.getSingleton()?.getMimeTypeFromExtension(resolvedExt) }.getOrNull()
        ?: when (kind) {
            AttachmentKind.GIF -> "image/gif"
            AttachmentKind.IMAGE -> "image/jpeg"
            AttachmentKind.VIDEO -> "video/mp4"
            AttachmentKind.AUDIO -> "audio/mpeg"
            AttachmentKind.VOICE -> "audio/mp4"
            AttachmentKind.DOCUMENT, AttachmentKind.UNKNOWN -> "application/octet-stream"
        }

    val picturesDir = (Environment.DIRECTORY_PICTURES as String?)?.takeIf(String::isNotBlank) ?: "Pictures"
    val moviesDir = (Environment.DIRECTORY_MOVIES as String?)?.takeIf(String::isNotBlank) ?: "Movies"
    val musicDir = (Environment.DIRECTORY_MUSIC as String?)?.takeIf(String::isNotBlank) ?: "Music"
    val downloadsDir = (Environment.DIRECTORY_DOWNLOADS as String?)?.takeIf(String::isNotBlank) ?: "Download"

    val (envDir, successMsg) = when (kind) {
        AttachmentKind.GIF -> picturesDir to "گیف در گالری ذخیره شد"
        AttachmentKind.IMAGE -> picturesDir to "تصویر در گالری ذخیره شد"
        AttachmentKind.VIDEO -> moviesDir to "ویدیو در گالری ذخیره شد"
        AttachmentKind.AUDIO -> musicDir to "موسیقی در پوشه موسیقی ذخیره شد"
        AttachmentKind.VOICE -> musicDir to "پیام صوتی در پوشه موسیقی ذخیره شد"
        AttachmentKind.DOCUMENT, AttachmentKind.UNKNOWN -> downloadsDir to "فایل در پوشه دانلودها ذخیره شد"
    }

    return ChatAttachmentSaveSpec(
        displayName = displayName,
        mimeType = resolvedMime,
        relativePath = "$envDir/Vista",
        environmentDir = envDir,
        successMessage = successMsg,
    )
}

internal suspend fun saveChatAttachmentToDevice(
    context: Context,
    sourceUriOrUrl: String,
    fallbackUrl: String? = null,
    kind: AttachmentKind,
    fileName: String? = null,
    mimeType: String? = null,
    httpClient: OkHttpClient? = null,
): Result<String> = withContext(Dispatchers.IO) {
    val candidates = listOfNotNull(
        sourceUriOrUrl.trim().takeIf(String::isNotBlank),
        fallbackUrl?.trim()?.takeIf { it.isNotBlank() && it != sourceUriOrUrl.trim() },
    )
    if (candidates.isEmpty()) {
        return@withContext Result.failure(IllegalArgumentException("منبع فایل برای ذخیره وجود ندارد"))
    }
    if (candidates.any(::isSecretLocalUri)) {
        return@withContext Result.failure(SecurityException("ذخیره رسانه در گفتگوی محرمانه مجاز نیست"))
    }

    val spec = buildAttachmentSaveSpec(
        kind = kind,
        fileName = fileName,
        mimeType = mimeType,
        sourceUrl = fallbackUrl ?: sourceUriOrUrl,
    )

    val stream = candidates.firstNotNullOfOrNull { candidate ->
        openAttachmentInputStream(context, candidate, httpClient)
    } ?: return@withContext Result.failure(IOException("دسترسی به فایل برای ذخیره ممکن نشد"))

    stream.use { input ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStoreQ(context, input, kind, spec)
        } else {
            saveViaLegacyExternalStorage(context, input, spec)
        }
    }
}

private fun saveViaMediaStoreQ(
    context: Context,
    input: InputStream,
    kind: AttachmentKind,
    spec: ChatAttachmentSaveSpec,
): Result<String> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return Result.failure(UnsupportedOperationException("MediaStore Q API unavailable"))
    }
    val resolver = context.contentResolver
    val collection: Uri = when (kind) {
        AttachmentKind.IMAGE, AttachmentKind.GIF ->
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        AttachmentKind.VIDEO ->
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        AttachmentKind.AUDIO, AttachmentKind.VOICE ->
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        AttachmentKind.DOCUMENT, AttachmentKind.UNKNOWN ->
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    }

    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, spec.displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, spec.mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, spec.relativePath)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
        if (kind == AttachmentKind.AUDIO) {
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
        }
    }

    val insertedUri = resolver.insert(collection, values)
        ?: return Result.failure(IOException("ایجاد فایل در حافظه دستگاه ممکن نشد"))

    return runCatching {
        resolver.openOutputStream(insertedUri)?.use { out ->
            input.copyTo(out)
            out.flush()
        } ?: throw IOException("باز کردن مسیر خروجی برای ذخیره ممکن نشد")

        val finishValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        resolver.update(insertedUri, finishValues, null, null)
        spec.successMessage
    }.onFailure {
        runCatching { resolver.delete(insertedUri, null, null) }
    }
}

@Suppress("DEPRECATION")
private fun saveViaLegacyExternalStorage(
    context: Context,
    input: InputStream,
    spec: ChatAttachmentSaveSpec,
): Result<String> {
    return runCatching {
        val baseDir = Environment.getExternalStoragePublicDirectory(spec.environmentDir)
        val dir = File(baseDir, "Vista")
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("ساخت پوشه مقصد ممکن نشد")
        }
        val destFile = resolveUniqueDestFile(dir, spec.displayName)
        destFile.outputStream().buffered().use { out ->
            input.copyTo(out)
            out.flush()
        }
        MediaScannerConnection.scanFile(
            context.applicationContext,
            arrayOf(destFile.absolutePath),
            arrayOf(spec.mimeType),
            null,
        )
        spec.successMessage
    }
}

private fun resolveUniqueDestFile(dir: File, displayName: String): File {
    val candidate = File(dir, displayName)
    if (!candidate.exists()) return candidate
    val dotIdx = displayName.lastIndexOf('.')
    val stem = if (dotIdx > 0) displayName.substring(0, dotIdx) else displayName
    val ext = if (dotIdx > 0) displayName.substring(dotIdx) else ""
    var counter = 1
    while (counter < 10_000) {
        val next = File(dir, "$stem ($counter)$ext")
        if (!next.exists()) return next
        counter++
    }
    return File(dir, "${stem}_${System.currentTimeMillis()}$ext")
}

private fun openAttachmentInputStream(
    context: Context,
    url: String,
    httpClient: OkHttpClient?,
): InputStream? {
    if (isSecretLocalUri(url)) return null
    val parsed = Uri.parse(url)
    return when {
        url.startsWith("/") -> {
            File(url).takeIf { it.isFile && it.canRead() }?.inputStream()
        }
        parsed.scheme == "file" -> {
            parsed.path?.let(::File)?.takeIf { it.isFile && it.canRead() }?.inputStream()
        }
        parsed.scheme == "content" -> {
            runCatching { context.contentResolver.openInputStream(parsed) }.getOrNull()
        }
        url.startsWith("http://") || url.startsWith("https://") -> {
            runCatching {
                val client = httpClient ?: SAVE_FALLBACK_HTTP_CLIENT
                val req = Request.Builder().url(url).build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    resp.close()
                    null
                } else {
                    resp.body?.byteStream() ?: run {
                        resp.close()
                        null
                    }
                }
            }.getOrNull()
        }
        else -> null
    }
}

private fun sanitizeSaveFileName(raw: String?): String? {
    val cleaned = raw
        ?.trim()
        ?.replace(Regex("[<>:\"/\\\\|?*\\p{Cntrl}]"), "_")
        ?.trim('.', ' ')
        ?.take(160)
    return cleaned?.takeIf(String::isNotBlank)
}

private fun extractUrlFileName(url: String?): String? {
    val clean = url?.substringBefore('?')?.substringBefore('#')?.substringAfterLast('/')
    return sanitizeSaveFileName(clean)
}

private fun extractValidExtension(name: String?): String? {
    val ext = name?.substringAfterLast('.', "")?.lowercase(Locale.US)?.takeIf(::isValidExtension)
    return ext
}

private fun isValidExtension(ext: String): Boolean = ext.matches(Regex("[a-z0-9]{1,8}"))

private fun defaultBaseName(kind: AttachmentKind, epochMillis: Long): String {
    val prefix = when (kind) {
        AttachmentKind.GIF -> "vista_gif"
        AttachmentKind.IMAGE -> "vista_img"
        AttachmentKind.VIDEO -> "vista_vid"
        AttachmentKind.AUDIO -> "vista_music"
        AttachmentKind.VOICE -> "vista_voice"
        AttachmentKind.DOCUMENT, AttachmentKind.UNKNOWN -> "vista_file"
    }
    return "${prefix}_$epochMillis"
}
