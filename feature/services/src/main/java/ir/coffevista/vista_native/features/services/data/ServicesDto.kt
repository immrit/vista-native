package ir.coffevista.vista_native.features.services.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceSectionDto(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("subtitle")
    val subtitle: String? = null,
    @SerialName("icon")
    val icon: String? = null,
    @SerialName("route")
    val route: String? = null,
    @SerialName("color")
    val color: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
)

@Serializable
data class ServiceBannerDto(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("subtitle")
    val subtitle: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("link")
    val link: String? = null,
    @SerialName("link_type")
    val linkType: String? = null,
    @SerialName("height_type")
    val heightType: String? = null,
    @SerialName("bg_color")
    val bgColor: String? = null,
    @SerialName("text_color")
    val textColor: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
)

@Serializable
data class ServicesHubResponseDto(
    @SerialName("sections")
    val sections: List<ServiceSectionDto> = emptyList(),
    @SerialName("banners")
    val banners: List<ServiceBannerDto> = emptyList(),
)

@Serializable
data class ContactVistaUserDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("username")
    val username: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("is_verified")
    val isVerified: Boolean = false,
    @SerialName("verification_type")
    val verificationType: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
)

@Serializable
data class FindContactsRequestDto(
    @SerialName("phone_numbers")
    val phoneNumbers: List<String> = emptyList(),
)

@Serializable
data class FindContactsResponseDto(
    @SerialName("users")
    val users: List<ContactVistaUserDto> = emptyList(),
)

@Serializable
data class GameSsoTicketResponseDto(
    @SerialName("ticket")
    val ticket: String? = null,
)

@Serializable
data class TopGroupDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("image")
    val image: String? = null,
    @SerialName("score")
    val score: Int = 0,
    @SerialName("member_count")
    val memberCount: Int = 0,
    @SerialName("premium_count")
    val premiumCount: Int = 0,
    @SerialName("verified_count")
    val verifiedCount: Int = 0,
)

@Serializable
data class TopGroupsResponseDto(
    @SerialName("groups")
    val groups: List<TopGroupDto> = emptyList(),
)

internal fun ServiceSectionDto.toDomain(): ServiceSection = ServiceSection(
    id = id ?: 0L,
    title = title.orEmpty(),
    subtitle = subtitle.orEmpty(),
    icon = icon ?: "grid",
    route = route.orEmpty(),
    color = color ?: "#6366F1",
    isActive = isActive,
    sortOrder = sortOrder,
)

internal fun ServiceBannerDto.toDomain(): ServiceBanner = ServiceBanner(
    id = id ?: 0L,
    title = title.orEmpty(),
    subtitle = subtitle.orEmpty(),
    imageUrl = imageUrl.orEmpty(),
    link = link.orEmpty(),
    linkType = linkType ?: "web",
    heightType = heightType ?: "md",
    bgColor = bgColor ?: "#6366F1",
    textColor = textColor ?: "#FFFFFF",
    isActive = isActive,
    sortOrder = sortOrder,
)

internal fun ServicesHubResponseDto.toDomain(): ServicesHubData = ServicesHubData(
    sections = sections.map { it.toDomain() },
    banners = banners.map { it.toDomain() },
)

internal fun ContactVistaUserDto.toDomain(): ContactVistaUser = ContactVistaUser(
    id = id.orEmpty(),
    username = username.orEmpty(),
    fullName = fullName.orEmpty(),
    avatarUrl = avatarUrl.orEmpty(),
    isVerified = isVerified,
    verificationType = verificationType.orEmpty(),
    phoneNumber = phoneNumber.orEmpty(),
)

internal fun TopGroupDto.toDomain(): TopGroup = TopGroup(
    id = id.orEmpty(),
    name = name.orEmpty(),
    image = image?.takeIf(String::isNotBlank),
    score = score,
    memberCount = memberCount,
    premiumCount = premiumCount,
    verifiedCount = verifiedCount,
)
