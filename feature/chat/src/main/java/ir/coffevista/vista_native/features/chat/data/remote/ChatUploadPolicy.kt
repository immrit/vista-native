package ir.coffevista.vista_native.features.chat.data.remote

import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import java.util.Locale

internal enum class ChatUploadTier { NORMAL, PREMIUM, UNLIMITED }

internal data class ChatUploadProfile(
    val role: String?,
    val accountType: String?,
    val isVerified: Boolean,
    val verificationType: String?,
    val premiumDaysRemaining: Int?,
    val subscriptionExpiresAtEpochMillis: Long?,
)

internal data class ValidatedChatAttachment(
    val kind: AttachmentKind,
    val extension: String,
    val mimeType: String,
    val maxBytes: Long?,
)

internal class ChatUploadPolicy(
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun tier(profile: ChatUploadProfile?): ChatUploadTier {
        if (profile == null) return ChatUploadTier.NORMAL
        val badge = profile.verificationType.normalizedToken()
        if (profile.isVerified && badge in BLUE_BADGES) return ChatUploadTier.UNLIMITED
        if (profile.isVerified && badge in PREMIUM_BADGES) return ChatUploadTier.PREMIUM
        val role = profile.role.normalizedToken().ifBlank { profile.accountType.normalizedToken() }
        val activeSubscription = role == "premium" && (
            profile.premiumDaysRemaining?.let { it > 0 }
                ?: profile.subscriptionExpiresAtEpochMillis?.let { it > now() }
                ?: false
            )
        return if (activeSubscription) ChatUploadTier.PREMIUM else ChatUploadTier.NORMAL
    }

    fun maxBytes(profile: ChatUploadProfile?): Long? = when (tier(profile)) {
        ChatUploadTier.NORMAL -> NORMAL_MAX_BYTES
        ChatUploadTier.PREMIUM -> PREMIUM_MAX_BYTES
        ChatUploadTier.UNLIMITED -> null
    }

    fun validate(
        fileName: String,
        declaredMimeType: String,
        declaredKind: AttachmentKind,
        sizeBytes: Long,
        header: ByteArray,
        profile: ChatUploadProfile?,
    ): ValidatedChatAttachment {
        require(sizeBytes > 0L) { "فایل انتخاب‌شده خالی است" }
        val maxBytes = maxBytes(profile)
        require(maxBytes == null || sizeBytes <= maxBytes) {
            "حجم فایل بیشتر از ${maxBytes!! / (1024L * 1024L)} مگابایت است"
        }

        val extension = fileName.substringAfterLast('.', "")
            .lowercase(Locale.US)
            .filter(Char::isLetterOrDigit)
            .ifBlank { inferExtension(header) }
        require(extension.isNotBlank()) { "نوع فایل قابل تشخیص نیست" }

        val contentKind = detectKind(extension, header)
        val canonicalKind = when (declaredKind) {
            AttachmentKind.VOICE -> {
                require(contentKind == AttachmentKind.AUDIO) { "محتوای فایل صوتی معتبر نیست" }
                AttachmentKind.VOICE
            }
            AttachmentKind.IMAGE, AttachmentKind.GIF -> {
                require(contentKind == AttachmentKind.IMAGE || contentKind == AttachmentKind.GIF) {
                    "محتوای تصویر معتبر نیست"
                }
                if (contentKind == AttachmentKind.GIF) AttachmentKind.GIF else AttachmentKind.IMAGE
            }
            AttachmentKind.VIDEO -> {
                require(contentKind == AttachmentKind.VIDEO) { "محتوای ویدیو معتبر نیست" }
                AttachmentKind.VIDEO
            }
            AttachmentKind.AUDIO -> {
                require(contentKind == AttachmentKind.AUDIO) { "محتوای فایل صوتی معتبر نیست" }
                AttachmentKind.AUDIO
            }
            AttachmentKind.DOCUMENT -> {
                require(contentKind == AttachmentKind.DOCUMENT) { "فقط فایل PDF معتبر قابل ارسال است" }
                AttachmentKind.DOCUMENT
            }
            AttachmentKind.UNKNOWN -> error("نوع فایل پشتیبانی نمی‌شود")
        }

        val canonicalMime = mimeTypeFor(extension, canonicalKind)
        val normalizedMime = declaredMimeType.trim().lowercase(Locale.US)
        require(mimeMatchesKind(normalizedMime, canonicalKind)) { "نوع اعلام‌شده فایل با محتوای آن هم‌خوان نیست" }
        return ValidatedChatAttachment(canonicalKind, extension, canonicalMime, maxBytes)
    }

    private fun detectKind(extension: String, header: ByteArray): AttachmentKind = when {
        extension in IMAGE_EXTENSIONS && isImage(header, extension) ->
            if (extension == "gif") AttachmentKind.GIF else AttachmentKind.IMAGE
        extension in VIDEO_EXTENSIONS && isVideo(header) -> AttachmentKind.VIDEO
        extension == "pdf" && header.startsWith(PDF) -> AttachmentKind.DOCUMENT
        extension in AUDIO_EXTENSIONS && isAudio(header, extension) -> AttachmentKind.AUDIO
        else -> AttachmentKind.UNKNOWN
    }

    private fun inferExtension(header: ByteArray): String = when {
        header.startsWith(PDF) -> "pdf"
        header.startsWith(JPEG) -> "jpg"
        header.startsWith(PNG) -> "png"
        header.startsWith(GIF) -> "gif"
        header.isRiff("WEBP") -> "webp"
        header.isRiff("WAVE") -> "wav"
        header.startsWith(OGG) -> "ogg"
        header.startsWith(FLAC) -> "flac"
        header.startsWith(ID3) -> "mp3"
        header.isIsoBaseMedia() -> "m4a"
        else -> ""
    }

    private fun isImage(header: ByteArray, extension: String): Boolean = when (extension) {
        "jpg", "jpeg" -> header.startsWith(JPEG)
        "png" -> header.startsWith(PNG)
        "gif" -> header.startsWith(GIF)
        "webp" -> header.isRiff("WEBP")
        "bmp" -> header.startsWith(BMP)
        "heic", "heif" -> header.isIsoBaseMedia(HEIF_BRANDS)
        else -> false
    }

    private fun isVideo(header: ByteArray): Boolean = header.isIsoBaseMedia(VIDEO_BRANDS) ||
        header.startsWith(EBML)

    private fun isAudio(header: ByteArray, extension: String): Boolean = when (extension) {
        "mp3" -> header.startsWith(ID3) || header.hasMpegFrameSync()
        "wav" -> header.isRiff("WAVE")
        "ogg" -> header.startsWith(OGG)
        "flac" -> header.startsWith(FLAC)
        "m4a" -> header.isIsoBaseMedia()
        "aac" -> header.hasAacFrameSync() || header.isIsoBaseMedia()
        else -> false
    }

    private fun mimeMatchesKind(mime: String, kind: AttachmentKind): Boolean {
        if (mime.isBlank() || mime == "application/octet-stream") return true
        return when (kind) {
            AttachmentKind.IMAGE, AttachmentKind.GIF -> mime.startsWith("image/")
            AttachmentKind.VIDEO -> mime.startsWith("video/")
            AttachmentKind.VOICE, AttachmentKind.AUDIO -> mime.startsWith("audio/")
            AttachmentKind.DOCUMENT -> mime == "application/pdf"
            AttachmentKind.UNKNOWN -> false
        }
    }

    private fun mimeTypeFor(extension: String, kind: AttachmentKind): String = when (extension) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        "heic" -> "image/heic"
        "heif" -> "image/heif"
        "mp4" -> "video/mp4"
        "mov" -> "video/quicktime"
        "webm", "mkv" -> "video/webm"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        "pdf" -> "application/pdf"
        else -> when (kind) {
            AttachmentKind.IMAGE, AttachmentKind.GIF -> "image/*"
            AttachmentKind.VIDEO -> "video/*"
            AttachmentKind.VOICE, AttachmentKind.AUDIO -> "audio/*"
            AttachmentKind.DOCUMENT, AttachmentKind.UNKNOWN -> "application/octet-stream"
        }
    }

    internal companion object {
        const val NORMAL_MAX_BYTES = 15L * 1024L * 1024L
        const val PREMIUM_MAX_BYTES = 100L * 1024L * 1024L
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
        private val VIDEO_EXTENSIONS = setOf("mp4", "mov", "webm", "mkv")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac")
        private val BLUE_BADGES = setOf("bluetick", "blue", "bluebadge")
        private val PREMIUM_BADGES = setOf("goldtick", "gold", "goldbadge", "blacktick", "black", "blackbadge")
        private val HEIF_BRANDS = setOf("heic", "heix", "hevc", "heif", "mif1")
        private val VIDEO_BRANDS = setOf("isom", "iso2", "mp41", "mp42", "avc1", "qt  ", "mmp4")
        private val PDF = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        private val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        private val GIF = byteArrayOf(0x47, 0x49, 0x46)
        private val BMP = byteArrayOf(0x42, 0x4D)
        private val OGG = byteArrayOf(0x4F, 0x67, 0x67, 0x53)
        private val FLAC = byteArrayOf(0x66, 0x4C, 0x61, 0x43)
        private val ID3 = byteArrayOf(0x49, 0x44, 0x33)
        private val EBML = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte())
    }
}

private fun String?.normalizedToken(): String = orEmpty().trim().lowercase(Locale.US)
    .filter(Char::isLetterOrDigit)

private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

private fun ByteArray.isRiff(format: String): Boolean = size >= 12 &&
    copyOfRange(0, 4).decodeToString() == "RIFF" && copyOfRange(8, 12).decodeToString() == format

private fun ByteArray.isIsoBaseMedia(allowedBrands: Set<String>? = null): Boolean {
    if (size < 12 || copyOfRange(4, 8).decodeToString() != "ftyp") return false
    val brand = copyOfRange(8, 12).decodeToString().lowercase(Locale.US)
    return allowedBrands == null || brand in allowedBrands
}

private fun ByteArray.hasMpegFrameSync(): Boolean = size >= 2 &&
    this[0].toInt() and 0xFF == 0xFF && this[1].toInt() and 0xE0 == 0xE0

private fun ByteArray.hasAacFrameSync(): Boolean = size >= 2 &&
    this[0].toInt() and 0xFF == 0xFF && this[1].toInt() and 0xF0 == 0xF0
