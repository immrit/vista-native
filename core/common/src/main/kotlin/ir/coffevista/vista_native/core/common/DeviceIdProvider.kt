package ir.coffevista.vista_native.core.common

fun interface DeviceIdProvider {
    fun deviceId(): String?

    companion object {
        const val PREFERENCES_NAME = "vista_chat_runtime"
        const val KEY_DEVICE_ID = "device_id"

        @Volatile
        var current: DeviceIdProvider? = null
    }
}
