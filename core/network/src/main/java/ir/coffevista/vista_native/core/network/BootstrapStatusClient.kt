package ir.coffevista.vista_native.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class SystemAlert(
    val id: String,
    val severity: String,
    val scope: String,
    val feature: String? = null,
    val message: String,
)

data class RemoteTierLimits(
    val maxPostChars: Int? = null,
    val canEditPost: Boolean? = null,
    val canEditComment: Boolean? = null,
    val maxStoryHours: Int? = null,
    val maxUploadMb: Int? = null,
    val canPostMusic: Boolean? = null,
    val maxVideoSeconds: Int? = null,
    val canHideEngagementCounts: Boolean? = null,
    val searchPriority: Int? = null,
    val adsFree: Boolean? = null,
    val maxGalleryImages: Int? = null,
)

data class RemoteAnnouncement(
    val enabled: Boolean = false,
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val ctaText: String = "",
    val ctaUrl: String = "",
    val severity: String = "info",
    val dismissible: Boolean = true,
)

data class RemoteBannerSlot(
    val enabled: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val linkUrl: String = "",
    val bgColor: String = "#0097C7",
    val textColor: String = "#FFFFFF",
)

data class RemoteThemeOverride(
    val enabled: Boolean = false,
    val holidayName: String = "",
    val primaryHex: String = "#0097C7",
    val accentHex: String = "#F0543B",
    val badgeText: String = "",
)

data class RemoteAdsConfig(
    val enabled: Boolean = true,
    val feedInterval: Int = 8,
    val storiesEnabled: Boolean = true,
    val bannerEnabled: Boolean = true,
)

data class RemoteQuickServiceItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val route: String,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val gradientStart: String = "#0097C7",
    val gradientEnd: String = "#1FB6E8",
    val icon: String = "",
    val badgeText: String = "",
    val featured: Boolean = false,
)

data class RemoteServicesHubConfig(
    val headerTitle: String = "سرویس‌ها",
    val headerSubtitle: String = "ابزارها، سرگرمی و جمع‌های ویستا",
    val announcementText: String = "",
    val bannersTitle: String = "برای شما",
    val bannersSubtitle: String = "انتخاب‌های تازه ویستا",
    val quickTitle: String = "دسترسی سریع",
    val quickSubtitle: String = "مسیرهای پرکاربرد",
    val contactsTitle: String = "آشناها در ویستا",
    val sectionsTitle: String = "سرویس‌های بیشتر",
    val sectionsSubtitle: String = "انتخاب‌هایی که از ویستا برایت آماده شده",
    val quickLayout: String = "bento",
    val bannersLayout: String = "carousel",
    val bannerAutoPlay: Boolean = true,
    val sectionsLayout: String = "rail",
    val showBanners: Boolean = true,
    val showQuickServices: Boolean = true,
    val showContacts: Boolean = true,
    val showSections: Boolean = true,
    val sectionOrder: List<String> = listOf("banners", "quick_services", "contacts", "sections"),
)

data class RemoteBrandOrnament(
    val enabled: Boolean = false,
    val preset: String = "none",
    val customImageUrl: String = "",
    val customLogoLightUrl: String = "",
    val customLogoDarkUrl: String = "",
    val offsetX: Int = 0,
    val offsetY: Int = -10,
    val scale: Float = 1.0f,
    val rotationDeg: Int = 0,
    val tapAction: String = "none",
    val tapTitle: String = "",
    val tapMessage: String = "",
    val tapUrl: String = "",
)

data class RemoteAmbientEffect(
    val effect: String = "none",
    val density: String = "low",
)

data class RemoteNavigationSlot(
    val key: String,
    val label: String = "",
    val visible: Boolean = true,
    val sortOrder: Int = 0,
    val badgeText: String = "",
    val icon: String = "",
    val route: String = "",
)

data class RemoteCopyOverrides(
    val feedForYouTab: String = "",
    val feedFollowingTab: String = "",
    val maintenanceTitle: String = "",
    val maintenanceMessage: String = "",
    val composerPlaceholder: String = "",
)

data class RemotePricingPlanMeta(
    val enabled: Boolean = true,
    val originalPriceToman: Long = 0L,
    val badgeText: String = "",
    val recommended: Boolean = false,
)

