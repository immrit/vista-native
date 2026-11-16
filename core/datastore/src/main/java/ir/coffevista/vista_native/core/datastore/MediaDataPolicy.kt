package ir.coffevista.vista_native.core.datastore

import ir.coffevista.vista_native.core.common.ConnectionTransport

data class UploadCompressionPreset(
    val maxDimension: Int,
    val jpegQuality: Int,
)

enum class MediaRetentionPeriod(val key: String, val durationMillis: Long?) {
    THREE_DAYS("3_days", 3L * 24L * 60L * 60L * 1_000L),
    ONE_WEEK("1_week", 7L * 24L * 60L * 60L * 1_000L),
    ONE_MONTH("1_month", 30L * 24L * 60L * 60L * 1_000L),
    FOREVER("forever", null),
    ;

    companion object {
        fun fromKey(key: String): MediaRetentionPeriod =
            entries.firstOrNull { it.key == key } ?: FOREVER
    }
}

object MediaDataPolicy {
    const val CELLULAR_AUTO_DOWNLOAD_NON_IMAGE_MAX_BYTES = 2L * 1024L * 1024L
    const val DATA_SAVER_MAX_VIDEO_BITRATE = 700_000

    fun uploadPreset(mediaQuality: String): UploadCompressionPreset = when (mediaQuality) {
        "data_saver" -> UploadCompressionPreset(maxDimension = 800, jpegQuality = 60)
        "standard" -> UploadCompressionPreset(maxDimension = 1280, jpegQuality = 78)
        else -> UploadCompressionPreset(maxDimension = 2048, jpegQuality = 90)
    }

    fun keepMediaDurationMillis(period: String): Long? =
        MediaRetentionPeriod.fromKey(period).durationMillis

    /**
     * Evaluates whether a chat attachment should be auto-downloaded/loaded under the
     * user's Wi-Fi / Cellular / Roaming preferences.
     */
    fun shouldAutoDownloadAttachment(
        transport: ConnectionTransport,
        isImageOrGifOrVoice: Boolean,
        sizeBytes: Long?,
        prefs: SettingsPreferences,
    ): Boolean = when (transport) {
        ConnectionTransport.OFFLINE -> false
        ConnectionTransport.WIFI -> prefs.autoDownloadWifi
        ConnectionTransport.ROAMING -> prefs.autoDownloadRoaming && isImageOrGifOrVoice
        ConnectionTransport.CELLULAR -> {
            prefs.autoDownloadCellular && (
                isImageOrGifOrVoice ||
                    (sizeBytes != null && sizeBytes in 1L..CELLULAR_AUTO_DOWNLOAD_NON_IMAGE_MAX_BYTES)
                )
        }
    }
}
