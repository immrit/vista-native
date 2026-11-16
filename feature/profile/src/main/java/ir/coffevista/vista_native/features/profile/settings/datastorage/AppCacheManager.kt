package ir.coffevista.vista_native.features.profile.settings.datastorage

import android.content.Context
import coil.Coil
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.datastore.MediaDataPolicy
import ir.coffevista.vista_native.core.datastore.SettingsPreferenceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class MediaPruneResult(
    val deletedFiles: Int = 0,
    val freedBytes: Long = 0L,
)

enum class CacheCategory {
    IMAGES,
    VIDEOS,
    AUDIO,
    DOCUMENTS_OTHER,
}

data class CacheBreakdown(
    val totalBytes: Long = 0L,
    val imageBytes: Long = 0L,
    val videoBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val otherBytes: Long = 0L,
)

/**
 * مدیریت حرفه‌ای و دسته‌بندی‌شده حافظه موقت (کش) به سبک تلگرام.
 * شامل تفکیک حجم تصاویر، ویدیوها، فایل‌های صوتی و اسناد و قابلیت پاکسازی هوشمند.
 */
@Singleton
class AppCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceStore: SettingsPreferenceStore? = null,
) {
    suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        cacheRoots().sumOf(::sizeOf)
    }

    suspend fun calculateBreakdown(): CacheBreakdown = withContext(Dispatchers.IO) {
        var images = 0L
        var videos = 0L
        var audio = 0L
        var other = 0L

        cacheRoots().forEach { root ->
            traverseFiles(root) { file ->
                val size = file.length()
                when (classifyFile(file)) {
                    CacheCategory.IMAGES -> images += size
                    CacheCategory.VIDEOS -> videos += size
                    CacheCategory.AUDIO -> audio += size
                    CacheCategory.DOCUMENTS_OTHER -> other += size
                }
            }
        }

        CacheBreakdown(
            totalBytes = images + videos + audio + other,
            imageBytes = images,
            videoBytes = videos,
            audioBytes = audio,
            otherBytes = other,
        )
    }

    suspend fun clear(categories: Set<CacheCategory> = CacheCategory.entries.toSet()): Long = withContext(Dispatchers.IO) {
        var freed = 0L

        // پاکسازی فایل‌های دسته‌بندی‌شده
        cacheRoots().forEach { root ->
            traverseFiles(root) { file ->
                val cat = classifyFile(file)
                if (cat in categories) {
                    val len = file.length()
                    if (file.delete()) {
                        freed += len
                    }
                }
            }
        }

        // در صورت انتخاب تصاویر، کش Coil را نیز پاک می‌کنیم
        if (CacheCategory.IMAGES in categories) {
            runCatching {
                val imageLoader = Coil.imageLoader(context)
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()
            }
        }

        // پاکسازی دایرکتوری‌های خالی درون کش
        cacheRoots().forEach { root ->
            cleanEmptyDirectories(root)
        }

        freed
    }

    suspend fun pruneExpiredMedia(
        periodOverride: String? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): MediaPruneResult = withContext(Dispatchers.IO) {
        val period = periodOverride
            ?: runCatching { preferenceStore?.preferences?.first()?.keepMediaPeriod }.getOrNull()
            ?: "forever"
        val maxAgeMillis = MediaDataPolicy.keepMediaDurationMillis(period)
            ?: return@withContext MediaPruneResult()
        val cutoff = nowMillis - maxAgeMillis
        var deletedCount = 0
        var freedBytes = 0L
        cacheRoots().forEach { root ->
            traverseFiles(root) { file ->
                if (file.lastModified() < cutoff && shouldPruneFile(file)) {
                    val len = file.length()
                    if (file.delete()) {
                        deletedCount += 1
                        freedBytes += len
                    }
                }
            }
            cleanEmptyDirectories(root)
        }
        MediaPruneResult(deletedFiles = deletedCount, freedBytes = freedBytes)
    }

    private fun shouldPruneFile(file: File): Boolean {
        val name = file.name.lowercase()
        // Keep SimpleCache structural index files so active video cache metadata stays consistent.
        if (name == "cached_content_index.exi" || name == ".uid") return false
        return true
    }

    private fun classifyFile(file: File): CacheCategory {
        val name = file.name.lowercase()
        val path = file.absolutePath.lowercase()
        return when {
            path.contains("image_cache") || path.contains("coil") ||
                name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                name.endsWith(".webp") || name.endsWith(".gif") || name.endsWith(".bmp") -> CacheCategory.IMAGES

            path.contains("video") || path.contains("exoplayer") ||
                name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".mov") ||
                name.endsWith(".webm") || name.endsWith(".avi") -> CacheCategory.VIDEOS

            path.contains("audio") || path.contains("voice") || path.contains("music") ||
                name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".aac") ||
                name.endsWith(".wav") || name.endsWith(".ogg") -> CacheCategory.AUDIO

            else -> CacheCategory.DOCUMENTS_OTHER
        }
    }

    private fun traverseFiles(dir: File, onFile: (File) -> Unit) {
        if (!dir.exists()) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                traverseFiles(child, onFile)
            } else if (child.isFile) {
                onFile(child)
            }
        }
    }

    private fun cleanEmptyDirectories(dir: File) {
        if (!dir.exists() || !dir.isDirectory) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                cleanEmptyDirectories(child)
                if (child.listFiles()?.isEmpty() == true) {
                    child.delete()
                }
            }
        }
    }

    private fun cacheRoots(): List<File> = buildList {
        add(context.cacheDir)
        context.externalCacheDir?.let(::add)
        val chatDownloads = File(context.filesDir, "chat_downloads")
        if (chatDownloads.exists()) add(chatDownloads)
    }

    private fun sizeOf(file: File): Long = when {
        !file.exists() -> 0L
        file.isFile -> file.length()
        else -> file.listFiles()?.sumOf(::sizeOf) ?: 0L
    }
}
