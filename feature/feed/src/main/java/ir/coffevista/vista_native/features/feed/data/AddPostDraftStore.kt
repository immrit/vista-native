package ir.coffevista.vista_native.features.feed.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PostDraft(
    val content: String = "",
    val location: String? = null,
    val aspectRatio: String? = null,
    val hideLikeCount: Boolean = false,
    val hideCommentCount: Boolean = false,
    val commentsDisabled: Boolean = false,
    val imagePaths: List<String> = emptyList(),
    val videoPath: String? = null,
    val videoSourceUri: String? = null,
    val videoDurationMs: Long = 0L,
    val musicPath: String? = null,
    val musicUrl: String? = null,
    val musicTitle: String? = null,
    val musicStartMs: Int = 0,
    val musicEndMs: Int = 0,
    val musicDurationMs: Int = 0,
    val isMusicBackgroundMode: Boolean = false,
    val savedAtEpochMs: Long = System.currentTimeMillis(),
) {
    fun hasContent(): Boolean =
        content.isNotBlank() ||
            !location.isNullOrBlank() ||
            imagePaths.isNotEmpty() ||
            !videoPath.isNullOrBlank() ||
            !musicPath.isNullOrBlank() ||
            !musicUrl.isNullOrBlank()
}

internal object PostDraftCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(draft: PostDraft): String = json.encodeToString(draft)

    fun decode(raw: String): PostDraft? = runCatching {
        json.decodeFromString<PostDraft>(raw)
    }.getOrNull()
}

interface PostDraftStorage {
    fun saveDraft(userId: String = "", draft: PostDraft)
    fun getDraft(userId: String = ""): PostDraft?
    fun clearDraft(userId: String = "")
    fun persistImages(userId: String, uris: List<Uri>): List<String>
    fun persistVideo(userId: String, videoFile: File?, videoUri: Uri?): String?
    fun persistMusic(userId: String, musicUri: Uri?): String?
    fun isDraftFile(file: File): Boolean
    fun copyDraftVideoForUpload(videoFile: File): File
    fun copyDraftFileToCache(file: File, prefix: String, suffix: String): File
}

