package ir.coffevista.vista_native.core.common

enum class StoreChannel(val wireName: String) {
    Bazaar("bazaar"),
    Myket("myket"),
    Direct("direct"),
    ;

    val displayNameFa: String
        get() = when (this) {
            Myket -> "مایکت"
            Direct -> "وب‌سایت ویستا"
            Bazaar -> "کافه‌بازار"
        }

    companion object {
        fun fromWire(value: String?): StoreChannel = when (value?.trim()?.lowercase()) {
            "myket" -> Myket
            "direct" -> Direct
            else -> Bazaar
        }
    }
}

data class StoreBillingConfig(
    val channel: StoreChannel = StoreChannel.Bazaar,
    val myketPublicKey: String = "",
)
