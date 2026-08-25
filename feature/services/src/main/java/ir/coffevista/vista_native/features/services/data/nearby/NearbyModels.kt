package ir.coffevista.vista_native.features.services.data.nearby

data class NearbyCandidate(
    val userId: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val bio: String,
    val gender: String,
    val maritalStatus: String,
    val age: Int,
    val locationText: String,
    val isVerified: Boolean,
    val verificationType: String,
    val distanceKm: Double,
    val lastSeenAt: String? = null,
    val zone: String? = null,
    val cityName: String? = null,
    val provinceName: String? = null,
    val isOnlineNow: Boolean = false,
) {
    val zoneType: String
        get() {
            if (!zone.isNullOrBlank()) return zone
            return when {
                distanceKm < 30 -> "same_city"
                distanceKm < 200 -> "same_province"
                else -> "other_province"
            }
        }

    val cityLabel: String
        get() {
            if (!cityName.isNullOrBlank()) return cityName
            if (!provinceName.isNullOrBlank()) return provinceName
            return ""
        }

    val presenceLabel: String
        get() {
            if (isOnlineNow) return "آنلاین"
            if (lastSeenAt.isNullOrBlank()) return "اخیراً آنلاین"
            return "اخیراً آنلاین"
        }

    val isRecentlyOnline: Boolean
        get() = isOnlineNow || lastSeenAt == null

    val distanceLabel: String
        get() {
            if (distanceKm <= 0) return ""
            return if (distanceKm < 1) {
                "${(distanceKm * 1000).toInt()} متر"
            } else if (distanceKm < 10) {
                String.format(java.util.Locale.US, "%.1f کیلومتر", distanceKm)
            } else {
                "${distanceKm.toInt()} کیلومتر"
            }
        }

    val locationLine: String
        get() {
            val city = cityLabel
            val d = distanceLabel
            return when {
                city.isEmpty() -> d
                d.isEmpty() -> city
                else -> "$city • $d"
            }
        }
}

data class NearbyMatch(
    val matchId: String,
    val userId: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val isVerified: Boolean,
    val verificationType: String,
    val matchedAt: String,
)

data class NearbyPreferences(
    val interestedIn: String = "all", // male | female | all
    val minAge: Int = 18,
    val maxAge: Int = 60,
    val maxDistanceKm: Int = 50,
    val maritalPref: String = "all", // all | single | married
    val isEnabled: Boolean = false,
    val hasLocation: Boolean = false,
)

data class NearbyReceivedLike(
    val userId: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val isVerified: Boolean,
    val verificationType: String,
    val action: String = "like", // like | superlike
)

data class NearbyReceivedLikes(
    val count: Int,
    val likes: List<NearbyReceivedLike>,
)

data class NearbyLikeResult(
    val matched: Boolean,
    val matchId: String,
    val match: NearbyMatch? = null,
)
