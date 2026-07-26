package ir.coffevista.vista_native.core.model.session

data class AuthenticatedContext(
    val userId: String,
    val profileCompleted: Boolean,
    val passwordRequired: Boolean,
    val offline: Boolean,
    val displayName: String = "کاربر ویستا",
)
