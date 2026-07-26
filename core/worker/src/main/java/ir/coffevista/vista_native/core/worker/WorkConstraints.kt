package ir.coffevista.vista_native.core.worker

import androidx.work.Constraints
import androidx.work.NetworkType

data class WorkRequirements(
    val network: NetworkType = NetworkType.CONNECTED,
    val requiresCharging: Boolean = false,
    val requiresStorageNotLow: Boolean = true,
) {
    fun toConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(network)
        .setRequiresCharging(requiresCharging)
        .setRequiresStorageNotLow(requiresStorageNotLow)
        .build()
}
