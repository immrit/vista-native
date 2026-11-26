package ir.coffevista.vista_native.features.services.data

import androidx.compose.runtime.Immutable

@Immutable
data class ServiceSection(
    val id: Long,
    val title: String,
    val subtitle: String,
    val icon: String,
    val route: String,
    val color: String,
    val isActive: Boolean,
    val sortOrder: Int,
)

@Immutable
data class ServiceBanner(
    val id: Long,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val link: String,
    val linkType: String,
    val heightType: String,
    val bgColor: String,
    val textColor: String,
    val isActive: Boolean,
    val sortOrder: Int,
) {
    val heightDp: Int
        get() = when (heightType.lowercase()) {
            "sm" -> 100
            "lg" -> 220
            "xl" -> 300
            else -> 160
        }
}

@Immutable
data class ServiceMaintenance(
    val isEnabled: Boolean = false,
    val title: String = "",
    val message: String = "",
    val retryAfterSeconds: Int = 0,
    val endsAt: String? = null,
)

@Immutable
data class ServicesHubData(
    val maintenance: ServiceMaintenance = ServiceMaintenance(),
    val sections: List<ServiceSection> = emptyList(),
    val banners: List<ServiceBanner> = emptyList(),
)

/**
 * The hub is remotely managed and has historically contained duplicate cards.
 * Keep the first active card by route (or title when no route exists), matching
 * Flutter so duplicate backend payloads cannot displace a distinct service.
 */
internal fun normalizedActiveSections(source: Iterable<ServiceSection>): List<ServiceSection> {
    val sorted = source
        .filter(ServiceSection::isActive)
        .sortedWith(compareBy<ServiceSection> { it.sortOrder }.thenBy { it.id })
    val unique = linkedMapOf<String, ServiceSection>()
    sorted.forEach { section ->
        val route = section.route.trim().lowercase()
        val title = section.title.trim().lowercase()
        val key = if (route.isNotEmpty()) "route:$route" else "title:$title"
        unique.putIfAbsent(key, section)
    }
    return unique.values.toList()
}

internal val DEFAULT_HUB_SECTION_ORDER = listOf(
    "banners",
    "quick_services",
    "contacts",
    "sections",
)

/**
 * Normalizes remote-configured section ordering for ServicesScreen:
 * keeps valid unique keys in requested order and appends any missing defaults.
 */
internal fun normalizedHubSectionOrder(order: List<String>): List<String> {
    val allowed = DEFAULT_HUB_SECTION_ORDER.toSet()
    val seen = linkedSetOf<String>()
    order.forEach { raw ->
        val normalized = raw.trim().lowercase()
        if (normalized in allowed) {
            seen.add(normalized)
        }
    }
    DEFAULT_HUB_SECTION_ORDER.forEach { key ->
        seen.add(key)
    }
    return seen.toList()
}

@Immutable
data class ContactVistaUser(
    val id: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val isVerified: Boolean,
    val verificationType: String,
    val phoneNumber: String,
)

@Immutable
data class TopGroup(
    val id: String,
    val name: String,
    val image: String?,
    val score: Int,
    val memberCount: Int,
    val premiumCount: Int,
    val verifiedCount: Int,
)
