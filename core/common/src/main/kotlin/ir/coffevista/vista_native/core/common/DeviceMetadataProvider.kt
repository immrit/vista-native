package ir.coffevista.vista_native.core.common

data class DeviceMetadata(
    val deviceName: String,
    val deviceModel: String,
    val osVersion: String,
    val appVersion: String,
    val appVersionCode: Long = 0L,
    val userAgent: String,
)

fun interface DeviceMetadataProvider {
    fun metadata(): DeviceMetadata

    companion object {
        @Volatile
        var current: DeviceMetadataProvider? = null

        fun formatAndroidDeviceName(
            manufacturer: String?,
            model: String?,
            brand: String? = null,
        ): String {
            val cleanModel = model?.trim().orEmpty()
            val cleanManuf = manufacturer?.trim().orEmpty()
                .ifBlank { brand?.trim().orEmpty() }
            if (cleanModel.isBlank()) {
                return if (cleanManuf.isNotBlank()) formatBrandName(cleanManuf) else "Android Device"
            }
            if (cleanManuf.isBlank()) return cleanModel
            val prettyManuf = formatBrandName(cleanManuf)
            return if (cleanModel.startsWith(prettyManuf, ignoreCase = true) ||
                cleanModel.startsWith(cleanManuf, ignoreCase = true)
            ) {
                cleanModel
            } else {
                "$prettyManuf $cleanModel"
            }
        }

        private fun formatBrandName(raw: String): String = when (raw.lowercase()) {
            "samsung" -> "Samsung"
            "xiaomi" -> "Xiaomi"
            "redmi" -> "Redmi"
            "poco" -> "POCO"
            "huawei" -> "Huawei"
            "honor" -> "Honor"
            "google" -> "Google"
            "oneplus" -> "OnePlus"
            "realme" -> "Realme"
            "oppo" -> "OPPO"
            "vivo" -> "vivo"
            "nokia" -> "Nokia"
            "sony" -> "Sony"
            "motorola" -> "Motorola"
            "asus" -> "ASUS"
            else -> raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
