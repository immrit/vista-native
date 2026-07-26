package ir.coffevista.vista_native.core.network

enum class EnvironmentName {
    BETA,
    PRODUCTION,
}

data class AppEnvironment(
    val name: EnvironmentName,
    val internalBaseUrl: String,
    val debugBuild: Boolean,
) {
    init {
        require(internalBaseUrl.startsWith("https://")) {
            "Internal API base URL must use HTTPS"
        }
        require(!internalBaseUrl.contains('@')) {
            "Internal API base URL must not contain credentials"
        }
    }
}
