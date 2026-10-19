package ir.coffevista.vista_native.core.common

fun interface TokenRefresher {
    fun refreshToken(): String?
}
