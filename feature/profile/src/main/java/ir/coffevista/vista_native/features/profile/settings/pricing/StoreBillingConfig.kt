package ir.coffevista.vista_native.features.profile.settings.pricing

enum class StoreChannel(val wireName: String) {
    Bazaar("bazaar"),
    Myket("myket"),
    Direct("direct"),
    ;

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