data class RemoteConfigSnapshot(
    val version: Long = 1L,
    val tiers: Map<String, RemoteTierLimits> = emptyMap(),
    val maxGroupMembers: Int = 20,
    val announcement: RemoteAnnouncement = RemoteAnnouncement(),
    val feedBanner: RemoteBannerSlot = RemoteBannerSlot(),
    val exploreBanner: RemoteBannerSlot = RemoteBannerSlot(),
    val themeOverride: RemoteThemeOverride = RemoteThemeOverride(),
    val ads: RemoteAdsConfig = RemoteAdsConfig(),
    val quickServices: List<RemoteQuickServiceItem> = emptyList(),
    val servicesHub: RemoteServicesHubConfig = RemoteServicesHubConfig(),
    val brandOrnament: RemoteBrandOrnament = RemoteBrandOrnament(),
    val ambientEffect: RemoteAmbientEffect = RemoteAmbientEffect(),
    val navigationSlots: List<RemoteNavigationSlot> = emptyList(),
    val copyOverrides: RemoteCopyOverrides = RemoteCopyOverrides(),
    val paymentGateway: String = "store",
    val pricingMeta: Map<String, RemotePricingPlanMeta> = emptyMap(),
    val premiumFeatures: List<String> = emptyList(),
    val rollouts: Map<String, Int> = emptyMap(),
)

object SystemGateMonitor {
    private val _maintenanceActive = MutableStateFlow(false)
    val maintenanceActive: StateFlow<Boolean> = _maintenanceActive.asStateFlow()

    private val _bannedReasonFa = MutableStateFlow<String?>(null)
    val bannedReasonFa: StateFlow<String?> = _bannedReasonFa.asStateFlow()

    private val _isBanned = MutableStateFlow(false)
    val isBanned: StateFlow<Boolean> = _isBanned.asStateFlow()

    private val _sessionTerminated = MutableStateFlow(false)
    val sessionTerminated: StateFlow<Boolean> = _sessionTerminated.asStateFlow()

    private val _featureFlags = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val featureFlags: StateFlow<Map<String, Boolean>> = _featureFlags.asStateFlow()

    private val _remoteConfig = MutableStateFlow<RemoteConfigSnapshot?>(null)
    val remoteConfig: StateFlow<RemoteConfigSnapshot?> = _remoteConfig.asStateFlow()

    fun isFeatureEnabled(featureKey: String): Boolean {
        val normalized = featureKey.trim().lowercase()
        val flags = _featureFlags.value
        return flags[normalized] ?: flags["${normalized}_enabled"] ?: true
    }

    fun updateBootstrapStatus(status: BootstrapStatus) {
        _maintenanceActive.value = status.maintenance
        _featureFlags.value = status.featureFlags
        if (status.remoteConfig != null) {
            _remoteConfig.value = status.remoteConfig
        }
    }

    fun reportMaintenance(active: Boolean) {
        _maintenanceActive.value = active
    }

    fun reportSessionTerminated(terminated: Boolean = true) {
        _sessionTerminated.value = terminated
    }

    fun reportBanned(reasonFa: String? = null) {
        _bannedReasonFa.value = reasonFa ?: "دستگاه یا حساب کاربری شما به دلیل نقض قوانین مسدود شده است."
        _isBanned.value = true
    }

    fun clearBanned() {
        _bannedReasonFa.value = null
        _isBanned.value = false
    }
}

data class BootstrapStatus(
    val maintenance: Boolean,
    val tlsUpdate: TlsPolicyUpdate?,
    val featureFlags: Map<String, Boolean> = emptyMap(),
    val killSwitches: Set<String> = emptySet(),
    val alerts: List<SystemAlert> = emptyList(),
    val fcmResyncEpoch: Long = 0L,
    val remoteConfig: RemoteConfigSnapshot? = null,
) {
    fun isFeatureEnabled(featureKey: String): Boolean {
        val normalized = featureKey.trim().lowercase()
        if (normalized in killSwitches) return false
        return featureFlags[normalized]
            ?: featureFlags["${normalized}_enabled"]
            ?: true
    }
}

