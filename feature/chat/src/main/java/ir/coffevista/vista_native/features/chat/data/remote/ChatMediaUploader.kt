package ir.coffevista.vista_native.features.chat.data.remote

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import ir.coffevista.vista_native.features.chat.data.mapper.epochMillisOrNull
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft
import java.io.File
import java.io.InputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink

data class UploadedChatMedia(
    val objectUrl: String,
    val objectKey: String,
)

data class PreparedChatUpload(
    val draft: ChatAttachmentDraft,
    val objectKey: String,
)

class ChatMediaUploader(
    context: Context,
    private val api: ChatApi,
    private val externalClient: OkHttpClient,
) {
    private val resolver: ContentResolver = context.contentResolver
    private val activeCalls = ConcurrentHashMap<String, ActiveUpload>()
    private val policy = ChatUploadPolicy()

    /** Validate before creating an optimistic message; privilege lookup fails closed above 15MB. */
    suspend fun prepare(
        accountId: String,
        conversationId: String,
        draft: ChatAttachmentDraft,
        existingObjectKey: String? = null,
    ): PreparedChatUpload = withContext(Dispatchers.IO) {
        val profile = if (draft.sizeBytes > ChatUploadPolicy.NORMAL_MAX_BYTES) {
            runCatching { api.ownUploadProfile().toUploadProfile() }.getOrNull()
        } else {
            null
        }
        val validated = policy.validate(
            fileName = draft.fileName,
            declaredMimeType = draft.mimeType,
            declaredKind = draft.kind,
            sizeBytes = draft.sizeBytes,
            header = readHeader(Uri.parse(draft.uri)),
            profile = profile,
        )
        val normalized = draft.copy(kind = validated.kind, mimeType = validated.mimeType)
        PreparedChatUpload(
            draft = normalized,
            objectKey = existingObjectKey?.trim()?.takeIf(String::isNotEmpty)
                ?: buildObjectKey(accountId, conversationId, validated.kind, validated.extension),
        )
    }

    suspend fun upload(
        accountId: String,
        clientId: String,
        prepared: PreparedChatUpload,
        onProgress: (Float) -> Unit,
    ): UploadedChatMedia = withContext(Dispatchers.IO) {
        val draft = prepared.draft
        val presign = api.presignUpload(
            PresignUploadRequest(
                objectKey = prepared.objectKey,
                contentType = draft.mimeType,
                fileSize = draft.sizeBytes,
            ),
        )
        val body = ContentUriRequestBody(
            resolver = resolver,
            uri = Uri.parse(draft.uri),
            contentType = draft.mimeType,
            contentLength = draft.sizeBytes,
            onProgress = onProgress,
        )
        val builder = Request.Builder().url(presign.url).put(body)
        presign.headers.forEach { (name, value) ->
            if (!name.equals("host", ignoreCase = true)) builder.header(name, value)
        }
        val call = externalClient.newCall(builder.build())
        val activeUpload = ActiveUpload(accountId, call)
        check(activeCalls.putIfAbsent(clientId, activeUpload) == null) { "انتقال فایل از قبل فعال است" }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("آپلود فایل با خطای ${response.code} متوقف شد")
            }
        } finally {
            activeCalls.remove(clientId, activeUpload)
        }
        onProgress(1f)
        UploadedChatMedia(presign.objectUrl, presign.objectKey)
    }

    fun cancel(clientId: String): Boolean = activeCalls.remove(clientId)?.let { upload ->
        upload.call.cancel()
        true
    } ?: false

    fun cancelAccount(accountId: String) {
        activeCalls.entries
            .filter { it.value.accountId == accountId }
            .forEach { (clientId, upload) ->
                if (activeCalls.remove(clientId, upload)) upload.call.cancel()
            }
    }

    suspend fun deleteUploadedObject(objectKey: String) {
        if (objectKey.isNotBlank()) api.deleteUpload(DeleteUploadRequest(objectKey))
    }

    private fun readHeader(uri: Uri): ByteArray = openStream(resolver, uri)?.use { input ->
        val buffer = ByteArray(16)
        val read = input.read(buffer)
        if (read <= 0) byteArrayOf() else buffer.copyOf(read)
    } ?: throw IOException("فایل انتخاب‌شده قابل خواندن نیست")

    private fun buildObjectKey(
        accountId: String,
        conversationId: String,
        kind: AttachmentKind,
        extension: String,
    ): String {
        val folder = when (kind) {
            AttachmentKind.IMAGE, AttachmentKind.GIF -> "images"
            AttachmentKind.VOICE, AttachmentKind.AUDIO -> "audio"
            AttachmentKind.DOCUMENT -> "documents"
            AttachmentKind.VIDEO, AttachmentKind.UNKNOWN -> "files"
        }
        return "chat/${accountId.safeSegment("user")}/${conversationId.safeSegment("chat")}/$folder/${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension"
    }

    private fun String.safeSegment(fallback: String): String =
        filter { it.isLetterOrDigit() || it == '_' || it == '-' }.ifBlank { fallback }
}

private fun openStream(resolver: ContentResolver, uri: Uri): InputStream? {
    val scheme = uri.scheme
    return if (scheme.isNullOrEmpty() || scheme == "file") {
        val path = uri.path ?: uri.toString()
        val file = File(path)
        if (file.exists() && file.canRead()) {
            file.inputStream()
        } else {
            runCatching { resolver.openInputStream(uri) }.getOrNull()
        }
    } else {
        runCatching { resolver.openInputStream(uri) }.getOrNull()
    }
}

private data class ActiveUpload(
    val accountId: String,
    val call: Call,
)

private fun UploadPrivilegeProfileDto.toUploadProfile() = ChatUploadProfile(
    role = role,
    accountType = accountType,
    isVerified = isVerified,
    verificationType = verificationType,
    premiumDaysRemaining = premiumDaysRemaining,
    subscriptionExpiresAtEpochMillis = subscriptionExpiresAt.epochMillisOrNull(),
)

private class ContentUriRequestBody(
    private val resolver: ContentResolver,
    private val uri: Uri,
    private val contentType: String,
    private val contentLength: Long,
    private val onProgress: (Float) -> Unit,
) : RequestBody() {
    override fun contentType() = contentType.toMediaType()

    override fun contentLength(): Long = contentLength

    override fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var sent = 0L
        openStream(resolver, uri)?.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                sink.write(buffer, 0, read)
                sent += read
                onProgress((sent.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f))
            }
        } ?: throw IOException("فایل انتخاب‌شده قابل خواندن نیست")
    }
}
