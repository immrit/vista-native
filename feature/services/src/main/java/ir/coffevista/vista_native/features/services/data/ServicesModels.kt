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
data class ServicesHubData(
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