@Singleton
class BootstrapStatusClient @Inject constructor(
    @BootstrapApi private val client: OkHttpClient,
    @InternalEnvironment private val environment: AppEnvironment,
    private val tlsPolicyStore: TlsPolicyStore,
) {
    private val statusUrl = environment.internalBaseUrl.trimEnd('/') + "/api/v1/system/status"
    private val json = Json { ignoreUnknownKeys = true }
    private val _statusState = MutableStateFlow<BootstrapStatus?>(null)
    val statusState: StateFlow<BootstrapStatus?> = _statusState.asStateFlow()

    suspend fun fetch(): BootstrapStatus {
        val request = Request.Builder().url(statusUrl).get().build()
        val response = client.newCall(request).await()
        response.use {
            if (!it.isSuccessful) {
                throw RemoteFailure(it.code, "bootstrap_status_failed", null)
            }
            val root = json.parseToJsonElement(it.body?.string().orEmpty()).jsonObject
            val data = root["data"] as? JsonObject ?: root
            val maintenance = data["maintenance"]?.jsonPrimitive?.booleanOrNull
                ?: data["maintenance_mode"]?.jsonPrimitive?.booleanOrNull
                ?: false
            val featureFlags = parseFeatureFlags(data)
            val killSwitches = parseKillSwitches(data, featureFlags)
            val alerts = parseAlerts(data)
            val fcmResyncEpoch = (data["fcm_resync_epoch"] as? JsonPrimitive)?.longOrNull ?: 0L

            val tls = data["tls_pinning"] as? JsonObject
            val update = tls?.let { raw ->
                observeBackendPolicy(
                    raw = raw,
                    tlsPolicyPublicKeyBase64 = environment.tlsPolicyPublicKeyBase64,
                )
            }
            val remoteConfig = (data["remote_config"] as? JsonObject)?.let(::parseRemoteConfigSnapshot)
            val status = BootstrapStatus(
                maintenance = maintenance,
                tlsUpdate = update,
                featureFlags = featureFlags,
                killSwitches = killSwitches,
                alerts = alerts,
                fcmResyncEpoch = fcmResyncEpoch,
                remoteConfig = remoteConfig,
            )
            _statusState.value = status
            SystemGateMonitor.updateBootstrapStatus(status)
            return status
        }
    }

    private fun observeBackendPolicy(
        raw: JsonObject,
        tlsPolicyPublicKeyBase64: String,
    ): TlsPolicyUpdate = verifyAndApplyTlsPolicy(
        raw = raw,
        tlsPolicyStore = tlsPolicyStore,
        tlsPolicyPublicKeyBase64 = tlsPolicyPublicKeyBase64,
    )

    private fun parseFeatureFlags(data: JsonObject): Map<String, Boolean> {
        val result = linkedMapOf<String, Boolean>()
        val sources = listOfNotNull(
            data["feature_flags"] as? JsonObject,
            data["features"] as? JsonObject,
        )
        for (source in sources) {
            for ((key, value) in source) {
                val boolVal = (value as? JsonPrimitive)?.booleanOrNull ?: continue
                result[key.trim().lowercase()] = boolVal
            }
        }
        return result
    }

    private fun parseKillSwitches(
        data: JsonObject,
        featureFlags: Map<String, Boolean>,
    ): Set<String> {
        val disabled = linkedSetOf<String>()
        (data["disabled"] as? JsonArray)?.forEach { elem ->
            val key = (elem as? JsonPrimitive)?.contentOrNull?.trim()?.lowercase()
            if (!key.isNullOrEmpty()) disabled += key
        }
        (data["kill_switches"] as? JsonObject)?.forEach { (key, value) ->
            if ((value as? JsonPrimitive)?.booleanOrNull == true) {
                disabled += key.trim().lowercase()
            }
        }
        featureFlags.forEach { (key, enabled) ->
            if (!enabled) disabled += key
        }
        return disabled
    }

    private fun parseAlerts(data: JsonObject): List<SystemAlert> {
        val arr = data["alerts"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull { elem ->
            val obj = elem as? JsonObject ?: return@mapNotNull null
            val id = (obj["id"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val message = (obj["message"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            SystemAlert(
                id = id,
                severity = (obj["severity"] as? JsonPrimitive)?.contentOrNull ?: "info",
                scope = (obj["scope"] as? JsonPrimitive)?.contentOrNull ?: "global",
                feature = (obj["feature"] as? JsonPrimitive)?.contentOrNull,
                message = message,
            )
        }
    }

    private fun parseIsoEpochSeconds(raw: String?): Long? {
        val trimmed = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return runCatching { Instant.parse(trimmed).epochSecond }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(trimmed).toEpochSecond() }.getOrNull()
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, exception: IOException) {
                if (continuation.isActive) continuation.resumeWithException(exception)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        })
    }

    private companion object {
        const val DEFAULT_PIN_TTL_SECONDS = 30L * 86_400L
    }
}

internal fun verifyAndApplyTlsPolicy(
    raw: JsonObject,
    tlsPolicyStore: TlsPolicyStore,
    tlsPolicyPublicKeyBase64: String,
): TlsPolicyUpdate {
    val rawMode = (raw["mode"] as? JsonPrimitive)?.contentOrNull?.lowercase()
    val mode = when (rawMode) {
        "monitor" -> TlsMode.MONITOR
        "enforce" -> TlsMode.ENFORCE
        else -> TlsMode.OFF
    }
    val fingerprints = (raw["fingerprints"] as? JsonArray)
        ?.mapNotNull { element -> (element as? JsonPrimitive)?.contentOrNull.normalizedPin() }
        .orEmpty()
    val rawExpiresAt = (raw["expires_at"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
    val expiresAtEpochSeconds = parsePolicyIsoEpochSeconds(rawExpiresAt)
        ?: if (mode != TlsMode.OFF) {
            (System.currentTimeMillis() / 1_000L) + 30L * 86_400L
        } else {
            null
        }
    val current = tlsPolicyStore.current()
    val maxAllowedRevision = maxOf(
        current.revision + 100L,
        (System.currentTimeMillis() / 1_000L) + 3_600L,
    )
    val explicitRevision = (raw["revision"] as? JsonPrimitive)?.longOrNull
        ?.takeIf { it in 1L..maxAllowedRevision }
    val resolvedRevision = explicitRevision ?: if (current.revision > 0L) current.revision else 1L
    val contractVersion = (raw["contract_version"] as? JsonPrimitive)?.longOrNull?.toInt()
        ?.takeIf { it > 0 }
        ?: TlsPolicyStore.SUPPORTED_CONTRACT_VERSION

    val candidate = TlsPolicy(
        contractVersion = contractVersion,
        revision = resolvedRevision,
        mode = mode,
        currentPinSha256 = fingerprints.getOrNull(0),
        nextPinSha256 = fingerprints.getOrNull(1),
        expiresAtEpochSeconds = expiresAtEpochSeconds,
    )

    val signatureB64 = (raw["signature"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
    val signatureVerified = verifyCanonicalTlsPolicySignature(
        publicKeyB64 = tlsPolicyPublicKeyBase64,
        signatureB64 = signatureB64,
        revision = resolvedRevision,
        mode = mode,
        fingerprints = fingerprints,
        rawExpiresAt = rawExpiresAt,
    ) && (mode != TlsMode.ENFORCE || fingerprints.isNotEmpty())

    return tlsPolicyStore.update(candidate, signatureVerified = signatureVerified)
}

internal fun verifyCanonicalTlsPolicySignature(
    publicKeyB64: String,
    signatureB64: String,
    revision: Long,
    mode: TlsMode,
    fingerprints: List<String>,
    rawExpiresAt: String,
): Boolean {
    val cleanPub = publicKeyB64.trim()
    val cleanSig = signatureB64.trim()
    if (cleanPub.isEmpty() || cleanSig.isEmpty()) return false
    val pubBytes = runCatching { java.util.Base64.getDecoder().decode(cleanPub) }.getOrNull() ?: return false
    val sigBytes = runCatching { java.util.Base64.getDecoder().decode(cleanSig) }.getOrNull() ?: return false
    val canonical = "v1|$revision|${mode.name.lowercase()}|${fingerprints.joinToString(",")}|${rawExpiresAt.trim()}"
    return Ed25519Verifier.verify(
        publicKey = pubBytes,
        message = canonical.toByteArray(Charsets.UTF_8),
        signature = sigBytes,
    )
}

private fun parsePolicyIsoEpochSeconds(raw: String?): Long? {
    val trimmed = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return runCatching { Instant.parse(trimmed).epochSecond }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(trimmed).toEpochSecond() }.getOrNull()
}

internal fun parseRemoteConfigSnapshot(obj: JsonObject): RemoteConfigSnapshot {
    val tiersObj = obj["tiers"] as? JsonObject
    val tiersMap = buildMap<String, RemoteTierLimits> {
        tiersObj?.forEach { (key, elem) ->
            val item = elem as? JsonObject ?: return@forEach
            put(
                key,
                RemoteTierLimits(
                    maxPostChars = (item["max_post_chars"] as? JsonPrimitive)?.intOrNull,
                    canEditPost = (item["can_edit_post"] as? JsonPrimitive)?.booleanOrNull,
                    canEditComment = (item["can_edit_comment"] as? JsonPrimitive)?.booleanOrNull,
                    maxStoryHours = (item["max_story_hours"] as? JsonPrimitive)?.intOrNull,
                    maxUploadMb = (item["max_upload_mb"] as? JsonPrimitive)?.intOrNull,
                    canPostMusic = (item["can_post_music"] as? JsonPrimitive)?.booleanOrNull,
                    maxVideoSeconds = (item["max_video_seconds"] as? JsonPrimitive)?.intOrNull,
                    canHideEngagementCounts = (item["can_hide_engagement_counts"] as? JsonPrimitive)?.booleanOrNull,
                    searchPriority = (item["search_priority"] as? JsonPrimitive)?.intOrNull,
                    adsFree = (item["ads_free"] as? JsonPrimitive)?.booleanOrNull,
                    maxGalleryImages = (item["max_gallery_images"] as? JsonPrimitive)?.intOrNull,
                ),
            )
        }
    }
    val chatObj = obj["chat"] as? JsonObject
    val maxGroupMembers = (chatObj?.get("max_group_members") as? JsonPrimitive)?.intOrNull ?: 20

    val uiObj = obj["ui"] as? JsonObject
    val annObj = uiObj?.get("announcement") as? JsonObject
    val announcement = RemoteAnnouncement(
        enabled = (annObj?.get("enabled") as? JsonPrimitive)?.booleanOrNull ?: false,
        id = (annObj?.get("id") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        title = (annObj?.get("title") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        message = (annObj?.get("message") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        ctaText = (annObj?.get("cta_text") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        ctaUrl = (annObj?.get("cta_url") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        severity = (annObj?.get("severity") as? JsonPrimitive)?.contentOrNull ?: "info",
        dismissible = (annObj?.get("dismissible") as? JsonPrimitive)?.booleanOrNull ?: true,
    )

    fun parseBanner(slot: JsonObject?): RemoteBannerSlot = RemoteBannerSlot(
        enabled = (slot?.get("enabled") as? JsonPrimitive)?.booleanOrNull ?: false,
        title = (slot?.get("title") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        subtitle = (slot?.get("subtitle") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        imageUrl = (slot?.get("image_url") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        linkUrl = (slot?.get("link_url") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        bgColor = (slot?.get("bg_color") as? JsonPrimitive)?.contentOrNull ?: "#0097C7",
        textColor = (slot?.get("text_color") as? JsonPrimitive)?.contentOrNull ?: "#FFFFFF",
    )

    val themeObj = uiObj?.get("theme_override") as? JsonObject
    val themeOverride = RemoteThemeOverride(
        enabled = (themeObj?.get("enabled") as? JsonPrimitive)?.booleanOrNull ?: false,
        holidayName = (themeObj?.get("holiday_name") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        primaryHex = (themeObj?.get("primary_hex") as? JsonPrimitive)?.contentOrNull ?: "#0097C7",
        accentHex = (themeObj?.get("accent_hex") as? JsonPrimitive)?.contentOrNull ?: "#F0543B",
        badgeText = (themeObj?.get("badge_text") as? JsonPrimitive)?.contentOrNull.orEmpty(),
    )

    val adsObj = uiObj?.get("ads") as? JsonObject
    val ads = RemoteAdsConfig(
        enabled = (adsObj?.get("enabled") as? JsonPrimitive)?.booleanOrNull ?: true,
        feedInterval = (adsObj?.get("feed_interval") as? JsonPrimitive)?.intOrNull ?: 8,
        storiesEnabled = (adsObj?.get("stories_enabled") as? JsonPrimitive)?.booleanOrNull ?: true,
        bannerEnabled = (adsObj?.get("banner_enabled") as? JsonPrimitive)?.booleanOrNull ?: true,
    )

    val quickServices = (uiObj?.get("quick_services") as? JsonArray)?.mapNotNull { elem ->
        val q = elem as? JsonObject ?: return@mapNotNull null
        val key = (q["key"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
        RemoteQuickServiceItem(
            key = key,
            title = (q["title"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            subtitle = (q["subtitle"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            route = (q["route"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            enabled = (q["enabled"] as? JsonPrimitive)?.booleanOrNull ?: true,
            sortOrder = (q["sort_order"] as? JsonPrimitive)?.intOrNull ?: 0,
            gradientStart = (q["gradient_start"] as? JsonPrimitive)?.contentOrNull ?: "#0097C7",
            gradientEnd = (q["gradient_end"] as? JsonPrimitive)?.contentOrNull ?: "#1FB6E8",
            icon = (q["icon"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            badgeText = (q["badge_text"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            featured = (q["featured"] as? JsonPrimitive)?.booleanOrNull ?: false,
        )
    }.orEmpty()

    val hubObj = uiObj?.get("services_hub") as? JsonObject
    val hubDef = RemoteServicesHubConfig()
    val servicesHub = if (hubObj == null) {
        hubDef
    } else {
        fun hubStr(k: String, fallback: String): String =
            (hubObj[k] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() } ?: fallback

        val parsedOrder = (hubObj["section_order"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.lowercase() }
            ?.takeIf { it.isNotEmpty() }
            ?: hubDef.sectionOrder
        RemoteServicesHubConfig(
            headerTitle = hubStr("header_title", hubDef.headerTitle),
            headerSubtitle = (hubObj["header_subtitle"] as? JsonPrimitive)?.contentOrNull ?: hubDef.headerSubtitle,
            announcementText = (hubObj["announcement_text"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            bannersTitle = hubStr("banners_title", hubDef.bannersTitle),
            bannersSubtitle = (hubObj["banners_subtitle"] as? JsonPrimitive)?.contentOrNull ?: hubDef.bannersSubtitle,
            quickTitle = hubStr("quick_title", hubDef.quickTitle),
            quickSubtitle = (hubObj["quick_subtitle"] as? JsonPrimitive)?.contentOrNull ?: hubDef.quickSubtitle,
            contactsTitle = hubStr("contacts_title", hubDef.contactsTitle),
            sectionsTitle = hubStr("sections_title", hubDef.sectionsTitle),
            sectionsSubtitle = (hubObj["sections_subtitle"] as? JsonPrimitive)?.contentOrNull ?: hubDef.sectionsSubtitle,
            quickLayout = hubStr("quick_layout", hubDef.quickLayout).lowercase(),
            bannersLayout = hubStr("banners_layout", hubDef.bannersLayout).lowercase(),
            bannerAutoPlay = (hubObj["banner_auto_play"] as? JsonPrimitive)?.booleanOrNull ?: hubDef.bannerAutoPlay,
            sectionsLayout = hubStr("sections_layout", hubDef.sectionsLayout).lowercase(),
            showBanners = (hubObj["show_banners"] as? JsonPrimitive)?.booleanOrNull ?: hubDef.showBanners,
            showQuickServices = (hubObj["show_quick_services"] as? JsonPrimitive)?.booleanOrNull ?: hubDef.showQuickServices,
            showContacts = (hubObj["show_contacts"] as? JsonPrimitive)?.booleanOrNull ?: hubDef.showContacts,
            showSections = (hubObj["show_sections"] as? JsonPrimitive)?.booleanOrNull ?: hubDef.showSections,
            sectionOrder = parsedOrder,
        )
    }

    val rollouts = (obj["rollouts"] as? JsonObject)?.mapNotNull { (k, v) ->
        val pct = (v as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
        k to pct
    }?.toMap().orEmpty()

    val brandObj = uiObj?.get("brand_ornament") as? JsonObject
    val brandOrnament = RemoteBrandOrnament(
        enabled = (brandObj?.get("enabled") as? JsonPrimitive)?.booleanOrNull ?: false,
        preset = (brandObj?.get("preset") as? JsonPrimitive)?.contentOrNull ?: "none",
        customImageUrl = (brandObj?.get("custom_image_url") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        customLogoLightUrl = (brandObj?.get("custom_logo_light_url") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        customLogoDarkUrl = (brandObj?.get("custom_logo_dark_url") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        offsetX = ((brandObj?.get("offset_x") as? JsonPrimitive)?.intOrNull ?: 0).coerceIn(-24, 24),
        offsetY = ((brandObj?.get("offset_y") as? JsonPrimitive)?.intOrNull ?: -10).coerceIn(-24, 24),
        scale = (((brandObj?.get("scale") as? JsonPrimitive)?.doubleOrNull ?: 1.0).toFloat()).coerceIn(0.5f, 2.0f),
        rotationDeg = ((brandObj?.get("rotation_deg") as? JsonPrimitive)?.intOrNull ?: 0).coerceIn(-45, 45),
        tapAction = (brandObj?.get("tap_action") as? JsonPrimitive)?.contentOrNull ?: "none",
        tapTitle = (brandObj?.get("tap_title") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        tapMessage = (brandObj?.get("tap_message") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        tapUrl = (brandObj?.get("tap_url") as? JsonPrimitive)?.contentOrNull.orEmpty(),
    )

    val ambientObj = uiObj?.get("ambient_effect") as? JsonObject
    val ambientEffect = RemoteAmbientEffect(
        effect = (ambientObj?.get("effect") as? JsonPrimitive)?.contentOrNull ?: "none",
        density = (ambientObj?.get("density") as? JsonPrimitive)?.contentOrNull ?: "low",
    )

    val navigationSlots = (uiObj?.get("navigation_slots") as? JsonArray)?.mapNotNull { elem ->
        val s = elem as? JsonObject ?: return@mapNotNull null
        val key = (s["key"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
        RemoteNavigationSlot(
            key = key,
            label = (s["label"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            visible = (s["visible"] as? JsonPrimitive)?.booleanOrNull ?: true,
            sortOrder = (s["sort_order"] as? JsonPrimitive)?.intOrNull ?: 0,
            badgeText = (s["badge_text"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            icon = (s["icon"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            route = (s["route"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
        )
    }.orEmpty()

    val copyObj = uiObj?.get("copy_overrides") as? JsonObject
    val copyOverrides = RemoteCopyOverrides(
        feedForYouTab = (copyObj?.get("feed_for_you_tab") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        feedFollowingTab = (copyObj?.get("feed_following_tab") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        maintenanceTitle = (copyObj?.get("maintenance_title") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        maintenanceMessage = (copyObj?.get("maintenance_message") as? JsonPrimitive)?.contentOrNull.orEmpty(),
        composerPlaceholder = (copyObj?.get("composer_placeholder") as? JsonPrimitive)?.contentOrNull.orEmpty(),
    )

    val pricingObj = obj["pricing"] as? JsonObject
    val paymentGateway = when ((pricingObj?.get("payment_gateway") as? JsonPrimitive)?.contentOrNull?.trim()?.lowercase()) {
        "zibal" -> "zibal"
        else -> "store"
    }
    val pricingMeta = (pricingObj?.get("plans") as? JsonObject)?.mapNotNull { (k, elem) ->
        val p = elem as? JsonObject ?: return@mapNotNull null
        k to RemotePricingPlanMeta(
            enabled = (p["enabled"] as? JsonPrimitive)?.booleanOrNull ?: true,
            originalPriceToman = (p["original_price_toman"] as? JsonPrimitive)?.longOrNull ?: 0L,
            badgeText = (p["badge_text"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            recommended = (p["recommended"] as? JsonPrimitive)?.booleanOrNull ?: false,
        )
    }?.toMap().orEmpty()
    val premiumFeatures = (pricingObj?.get("premium_features") as? JsonArray)?.mapNotNull { elem ->
        (elem as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
    }.orEmpty()

    return RemoteConfigSnapshot(
        version = (obj["version"] as? JsonPrimitive)?.longOrNull ?: 1L,
        tiers = tiersMap,
        maxGroupMembers = maxGroupMembers,
        announcement = announcement,
        feedBanner = parseBanner(uiObj?.get("feed_banner") as? JsonObject),
        exploreBanner = parseBanner(uiObj?.get("explore_banner") as? JsonObject),
        themeOverride = themeOverride,
        ads = ads,
        quickServices = quickServices,
        servicesHub = servicesHub,
        brandOrnament = brandOrnament,
        ambientEffect = ambientEffect,
        navigationSlots = navigationSlots,
        copyOverrides = copyOverrides,
        paymentGateway = paymentGateway,
        pricingMeta = pricingMeta,
        premiumFeatures = premiumFeatures,
        rollouts = rollouts,
    )
}