@Singleton
class AddPostDraftStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : PostDraftStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val draftsRootDir: File
        get() = File(context.filesDir, DRAFTS_DIR_NAME)

    fun saveDraft(draft: PostDraft) {
        saveDraft("", draft)
    }

    override fun saveDraft(userId: String, draft: PostDraft) {
        if (!draft.hasContent()) {
            clearDraft(userId)
            return
        }
        val encoded = PostDraftCodec.encode(draft)
        prefs.edit()
            .putString(jsonKey(userId), encoded)
            .commit()
    }

    override fun getDraft(userId: String): PostDraft? {
        val rawJson = prefs.getString(jsonKey(userId), null)
            ?: if (userId.isNotBlank()) prefs.getString(jsonKey(""), null) else null
        val decoded = rawJson?.let(PostDraftCodec::decode) ?: readLegacyDraftOrNull() ?: return null
        val existingImages = decoded.imagePaths.filter { path ->
            File(path).let { it.exists() && it.length() > 0L }
        }
        val existingVideo = decoded.videoPath?.takeIf { path ->
            File(path).let { it.exists() && it.length() > 0L }
        }
        val existingMusic = decoded.musicPath?.takeIf { path ->
            File(path).let { it.exists() && it.length() > 0L }
        }
        val sanitized = decoded.copy(
            imagePaths = existingImages,
            videoPath = existingVideo,
            videoDurationMs = if (existingVideo != null) decoded.videoDurationMs else 0L,
            musicPath = existingMusic,
        )
        return sanitized.takeIf { it.hasContent() }
    }

    override fun clearDraft(userId: String) {
        prefs.edit()
            .remove(jsonKey(userId))
            .remove(jsonKey(""))
            .remove(KEY_CONTENT)
            .remove(KEY_LOCATION)
            .remove(KEY_ASPECT_RATIO)
            .remove(KEY_HIDE_LIKES)
            .remove(KEY_HIDE_COMMENTS)
            .remove(KEY_COMMENTS_DISABLED)
            .remove(KEY_SAVED_AT)
            .commit()
        runCatching {
            File(draftsRootDir, safeUserKey(userId)).deleteRecursively()
        }
        if (userId.isNotBlank()) {
            runCatching {
                File(draftsRootDir, safeUserKey("")).deleteRecursively()
            }
        }
    }

    override fun persistImages(userId: String, uris: List<Uri>): List<String> {
        val dir = userDraftDir(userId)
        if (uris.isEmpty()) {
            dir.listFiles()?.filter { it.name.startsWith("img_") }?.forEach { runCatching { it.delete() } }
            return emptyList()
        }
        val keptPaths = ArrayList<String>(uris.size)
        uris.forEachIndexed { index, uri ->
            val directFile = uri.toExistingLocalFileOrNull()
            if (directFile != null && isInsideDir(directFile, dir)) {
                keptPaths += directFile.absolutePath
                return@forEachIndexed
            }
            val fileName = "img_${index}_${uri.toString().hashCode().toUInt()}.jpg"
            val target = File(dir, fileName)
            if (target.exists() && target.length() > 0L) {
                keptPaths += target.absolutePath
                return@forEachIndexed
            }
            val copied = copyUriOrFileToTarget(uri = uri, sourceFile = directFile, target = target)
            if (copied) {
                keptPaths += target.absolutePath
            }
        }
        val keptSet = keptPaths.toSet()
        dir.listFiles()
            ?.filter { it.name.startsWith("img_") && it.absolutePath !in keptSet }
            ?.forEach { runCatching { it.delete() } }
        return keptPaths
    }

    override fun persistVideo(userId: String, videoFile: File?, videoUri: Uri?): String? {
        val dir = userDraftDir(userId)
        val target = File(dir, "draft_video.mp4")
        val existingSource = videoFile?.takeIf { it.exists() && it.length() > 0L }
        if (existingSource != null) {
            if (isSameCanonicalFile(existingSource, target)) {
                return target.absolutePath
            }
            if (copyFileAtomically(existingSource, target)) {
                return target.absolutePath
            }
        }
        if (videoUri != null) {
            val uriFile = videoUri.toExistingLocalFileOrNull()
            if (uriFile != null && isSameCanonicalFile(uriFile, target)) {
                return target.absolutePath
            }
            if (copyUriOrFileToTarget(uri = videoUri, sourceFile = uriFile, target = target)) {
                return target.absolutePath
            }
        }
        runCatching { target.delete() }
        return null
    }

    override fun persistMusic(userId: String, musicUri: Uri?): String? {
        val dir = userDraftDir(userId)
        if (musicUri == null) {
            dir.listFiles()?.filter { it.name.startsWith("draft_music") }?.forEach { runCatching { it.delete() } }
            return null
        }
        val directFile = musicUri.toExistingLocalFileOrNull()
        if (directFile != null && isInsideDir(directFile, dir)) {
            return directFile.absolutePath
        }
        val ext = resolveAudioExtension(musicUri, directFile)
        val target = File(dir, "draft_music.$ext")
        val copied = copyUriOrFileToTarget(uri = musicUri, sourceFile = directFile, target = target)
        if (!copied) {
            return null
        }
        dir.listFiles()
            ?.filter { it.name.startsWith("draft_music") && it.absolutePath != target.absolutePath }
            ?.forEach { runCatching { it.delete() } }
        return target.absolutePath
    }

    override fun isDraftFile(file: File): Boolean = isInsideDir(file, draftsRootDir)

    override fun copyDraftVideoForUpload(videoFile: File): File =
        copyDraftFileToCache(videoFile, "upload-video-", ".mp4")

    override fun copyDraftFileToCache(file: File, prefix: String, suffix: String): File {
        if (!isDraftFile(file) || !file.exists()) return file
        val temp = File.createTempFile(prefix, suffix, context.cacheDir)
        file.copyTo(temp, overwrite = true)
        return temp
    }

    private fun readLegacyDraftOrNull(): PostDraft? {
        val content = prefs.getString(KEY_CONTENT, null).orEmpty()
        val location = prefs.getString(KEY_LOCATION, null)
        if (content.isBlank() && location.isNullOrBlank()) return null
        return PostDraft(
            content = content,
            location = location,
            aspectRatio = prefs.getString(KEY_ASPECT_RATIO, null),
            hideLikeCount = prefs.getBoolean(KEY_HIDE_LIKES, false),
            hideCommentCount = prefs.getBoolean(KEY_HIDE_COMMENTS, false),
            commentsDisabled = prefs.getBoolean(KEY_COMMENTS_DISABLED, false),
            savedAtEpochMs = prefs.getLong(KEY_SAVED_AT, 0L),
        )
    }

    private fun safeUserKey(userId: String): String =
        userId.trim().filter { it.isLetterOrDigit() || it == '_' || it == '-' }.ifBlank { "default" }

    private fun jsonKey(userId: String): String = "draft_json_${safeUserKey(userId)}"

    private fun userDraftDir(userId: String): File =
        File(draftsRootDir, safeUserKey(userId)).apply { mkdirs() }

    private fun Uri.toExistingLocalFileOrNull(): File? {
        if (!scheme.equals("file", ignoreCase = true)) return null
        val p = path ?: toString().removePrefix("file://")
        return File(p).takeIf { it.exists() && it.length() > 0L }
    }

    private fun openStream(uri: Uri, sourceFile: File?): InputStream? {
        if (sourceFile != null && sourceFile.exists()) return sourceFile.inputStream()
        return runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
    }

    private fun copyFileAtomically(source: File, target: File): Boolean {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        return runCatching {
            source.copyTo(tmp, overwrite = true)
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            target.exists() && target.length() > 0L
        }.getOrElse {
            runCatching { tmp.delete() }
            false
        }
    }

    private fun copyUriOrFileToTarget(uri: Uri, sourceFile: File?, target: File): Boolean {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        return runCatching {
            val stream = openStream(uri, sourceFile) ?: return false
            stream.use { input ->
                tmp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (tmp.length() <= 0L) {
                tmp.delete()
                return false
            }
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            target.exists() && target.length() > 0L
        }.getOrElse {
            runCatching { tmp.delete() }
            false
        }
    }

    private fun resolveAudioExtension(uri: Uri, file: File?): String {
        val fromFile = file?.extension?.lowercase()?.takeIf { it.isNotBlank() && it.length <= 5 }
        if (fromFile != null) return fromFile
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        val fromMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?.lowercase()
            ?.takeIf { it.isNotBlank() && it.length <= 5 }
        if (fromMime != null) return fromMime
        val segment = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
            ?.filter { it.isLetterOrDigit() }
            ?.takeIf { it.isNotBlank() && it.length <= 5 }
        return segment ?: "mp3"
    }

    private fun isInsideDir(file: File, dir: File): Boolean = runCatching {
        val rootPath = dir.canonicalPath
        val filePath = file.canonicalPath
        filePath == rootPath || filePath.startsWith(rootPath + File.separator)
    }.getOrDefault(false)

    private fun isSameCanonicalFile(a: File, b: File): Boolean = runCatching {
        a.canonicalPath == b.canonicalPath
    }.getOrDefault(false)

    private companion object {
        const val PREFS_NAME = "vista_post_draft_store"
        const val DRAFTS_DIR_NAME = "post_drafts"
        const val KEY_CONTENT = "draft_content"
        const val KEY_LOCATION = "draft_location"
        const val KEY_ASPECT_RATIO = "draft_aspect_ratio"
        const val KEY_HIDE_LIKES = "draft_hide_likes"
        const val KEY_HIDE_COMMENTS = "draft_hide_comments"
        const val KEY_COMMENTS_DISABLED = "draft_comments_disabled"
        const val KEY_SAVED_AT = "draft_saved_at"
    }
}
