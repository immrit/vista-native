package ir.coffevista.vista_native.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Lightweight in-memory snapshot of the user's most recently acquired coordinates and city.
 * Shared across feature modules (e.g., feed/stories location picker and services/nearby)
 * without triggering permission prompts.
 */
data class DeviceLocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val city: String? = null,
    val province: String? = null,
    val capturedAtMillis: Long = System.currentTimeMillis(),
) {
    fun isValid(): Boolean =
        latitude.isFinite() &&
            longitude.isFinite() &&
            (latitude != 0.0 || longitude != 0.0)
}

@Singleton
class DeviceLocationSnapshotCache @Inject constructor() {

    private val _snapshot = MutableStateFlow<DeviceLocationSnapshot?>(null)
    val snapshot: StateFlow<DeviceLocationSnapshot?> = _snapshot.asStateFlow()

    fun latest(maxAgeMs: Long = DEFAULT_MAX_AGE_MS): DeviceLocationSnapshot? {
        val current = _snapshot.value ?: return null
        if (!current.isValid()) return null
        return if (System.currentTimeMillis() - current.capturedAtMillis <= maxAgeMs) {
            current
        } else {
            null
        }
    }

    fun latestAny(): DeviceLocationSnapshot? =
        _snapshot.value?.takeIf(DeviceLocationSnapshot::isValid)

    fun updateCoordinates(
        latitude: Double,
        longitude: Double,
        capturedAtMillis: Long = System.currentTimeMillis(),
    ) {
        if (!latitude.isFinite() || !longitude.isFinite() || (latitude == 0.0 && longitude == 0.0)) {
            return
        }
        val previous = _snapshot.value
        val sameArea = previous != null &&
            abs(previous.latitude - latitude) < SAME_AREA_DELTA_DEG &&
            abs(previous.longitude - longitude) < SAME_AREA_DELTA_DEG
        _snapshot.value = DeviceLocationSnapshot(
            latitude = latitude,
            longitude = longitude,
            city = if (sameArea) previous?.city else null,
            province = if (sameArea) previous?.province else null,
            capturedAtMillis = capturedAtMillis,
        )
    }

    fun updateAddress(
        latitude: Double,
        longitude: Double,
        city: String?,
        province: String?,
        capturedAtMillis: Long = System.currentTimeMillis(),
    ) {
        if (!latitude.isFinite() || !longitude.isFinite() || (latitude == 0.0 && longitude == 0.0)) {
            return
        }
        val cleanCity = city?.trim()?.takeIf(String::isNotBlank)
        val cleanProvince = province?.trim()?.takeIf(String::isNotBlank)
        val previous = _snapshot.value
        val sameArea = previous != null &&
            abs(previous.latitude - latitude) < SAME_AREA_DELTA_DEG &&
            abs(previous.longitude - longitude) < SAME_AREA_DELTA_DEG
        _snapshot.value = DeviceLocationSnapshot(
            latitude = latitude,
            longitude = longitude,
            city = cleanCity ?: if (sameArea) previous?.city else null,
            province = cleanProvince ?: if (sameArea) previous?.province else null,
            capturedAtMillis = capturedAtMillis,
        )
    }

    fun clear() {
        _snapshot.value = null
    }

    companion object {
        const val DEFAULT_MAX_AGE_MS = 20 * 60_000L
        private const val SAME_AREA_DELTA_DEG = 0.005 // ~500m
    }
}
