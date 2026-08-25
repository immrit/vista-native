package ir.coffevista.vista_native.features.chat.data.remote

import android.content.Context
import ir.coffevista.vista_native.features.chat.data.local.ChatDao
import ir.coffevista.vista_native.features.chat.data.local.DownloadTaskEntity
import ir.coffevista.vista_native.features.chat.domain.model.DownloadState
import ir.coffevista.vista_native.features.chat.domain.repository.ChatContentCipher
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

class ChatMediaDownloader(
    private val context: Context,
    private val client: OkHttpClient,
    private val dao: ChatDao,
    private val cipher: ChatContentCipher,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val activeCalls = ConcurrentHashMap<String, Call>()
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val recoveryStarted = ConcurrentHashMap.newKeySet<String>()

    suspend fun start(
        accountId: String,
        conversationId: String,
        messageId: String,
        remoteUrl: String,
        fileName: String,
        mimeType: String?,
    ): DownloadTaskEntity {
        require(remoteUrl.startsWith("https://")) { "نشانی دانلود فایل معتبر نیست" }
        val existing = dao.download(accountId, messageId)
        if (existing?.state == DownloadState.COMPLETE.name && existing.localPath?.let(::File)?.isFile == true) {
            return existing
        }
        val record = (existing ?: DownloadTaskEntity(
            accountId = accountId,
            conversationId = conversationId,
            messageId = messageId,
            remoteUrlCiphertext = cipher.encrypt(accountId, conversationId, "$messageId:download-url", remoteUrl),
            fileNameCiphertext = cipher.encrypt(accountId, conversationId, "$messageId:download-name", sanitizeDisplayName(fileName)),
            mimeType = mimeType,
            localPath = null,
            receivedBytes = 0,
            totalBytes = 0,
            eTag = null,
            state = DownloadState.QUEUED.name,
            attempts = 0,
            updatedAtEpochMillis = now(),
        )).copy(
            conversationId = conversationId,
            remoteUrlCiphertext = cipher.encrypt(accountId, conversationId, "$messageId:download-url", remoteUrl),
            fileNameCiphertext = cipher.encrypt(accountId, conversationId, "$messageId:download-name", sanitizeDisplayName(fileName)),
            mimeType = mimeType,
            state = DownloadState.QUEUED.name,
            attempts = existing?.attempts ?: 0,
            updatedAtEpochMillis = now(),
        )
        dao.upsertDownload(record)
        launch(record.accountId, record.messageId)
        return record
    }

    suspend fun pause(accountId: String, messageId: String) {
        val row = dao.download(accountId, messageId) ?: return
        dao.upsertDownload(row.copy(state = DownloadState.PAUSED.name, updatedAtEpochMillis = now()))
        val taskKey = key(accountId, messageId)
        activeCalls[taskKey]?.cancel()
        activeJobs[taskKey]?.join()
    }

    suspend fun resume(accountId: String, messageId: String) {
        val taskKey = key(accountId, messageId)
        activeCalls.remove(taskKey)?.cancel()
        activeJobs.remove(taskKey)?.cancelAndJoin()
        val row = dao.download(accountId, messageId) ?: return
        dao.upsertDownload(row.copy(state = DownloadState.QUEUED.name, updatedAtEpochMillis = now()))
        launch(accountId, messageId)
    }

    suspend fun cancel(accountId: String, messageId: String) {
        val row = dao.download(accountId, messageId) ?: return
        dao.upsertDownload(
            row.copy(
                localPath = null,
                receivedBytes = 0,
                totalBytes = 0,
                state = DownloadState.CANCELLED.name,
                updatedAtEpochMillis = now(),
            ),
        )
        activeCalls[key(accountId, messageId)]?.cancel()
        deleteTaskFiles(row)
    }

    fun recover(accountId: String) {
        if (!recoveryStarted.add(accountId)) return
        scope.launch(Dispatchers.IO) {
            dao.recoverableDownloads(accountId).forEach { row ->
                dao.upsertDownload(row.copy(state = DownloadState.QUEUED.name, updatedAtEpochMillis = now()))
                launch(row.accountId, row.messageId)
            }
            cleanupCache(accountId)
        }
    }

    suspend fun clearAccount(accountId: String) {
        activeCalls.keys.filter { it.startsWith("$accountId:") }.forEach { activeCalls.remove(it)?.cancel() }
        val jobs = activeJobs.keys
            .filter { it.startsWith("$accountId:") }
            .mapNotNull(activeJobs::remove)
        jobs.forEach(Job::cancel)
        jobs.forEach { it.cancelAndJoin() }
        accountDirectory(accountId).deleteRecursively()
        recoveryStarted.remove(accountId)
    }

    private fun launch(accountId: String, messageId: String) {
        val taskKey = key(accountId, messageId)
        if (activeJobs[taskKey]?.isActive == true) return
        activeJobs[taskKey] = scope.launch(Dispatchers.IO) {
            try {
                download(accountId, messageId)
            } finally {
                activeJobs.remove(taskKey)
                activeCalls.remove(taskKey)
            }
        }
    }

    private suspend fun download(accountId: String, messageId: String) {
        var row = dao.download(accountId, messageId) ?: return
        if (row.state !in setOf(DownloadState.QUEUED.name, DownloadState.DOWNLOADING.name)) return
        val remoteUrl = cipher.decrypt(accountId, row.conversationId, "$messageId:download-url", row.remoteUrlCiphertext)
        val fileName = cipher.decrypt(accountId, row.conversationId, "$messageId:download-name", row.fileNameCiphertext)
        val finalFile = finalFile(row, fileName)
        val partFile = File(finalFile.absolutePath + ".part")
        finalFile.parentFile?.mkdirs()
        var received = partFile.takeIf(File::isFile)?.length() ?: 0L

        try {
            var response = execute(accountId, messageId, remoteUrl, received)
            if (received > 0L && response.code != 206) {
                response.close()
                partFile.delete()
                received = 0L
                response = execute(accountId, messageId, remoteUrl, 0L)
            }
            response.use { activeResponse ->
                if (!activeResponse.isSuccessful) throw IOException("HTTP ${activeResponse.code}")
                val body = activeResponse.body ?: throw IOException("پاسخ دانلود خالی است")
                val contentLength = body.contentLength().coerceAtLeast(0L)
                val total = if (activeResponse.code == 206) received + contentLength else contentLength
                row = row.copy(
                    receivedBytes = received,
                    totalBytes = total,
                    eTag = activeResponse.header("ETag"),
                    state = DownloadState.DOWNLOADING.name,
                    attempts = row.attempts + 1,
                    updatedAtEpochMillis = now(),
                )
                dao.upsertDownload(row)
                // FileOutputStream append is required for HTTP Range recovery.
                java.io.FileOutputStream(partFile, received > 0L).buffered().use { output ->
                    val input = body.byteStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var lastPersisted = received
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        received += count
                        if (received - lastPersisted >= PROGRESS_STEP_BYTES) {
                            dao.advanceDownload(accountId, messageId, received, total, now())
                            lastPersisted = received
                        }
                    }
                    output.flush()
                }
                val current = dao.download(accountId, messageId) ?: return
                if (current.state != DownloadState.DOWNLOADING.name) return
                if (finalFile.exists()) finalFile.delete()
                check(partFile.renameTo(finalFile)) { "ذخیره فایل دانلودشده ممکن نیست" }
                dao.upsertDownload(
                    current.copy(
                        localPath = finalFile.absolutePath,
                        receivedBytes = received,
                        totalBytes = if (total > 0L) total else received,
                        state = DownloadState.COMPLETE.name,
                        updatedAtEpochMillis = now(),
                    ),
                )
            }
        } catch (error: Throwable) {
            val current = dao.download(accountId, messageId) ?: return
            if (current.state !in setOf(DownloadState.PAUSED.name, DownloadState.CANCELLED.name)) {
                dao.upsertDownload(current.copy(state = DownloadState.FAILED.name, updatedAtEpochMillis = now()))
            }
        }
    }

    private fun execute(accountId: String, messageId: String, url: String, offset: Long): okhttp3.Response {
        val request = Request.Builder().url(url).apply {
            if (offset > 0L) header("Range", "bytes=$offset-")
        }.build()
        val call = client.newCall(request)
        activeCalls[key(accountId, messageId)] = call
        return call.execute()
    }

    private fun finalFile(row: DownloadTaskEntity, displayName: String): File {
        val extension = displayName.substringAfterLast('.', "").lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
        val name = sha256(row.messageId) + extension?.let { ".$it" }.orEmpty()
        return File(accountDirectory(row.accountId), name)
    }

    private fun deleteTaskFiles(row: DownloadTaskEntity) {
        row.localPath?.let(::File)?.delete()
        val directory = accountDirectory(row.accountId)
        directory.listFiles()?.filter { it.name.startsWith(sha256(row.messageId)) }?.forEach(File::delete)
    }

    private suspend fun cleanupCache(accountId: String, maxAgeMillis: Long = CACHE_MAX_AGE_MILLIS) {
        val cutoff = now() - maxAgeMillis
        dao.downloadsForAccount(accountId).forEach { row ->
            val finalFile = row.localPath?.let(::File)
            val expired = finalFile?.isFile == true && finalFile.lastModified() < cutoff
            val missing = row.state == DownloadState.COMPLETE.name && finalFile?.isFile != true
            if (expired) finalFile.delete()
            if (expired || missing) {
                deleteTaskFiles(row)
                dao.upsertDownload(
                    row.copy(
                        localPath = null,
                        receivedBytes = 0,
                        totalBytes = 0,
                        state = DownloadState.CANCELLED.name,
                        updatedAtEpochMillis = now(),
                    ),
                )
            }
        }
        accountDirectory(accountId).listFiles()?.forEach { file ->
            if (file.name.endsWith(".part") && file.lastModified() < cutoff) file.delete()
        }
    }

    private fun accountDirectory(accountId: String) =
        File(context.filesDir, "chat_downloads/${sha256(accountId)}").apply { mkdirs() }

    private fun sanitizeDisplayName(value: String): String =
        value.trim().replace(Regex("[<>:\"/\\\\|?*\\p{Cntrl}]"), "_").take(180).ifBlank { "file" }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
        .take(32)

    private fun key(accountId: String, messageId: String) = "$accountId:$messageId"

    private companion object {
        const val PROGRESS_STEP_BYTES = 64L * 1024L
        const val CACHE_MAX_AGE_MILLIS = 30L * 24L * 60L * 60L * 1_000L
    }
}
