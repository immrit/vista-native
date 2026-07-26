package ir.coffevista.vista_native.core.common

data class FoundationSignal(
    val name: String,
    val outcome: String,
    val attributes: Map<String, String> = emptyMap(),
)

fun interface FoundationTelemetry {
    fun record(signal: FoundationSignal)
}

object NoOpFoundationTelemetry : FoundationTelemetry {
    override fun record(signal: FoundationSignal) = Unit
}
