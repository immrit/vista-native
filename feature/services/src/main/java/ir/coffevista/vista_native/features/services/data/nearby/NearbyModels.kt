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
    val statusText: String = "",
    val hideDistance: Boolean = false,
    val hasPendingChatRequest: Boolean = false,
    val compatibilityScore: Int = 0,
) {
    val zoneType: String
        get() {
            if (!zone.isNullOrBlank()) return zone
            if (hideDistance || distanceKm <= 0.0) return ""
            return when {
                distanceKm < 25 -> "same_city"
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
        get() = if (isOnlineNow) "آنلاین" else "اخیراً آنلاین"

    val isRecentlyOnline: Boolean
        get() = isOnlineNow || lastSeenAt == null

    val distanceLabel: String
        get() {
            if (hideDistance) return "فاصله مخفی"
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
            if (hideDistance) {
                return if (city.isEmpty()) "فاصله مخفی" else city
            }
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
    val isOnlineNow: Boolean = false,
    val cityName: String = "",
    val provinceName: String = "",
    val hideDistance: Boolean = false,
)

data class NearbyPreferences(
    val interestedIn: String = "all", // male | female | all
    val minAge: Int = 18,
    val maxAge: Int = 60,
    val maxDistanceKm: Int = 50,
    val maritalPref: String = "all", // all | single | married
    val statusText: String = "",
    val hideDistance: Boolean = false,
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
    val isOnlineNow: Boolean = false,
    val cityName: String = "",
    val hideDistance: Boolean = false,
)

data class NearbyReceivedLikes(
    val count: Int,
    val likes: List<NearbyReceivedLike>,
)

data class NearbyLikeResult(
    val matched: Boolean,
    val matchId: String,
    val match: NearbyMatch? = null,
    val dailyLikesRemaining: Int = 0,
    val coinsBalance: Long = 0,
)

data class NearbyWallet(
    val coins: Long = 0,
    val chatRequestCredits: Int = 0,
    val freeDailyRequestsRemaining: Int = 0,
    val dailyLikesRemaining: Int = 0,
    val chatRequestCoinCost: Int = 20,
    val superlikeCoinCost: Int = 10,
) {
    val totalAvailableRequests: Int
        get() = chatRequestCredits + freeDailyRequestsRemaining
}

data class NearbyChatRequestPack(
    val id: String,
    val title: String,
    val requestsCount: Int,
    val coinCost: Int,
    val bonusLabel: String = "",
    val badge: String = "",
    val isPopular: Boolean = false,
)

data class NearbyShopCatalog(
    val wallet: NearbyWallet = NearbyWallet(),
    val packs: List<NearbyChatRequestPack> = emptyList(),
    val coinsPerRequest: Int = 20,
    val sampleRateLabel: String = "هر ۱,۰۰۰ سکه = ۵۰ درخواست چت",
)

data class NearbyBuyRequestsResult(
    val pack: NearbyChatRequestPack,
    val wallet: NearbyWallet,
)

data class NearbyChatRequest(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val peerId: String,
    val direction: String, // incoming | outgoing
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val isVerified: Boolean,
    val isOnlineNow: Boolean,
    val age: Int,
    val bio: String,
    val statusText: String,
    val cityName: String,
    val provinceName: String,
    val distanceKm: Double,
    val hideDistance: Boolean,
    val introMessage: String,
    val status: String, // pending | accepted | rejected | cancelled
    val conversationId: String,
    val createdAt: String,
    val respondedAt: String? = null,
)

data class NearbySendChatRequestResult(
    val request: NearbyChatRequest,
    val wallet: NearbyWallet,
    val spentKind: String,
    val coinsSpent: Int,
    val creditsSpent: Int,
)

data class NearbyRespondChatRequestResult(
    val status: String,
    val conversationId: String,
)

data class NearbyCoinPackage(
    val id: String,
    val coins: Long,
    val bonus: Long,
    val priceToman: Int,
    val amountRial: Int,
) {
    val totalCoins: Long get() = coins + bonus
}

data class NearbyBazaarPendingVerification(
    val productId: String,
    val purchaseToken: String,
    val packageName: String,
)

data class NearbyBazaarVerifyResult(
    val success: Boolean,
    val message: String,
    val type: String,
    val coinsAdded: Long,
    val newBalance: Long?,
    val alreadyProcessed: Boolean,
)
