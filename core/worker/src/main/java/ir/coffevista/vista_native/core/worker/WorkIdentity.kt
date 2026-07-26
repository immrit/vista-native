package ir.coffevista.vista_native.core.worker

import java.security.MessageDigest
import java.util.UUID

data class WorkIdentity(
    val accountId: String,
    val operation: String,
    val operationId: UUID,
) {
    init {
        require(accountId.isNotBlank()) { "Account id must not be blank" }
        require(OPERATION_PATTERN.matches(operation)) { "Invalid operation name" }
    }

    val accountTag: String
        get() = accountTag(accountId)

    val operationTag: String
        get() = "$PREFIX:operation:$operation"

    val uniqueName: String
        get() = "$PREFIX:$operation:${accountHash(accountId)}:$operationId"

    companion object {
        fun accountTag(accountId: String): String {
            require(accountId.isNotBlank()) { "Account id must not be blank" }
            return "$PREFIX:account:${accountHash(accountId)}"
        }

        private fun accountHash(accountId: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(accountId.toByteArray(Charsets.UTF_8))
                .take(12)
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

        private val OPERATION_PATTERN = Regex("[a-z][a-z0-9_]{2,31}")
        private const val PREFIX = "vista-work-v1"
    }
}
