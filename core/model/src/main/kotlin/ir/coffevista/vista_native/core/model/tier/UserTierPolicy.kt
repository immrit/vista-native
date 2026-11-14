package ir.coffevista.vista_native.core.model.tier

import java.util.Locale

enum class UserTier {
    NORMAL,
    BLACK_TICK,
    GOLD_TICK,
    BLUE_TICK,
}

data class UserTierCapabilities(
    val tier: UserTier,
    val maxPostChars: Int,
    val canEditPost: Boolean,
    val canEditComment: Boolean,
    val maxStoryHours: Int,
    val maxUploadMb: Int,
    val maxUploadBytes: Long,
    val canPostMusic: Boolean,
    val maxVideoDurationMs: Long,
    val canHideEngagementCounts: Boolean,
    val searchPriority: Int,
    val adsFree: Boolean,
    val maxGalleryImages: Int,
) {
    val isTickOrPremium: Boolean
        get() = tier != UserTier.NORMAL
}

object UserTierPolicy {
    private const val MB = 1024L * 1024L

    private val BLUE_TOKENS = setOf("blue", "bluetick", "bluebadge", "official", "identity")
    private val BLACK_TOKENS = setOf("black", "blacktick", "blackbadge", "dark", "creator", "founder")
    private val GOLD_TOKENS = setOf("gold", "goldtick", "goldbadge", "premium", "vip")

    fun normalizeToken(raw: String?): String =
        raw.orEmpty().trim().lowercase(Locale.US).filter(Char::isLetterOrDigit)

    fun resolve(
        role: String?,
        verificationType: String?,
        isVerified: Boolean = true,
        subscriptionPlan: String? = null,
        premiumDaysRemaining: Int? = null,
    ): UserTier {
        val roleToken = normalizeToken(role)
        val badgeToken = normalizeToken(verificationType)

        if (roleToken == "admin" || roleToken == "moderator" || (isVerified && badgeToken in BLUE_TOKENS)) {
            return UserTier.BLUE_TICK
        }
        if (isVerified && badgeToken in BLACK_TOKENS) {
            return UserTier.BLACK_TICK
        }
        val subExpired = premiumDaysRemaining != null && premiumDaysRemaining <= 0
        val hasActiveSub = !subExpired && (
            (premiumDaysRemaining != null && premiumDaysRemaining > 0) ||
                !subscriptionPlan.isNullOrBlank() ||
                roleToken == "premium"
            )
        if ((isVerified && badgeToken in GOLD_TOKENS && !subExpired) || hasActiveSub) {
            return UserTier.GOLD_TICK
        }
        return UserTier.NORMAL
    }

    fun capabilitiesFor(tier: UserTier): UserTierCapabilities = when (tier) {
        UserTier.BLUE_TICK -> UserTierCapabilities(
            tier = UserTier.BLUE_TICK,
            maxPostChars = 1500,
            canEditPost = true,
            canEditComment = true,
            maxStoryHours = 48,
            maxUploadMb = 100,
            maxUploadBytes = 100L * MB,
            canPostMusic = true,
            maxVideoDurationMs = 300_000L,
            canHideEngagementCounts = true,
            searchPriority = 1,
            adsFree = true,
            maxGalleryImages = 10,
        )
        UserTier.BLACK_TICK -> UserTierCapabilities(
            tier = UserTier.BLACK_TICK,
            maxPostChars = 500,
            canEditPost = true,
            canEditComment = true,
            maxStoryHours = 48,
            maxUploadMb = 50,
            maxUploadBytes = 50L * MB,
            canPostMusic = true,
            maxVideoDurationMs = 120_000L,
            canHideEngagementCounts = true,
            searchPriority = 2,
            adsFree = true,
            maxGalleryImages = 10,
        )
        UserTier.GOLD_TICK -> UserTierCapabilities(
            tier = UserTier.GOLD_TICK,
            maxPostChars = 1000,
            canEditPost = true,
            canEditComment = true,
            maxStoryHours = 48,
            maxUploadMb = 50,
            maxUploadBytes = 50L * MB,
            canPostMusic = true,
            maxVideoDurationMs = 120_000L,
            canHideEngagementCounts = true,
            searchPriority = 3,
            adsFree = true,
            maxGalleryImages = 10,
        )
        UserTier.NORMAL -> UserTierCapabilities(
            tier = UserTier.NORMAL,
            maxPostChars = 300,
            canEditPost = false,
            canEditComment = false,
            maxStoryHours = 24,
            maxUploadMb = 10,
            maxUploadBytes = 10L * MB,
            canPostMusic = false,
            maxVideoDurationMs = 60_000L,
            canHideEngagementCounts = false,
            searchPriority = 4,
            adsFree = false,
            maxGalleryImages = 3,
        )
    }

    fun capabilities(
        role: String?,
        verificationType: String?,
        isVerified: Boolean = true,
        subscriptionPlan: String? = null,
        premiumDaysRemaining: Int? = null,
    ): UserTierCapabilities = capabilitiesFor(
        resolve(
            role = role,
            verificationType = verificationType,
            isVerified = isVerified,
            subscriptionPlan = subscriptionPlan,
            premiumDaysRemaining = premiumDaysRemaining,
        )
    )
}
