package ir.coffevista.vista_native.core.common

fun interface DeviceIdProvider {
    fun deviceId(): String?

    companion object {
        @Volatile
        var current: DeviceIdProvider? = null
    }
}
