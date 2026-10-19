package ir.coffevista.vista_native.features.feed.music.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadStatus {
    data object NotDownloaded : DownloadStatus
    data class Downloading(val progress: Float) : DownloadStatus
    data class Downloaded(val localPath: String) : DownloadStatus
    data class Failed(val error: String) : DownloadStatus
}

data class DownloadInfo(
    val status: DownloadStatus = DownloadStatus.NotDownloaded,
    val progress: Float = 0f,
    val localPath: String? = null,
)

@Singleton
class MusicDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = OkHttpClient()
    private val _downloadStates = MutableStateFlow<Map<String, DownloadInfo>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadInfo>> = _downloadStates.asStateFlow()

    private val musicDir: File
        get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "music").apply {
            if (!exists()) mkdirs()
        }

    fun isDownloaded(url: String): Boolean {
        val normalized = url.trim()
        val info = _downloadStates.value[normalized]
        if (info?.status is DownloadStatus.Downloaded && info.localPath != null) {
            return File(info.localPath).exists()
        }
        val file = getTargetFile(normalized)
        return file.exists() && file.length() > 0
    }

    fun getDownloadPath(url: String): String? {
        val normalized = url.trim()
        val file = getTargetFile(normalized)
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    suspend fun downloadMusic(
        url: String,
        onProgress: ((Float) -> Unit)? = null,
    ): String? = withContext(Dispatchers.IO) {
        val normalized = url.trim()
        if (normalized.isEmpty()) return@withContext null

        val targetFile = getTargetFile(normalized)
        if (targetFile.exists() && targetFile.length() > 0) {
            val path = targetFile.absolutePath
            _downloadStates.update { current ->
                current + (normalized to DownloadInfo(
                    status = DownloadStatus.Downloaded(path),
                    progress = 1f,
                    localPath = path,
                ))
            }
            return@withContext path
        }

        _downloadStates.update { current ->
            current + (normalized to DownloadInfo(
                status = DownloadStatus.Downloading(0f),
                progress = 0f,
            ))
        }

        try {
            val request = Request.Builder().url(normalized).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed with HTTP ${response.code}")
            }

            val body = response.body ?: throw IllegalStateException("Response body is null")
            val totalBytes = body.contentLength()
            val tempFile = File(musicDir, "${targetFile.name}.tmp")

            body.byteStream().use { input: InputStream ->
                FileOutputStream(tempFile).use { output: FileOutputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead: Long = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        if (totalBytes > 0) {
                            val progress = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            _downloadStates.update { current ->
                                current + (normalized to DownloadInfo(
                                    status = DownloadStatus.Downloading(progress),
                                    progress = progress,
                                ))
                            }
                            onProgress?.invoke(progress)
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.renameTo(targetFile)) {
                val finalPath = targetFile.absolutePath
                _downloadStates.update { current ->
                    current + (normalized to DownloadInfo(
                        status = DownloadStatus.Downloaded(finalPath),
                        progress = 1f,
                        localPath = finalPath,
                    ))
                }
                onProgress?.invoke(1f)
                finalPath
            } else {
                throw IllegalStateException("Failed to finalize downloaded file")
            }
        } catch (e: Exception) {
            _downloadStates.update { current ->
                current + (normalized to DownloadInfo(
                    status = DownloadStatus.Failed(e.message ?: "دانلود ناموفق بود"),
                    progress = 0f,
                ))
            }
            null
        }
    }

    suspend fun deleteDownloadedFile(url: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = url.trim()
        val file = getTargetFile(normalized)
        val deleted = if (file.exists()) file.delete() else false
        _downloadStates.update { current ->
            current + (normalized to DownloadInfo(
                status = DownloadStatus.NotDownloaded,
                progress = 0f,
                localPath = null,
            ))
        }
        deleted
    }

    private fun getTargetFile(url: String): File {
        val sanitized = url.substringAfterLast('/').substringBefore('?').ifBlank { "track_${url.hashCode()}.mp3" }
        return File(musicDir, sanitized)
    }
}
