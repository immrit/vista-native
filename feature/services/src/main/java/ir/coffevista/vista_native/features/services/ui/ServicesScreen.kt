package ir.coffevista.vista_native.features.services.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.component.VistaMiniPlayerHost
import ir.coffevista.vista_native.core.designsystem.component.rememberVistaHaptics
import ir.coffevista.vista_native.core.designsystem.component.rememberVistaShimmerBrush
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassScrollSync
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassTokens
import ir.coffevista.vista_native.core.designsystem.glass.rememberVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlass
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassSource
import ir.coffevista.vista_native.core.designsystem.icon.VistaIcons
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.theme.vistaColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.core.network.RemoteServicesHubConfig
import ir.coffevista.vista_native.core.network.SystemGateMonitor
import ir.coffevista.vista_native.features.services.data.ServiceBanner
import ir.coffevista.vista_native.features.services.data.ServiceMaintenance
import ir.coffevista.vista_native.features.services.data.ServiceSection
import ir.coffevista.vista_native.features.services.data.normalizedActiveSections
import ir.coffevista.vista_native.features.services.data.normalizedHubSectionOrder
import kotlinx.coroutines.delay

private data class QuickButtonItem(
    val key: String,
    val label: String,
    val subtitle: String,
    val kind: ServicesActionKind,
    val iconName: String,
    val badgeText: String,
    val featured: Boolean,
    val gradientColors: List<Color>,
    val onClick: () -> Unit,
)

private fun Modifier.vistaCardPress(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val haptics = rememberVistaHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "vista_services_card_scale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = ripple(bounded = true),
            role = Role.Button,
        ) {
            haptics.tap()
            onClick()
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    onNavigateToNearby: () -> Unit,
    onNavigateToGame: () -> Unit,
    onNavigateToTopGroups: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToWeb: (url: String, title: String) -> Unit,
    onNavigateToSectionRoute: (route: String) -> Unit,
    viewModel: ServicesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val remoteConfig by SystemGateMonitor.remoteConfig.collectAsStateWithLifecycle()
    val hubConfig = remoteConfig?.servicesHub ?: RemoteServicesHubConfig()
    val isDarkServices = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val dispatchRoute: (String, String) -> Unit = remember(
        onNavigateToNearby,
        onNavigateToGame,
        onNavigateToTopGroups,
        onNavigateToContacts,
        onNavigateToWeb,
        onNavigateToSectionRoute,
    ) {
        { routeRaw, title ->
            val route = routeRaw.trim()
            when (route.lowercase()) {
                "/nearby", "vista://nearby" -> onNavigateToNearby()
                "/game", "/games", "vista://game", "vista://games" -> onNavigateToGame()
                "/groups", "/group", "vista://groups", "vista://group" -> onNavigateToTopGroups()
                "/contacts", "/contact", "vista://contacts", "vista://contact" -> onNavigateToContacts()
                else -> if (route.startsWith("http", ignoreCase = true)) {
                    onNavigateToWeb(route, title)
                } else if (route.isNotEmpty()) {
                    onNavigateToSectionRoute(route)
                }
            }
        }
    }

    val defaultQuickButtons = remember(
        isDarkServices,
        onNavigateToNearby,
        onNavigateToGame,
        onNavigateToTopGroups,
        onNavigateToContacts,
    ) {
        val groupsAccent = if (isDarkServices) Color(0xFFE5E7EB) else VistaBrandColors.InkBlack
        listOf(
            "nearby" to QuickButtonItem(
                key = "nearby",
                label = "اطراف من",
                subtitle = "آدم‌های نزدیکت را پیدا کن",
                kind = ServicesActionKind.NEARBY,
                iconName = "radar",
                badgeText = "",
                featured = true,
                gradientColors = listOf(VistaBrandColors.ViuBlueDeep, VistaBrandColors.ViuBlue),
                onClick = onNavigateToNearby,
            ),
            "game" to QuickButtonItem(
                key = "game",
                label = "بازی",
                subtitle = "رقابت کن و امتیاز بگیر",
                kind = ServicesActionKind.GAME,
                iconName = "game",
                badgeText = "رقابت",
                featured = false,
                gradientColors = listOf(VistaBrandColors.Coral, VistaBrandColors.CoralSoft),
                onClick = onNavigateToGame,
            ),
            "groups" to QuickButtonItem(
                key = "groups",
                label = "گروه‌ها",
                subtitle = "جمع‌های محبوب ویستا را ببین",
                kind = ServicesActionKind.GROUPS,
                iconName = "groups",
                badgeText = "",
                featured = false,
                gradientColors = listOf(groupsAccent, groupsAccent.copy(alpha = 0.85f)),
                onClick = onNavigateToTopGroups,
            ),
            "contacts" to QuickButtonItem(
                key = "contacts",
                label = "مخاطبین",
                subtitle = "دوستانت را در ویستا پیدا کن",
                kind = ServicesActionKind.CONTACTS,
                iconName = "contacts",
                badgeText = "",
                featured = false,
                gradientColors = listOf(VistaBrandColors.ViuBlue, VistaBrandColors.ViuBlueDeep),
                onClick = onNavigateToContacts,
            ),
        )
    }

    val quickButtons = remember(defaultQuickButtons, remoteConfig, dispatchRoute) {
        val overrides = remoteConfig?.quickServices.orEmpty()
        if (overrides.isEmpty()) {
            defaultQuickButtons.map { it.second }
        } else {
            val baseMap = defaultQuickButtons.toMap()
            overrides
                .filter { it.enabled }
                .sortedBy { it.sortOrder }
                .mapNotNull { ov ->
                    val base = baseMap[ov.key]
                    val parsedColors = listOfNotNull(
                        parseHexColorOrNull(ov.gradientStart),
                        parseHexColorOrNull(ov.gradientEnd),
                    ).takeIf { it.size == 2 }
                    if (base != null) {
                        val route = ov.route.trim()
                        val clickHandler = if (route.isNotBlank()) {
                            { dispatchRoute(route, ov.title.ifBlank { base.label }) }
                        } else {
                            base.onClick
                        }
                        base.copy(
                            label = ov.title.ifBlank { base.label },
                            subtitle = ov.subtitle.ifBlank { base.subtitle },
                            iconName = ov.icon.ifBlank { base.iconName },
                            badgeText = ov.badgeText,
                            featured = ov.featured || (ov.key == "nearby" && overrides.none { it.featured }),
                            gradientColors = parsedColors ?: base.gradientColors,
                            onClick = clickHandler,
                        )
                    } else {
                        val title = ov.title.trim()
                        val route = ov.route.trim()
                        if (title.isBlank() || route.isBlank()) return@mapNotNull null
                        QuickButtonItem(
                            key = ov.key,
                            label = title,
                            subtitle = ov.subtitle.ifBlank { "باز کردن سرویس" },
                            kind = ServicesActionKind.CUSTOM,
                            iconName = ov.icon.ifBlank { "grid" },
                            badgeText = ov.badgeText,
                            featured = ov.featured,
                            gradientColors = parsedColors
                                ?: listOf(VistaBrandColors.ViuBlueDeep, VistaBrandColors.ViuBlue),
                            onClick = { dispatchRoute(route, title) },
                        )
                    }
                }
        }
    }

    val blockOrder = remember(hubConfig.sectionOrder) {
        normalizedHubSectionOrder(hubConfig.sectionOrder)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val servicesGlassState = rememberVistaGlassState()
        val servicesListState = rememberLazyListState()
        VistaGlassScrollSync(servicesGlassState, servicesListState)

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .vistaGlass(
                                state = servicesGlassState,
                                shape = RectangleShape,
                                tint = if (isDarkServices) Color(0xFF16181C) else Color(0xFFFFFFFF),
                                isDark = isDarkServices,
                                blurRadius = VistaGlassTokens.BarBlur,
                                tintAlphaOverride = if (isDarkServices) 0.72f else 0.78f,
                                drawHighlight = true,
                                rimWidth = 0.dp,
                                shadowElevation = 2.dp,
                            ),
                    ) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = hubConfig.headerTitle.ifBlank { "سرویس‌ها" },
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontFamily = VistaFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 21.sp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            letterSpacing = (-0.4).sp,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (hubConfig.headerSubtitle.isNotBlank()) {
                                        Text(
                                            text = hubConfig.headerSubtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = VistaFontFamily,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.vistaColors.contentSecondary,
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            },
                            actions = {
                                val refreshTransition = rememberInfiniteTransition(label = "services_refresh_spin")
                                val spinAngle by refreshTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 850, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart,
                                    ),
                                    label = "spin",
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = if (isDarkServices) 0.55f else 0.65f,
                                            ),
                                        )
                                        .border(
                                            width = 0.75.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = CircleShape,
                                        )
                                        .vistaCardPress { viewModel.refresh() }
                                        .semantics { contentDescription = "به‌روزرسانی" },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = VistaIcons.Refresh,
                                        contentDescription = "به‌روزرسانی",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier
                                            .size(19.dp)
                                            .graphicsLayer {
                                                rotationZ = if (state.isRefreshing) spinAngle else 0f
                                            },
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            ),
                        )
                    }
                    VistaMiniPlayerHost(glassState = servicesGlassState)
                }
            },
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .vistaGlassSource(servicesGlassState)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                val maintenance = (state.hubState as? ServicesHubUiState.Success)?.data?.maintenance
                if (maintenance != null && maintenance.isEnabled) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        ServicesMaintenanceView(
                            maintenance = maintenance,
                            onRetry = viewModel::refresh,
                        )
                    }
                } else {
                    LazyColumn(
                        state = servicesListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding() + 6.dp,
                            bottom = 124.dp,
                        ),
                    ) {
                        if (hubConfig.announcementText.isNotBlank()) {
                            item(key = "hub_announcement") {
                                HubAnnouncementStrip(
                                    text = hubConfig.announcementText,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                )
                            }
                        }

                        blockOrder.forEach { blockKey ->
                            when (blockKey) {
                                "banners" -> if (hubConfig.showBanners) {
                                    when (val hub = state.hubState) {
                                        is ServicesHubUiState.Loading -> {
                                            item(key = "banners_shimmer") {
                                                BannerLoadingSkeleton()
                                            }
                                        }
                                        is ServicesHubUiState.Success -> {
                                            val banners = hub.data.banners
                                                .filter { it.isActive }
                                                .sortedWith(compareBy({ it.sortOrder }, { it.id }))
                                            if (banners.isNotEmpty()) {
                                                item(key = "banners_heading") {
                                                    SectionHeading(
                                                        title = hubConfig.bannersTitle.ifBlank { "برای شما" },
                                                        subtitle = hubConfig.bannersSubtitle,
                                                    )
                                                }
                                                if (hubConfig.bannersLayout == "carousel" && banners.size > 1) {
                                                    item(key = "banners_carousel") {
                                                        BannersCarousel(
                                                            banners = banners,
                                                            autoPlay = hubConfig.bannerAutoPlay,
                                                            onBannerClick = { banner ->
                                                                if (banner.link.isNotBlank() && banner.linkType != "none") {
                                                                    if (banner.linkType == "web" || banner.link.startsWith("http")) {
                                                                        onNavigateToWeb(banner.link, banner.title)
                                                                    } else {
                                                                        onNavigateToSectionRoute(banner.link)
                                                                    }
                                                                }
                                                            },
                                                        )
                                                    }
                                                } else {
                                                    items(banners, key = { "banner_${it.id}" }) { banner ->
                                                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                                                            BannerCard(
                                                                banner = banner,
                                                                onBannerClick = {
                                                                    if (banner.link.isNotBlank() && banner.linkType != "none") {
                                                                        if (banner.linkType == "web" || banner.link.startsWith("http")) {
                                                                            onNavigateToWeb(banner.link, banner.title)
                                                                        } else {
                                                                            onNavigateToSectionRoute(banner.link)
                                                                        }
                                                                    }
                                                                },
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        is ServicesHubUiState.Error -> Unit
                                    }
                                }

                                "quick_services" -> if (hubConfig.showQuickServices && quickButtons.isNotEmpty()) {
                                    item(key = "quick_heading") {
                                        SectionHeading(
                                            title = hubConfig.quickTitle.ifBlank { "دسترسی سریع" },
                                            subtitle = hubConfig.quickSubtitle,
                                        )
                                    }
                                    item(key = "quick_content") {
                                        QuickServicesSection(
                                            buttons = quickButtons,
                                            layout = hubConfig.quickLayout,
                                            isDark = isDarkServices,
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                        )
                                    }
                                }

                                "contacts" -> if (hubConfig.showContacts) {
                                    item(key = "contacts_section") {
                                        ContactsHorizontalSection(
                                            title = hubConfig.contactsTitle.ifBlank { "آشناها در ویستا" },
                                            state = state.contactsState,
                                            onAllContactsClick = onNavigateToContacts,
                                            onContactClick = onNavigateToUserProfile,
                                            onRetry = { viewModel.loadHub() },
                                            onGrantPermission = onNavigateToContacts,
                                        )
                                    }
                                }

                                "sections" -> if (hubConfig.showSections) {
                                    when (val hub = state.hubState) {
                                        is ServicesHubUiState.Loading -> {
                                            item(key = "sections_shimmer") {
                                                SectionsLoadingSkeleton()
                                            }
                                        }
                                        is ServicesHubUiState.Success -> {
                                            val sections = normalizedActiveSections(hub.data.sections)
                                            if (sections.isNotEmpty()) {
                                                item(key = "sections_heading") {
                                                    SectionHeading(
                                                        title = hubConfig.sectionsTitle.ifBlank { "سرویس‌های بیشتر" },
                                                        subtitle = hubConfig.sectionsSubtitle,
                                                    )
                                                }
                                                item(key = "sections_content") {
                                                    val openSection: (ServiceSection) -> Unit = { section ->
                                                        val route = section.route.trim()
                                                        if (route.startsWith("http", ignoreCase = true)) {
                                                            onNavigateToWeb(route, section.title)
                                                        } else {
                                                            onNavigateToSectionRoute(route)
                                                        }
                                                    }
                                                    when (hubConfig.sectionsLayout) {
                                                        "grid" -> ManagedSectionsGrid(
                                                            sections = sections,
                                                            isDark = isDarkServices,
                                                            onSectionClick = openSection,
                                                            modifier = Modifier.padding(horizontal = 16.dp),
                                                        )
                                                        "list" -> Column(
                                                            modifier = Modifier.padding(horizontal = 16.dp),
                                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                                        ) {
                                                            sections.forEach { section ->
                                                                ManagedSectionListCard(
                                                                    section = section,
                                                                    isDark = isDarkServices,
                                                                    onClick = { openSection(section) },
                                                                )
                                                            }
                                                        }
                                                        else -> LazyRow(
                                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        ) {
                                                            items(sections, key = { "sec_${it.id}" }) { section ->
                                                                ManagedSectionCard(
                                                                    section = section,
                                                                    isDark = isDarkServices,
                                                                    onClick = { openSection(section) },
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        is ServicesHubUiState.Error -> {
                                            item(key = "sections_error") {
                                                InlineHubErrorState(
                                                    title = "تازه‌های ویستا بارگذاری نشد",
                                                    subtitle = "سرویس‌های اصلی همچنان در دسترس‌اند.",
                                                    onRetry = { viewModel.loadHub() },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubAnnouncementStrip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.vistaColors.viuBlue, CircleShape),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = VistaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.2).sp,
            ),
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = VistaFontFamily,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.vistaColors.contentSecondary,
                ),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BannersCarousel(
    banners: List<ServiceBanner>,
    autoPlay: Boolean,
    onBannerClick: (ServiceBanner) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { banners.size })
    LaunchedEffect(pagerState, banners.size, autoPlay) {
        if (!autoPlay || banners.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5000L)
            if (!pagerState.isScrollInProgress && banners.isNotEmpty()) {
                val next = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val banner = banners[page]
            BannerCard(
                banner = banner,
                onBannerClick = { onBannerClick(banner) },
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(banners.size) { index ->
                val active = pagerState.currentPage == index
                val dotWidth by animateDpAsState(
                    targetValue = if (active) 20.dp else 6.dp,
                    animationSpec = tween(220),
                    label = "banner_dot_width",
                )
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(dotWidth)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.vistaColors.viuBlue
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }
    }
}

@Composable
private fun BannerLoadingSkeleton() {
    val brush = rememberVistaShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(92.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(brush),
        )
    }
}

@Composable
private fun SectionsLoadingSkeleton() {
    val brush = rememberVistaShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(116.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush),
                )
            }
        }
    }
}

@Composable
private fun QuickServicesSection(
    buttons: List<QuickButtonItem>,
    layout: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    if (buttons.isEmpty()) return

    when (layout) {
        "list" -> {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                buttons.forEach { button ->
                    CompactWideQuickCard(button = button, isDark = isDark)
                }
            }
        }
        "grid" -> {
            val rows = buttons.chunked(2)
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rows.forEach { rowItems ->
                    if (rowItems.size == 1) {
                        CompactWideQuickCard(button = rowItems[0], isDark = isDark)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            QuickGridCard(
                                button = rowItems[0],
                                isDark = isDark,
                                modifier = Modifier.weight(1f),
                            )
                            QuickGridCard(
                                button = rowItems[1],
                                isDark = isDark,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        else -> {
            if (buttons.size == 1) {
                HeroBentoQuickCard(
                    button = buttons[0],
                    isDark = isDark,
                    modifier = modifier,
                )
                return
            }
            val heroIndex = buttons.indexOfFirst { it.featured }.takeIf { it >= 0 } ?: 0
            val hero = buttons[heroIndex]
            val remaining = buttons.filterIndexed { index, _ -> index != heroIndex }
            val rows = remaining.chunked(2)

            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeroBentoQuickCard(button = hero, isDark = isDark)
                rows.forEach { rowItems ->
                    if (rowItems.size == 1) {
                        CompactWideQuickCard(button = rowItems[0], isDark = isDark)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            QuickGridCard(
                                button = rowItems[0],
                                isDark = isDark,
                                modifier = Modifier.weight(1f),
                            )
                            QuickGridCard(
                                button = rowItems[1],
                                isDark = isDark,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBentoQuickCard(
    button: QuickButtonItem,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = button.gradientColors.first()
    val cardBg = if (isDark) Color(0xFF1A1D23) else Color(0xFF121418)
    val borderBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                accent.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.08f),
            ),
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                accent.copy(alpha = 0.25f),
            ),
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${button.label}، ${button.subtitle}"
            },
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(0.9.dp, borderBrush),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .vistaCardPress(onClick = button.onClick)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = if (isDark) 0.22f else 0.28f),
                            Color.Transparent,
                        ),
                        center = Offset(140f, 120f),
                        radius = 420f,
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = accent.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(15.dp),
                        )
                        .border(
                            width = 0.75.dp,
                            color = accent.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(15.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    ServicesActionIcon(
                        kind = button.kind,
                        iconName = button.iconName,
                        color = if (accent.luminance() < 0.2f) Color.White else accent,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = button.label,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = Color.White,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (button.badgeText.isNotBlank()) {
                            ServiceBadgePill(
                                text = button.badgeText,
                                accent = accent,
                                onDarkCard = true,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = button.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = VistaFontFamily,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.76f),
                            lineHeight = 17.sp,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickGridCard(
    button: QuickButtonItem,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = button.gradientColors.first()
    val safeAccent = if (!isDark && accent.luminance() > 0.85f) {
        VistaBrandColors.InkBlack
    } else if (isDark && accent.luminance() < 0.08f) {
        Color(0xFFE5E7EB)
    } else {
        accent
    }

    Surface(
        modifier = modifier
            .height(126.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${button.label}، ${button.subtitle}"
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .vistaCardPress(onClick = button.onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = safeAccent.copy(alpha = if (isDark) 0.16f else 0.11f),
                            shape = RoundedCornerShape(13.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    ServicesActionIcon(
                        kind = button.kind,
                        iconName = button.iconName,
                        color = safeAccent,
                        modifier = Modifier.size(21.dp),
                    )
                }
                if (button.badgeText.isNotBlank()) {
                    ServiceBadgePill(
                        text = button.badgeText,
                        accent = safeAccent,
                        onDarkCard = isDark,
                    )
                }
            }

            Column {
                Text(
                    text = button.label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = button.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = VistaFontFamily,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.vistaColors.contentSecondary,
                        lineHeight = 15.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CompactWideQuickCard(
    button: QuickButtonItem,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = button.gradientColors.first()
    val safeAccent = if (!isDark && accent.luminance() > 0.85f) {
        VistaBrandColors.InkBlack
    } else if (isDark && accent.luminance() < 0.08f) {
        Color(0xFFE5E7EB)
    } else {
        accent
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${button.label}، ${button.subtitle}"
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .vistaCardPress(onClick = button.onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = safeAccent.copy(alpha = if (isDark) 0.16f else 0.11f),
                        shape = RoundedCornerShape(13.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                ServicesActionIcon(
                    kind = button.kind,
                    iconName = button.iconName,
                    color = safeAccent,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = button.label,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (button.badgeText.isNotBlank()) {
                        ServiceBadgePill(
                            text = button.badgeText,
                            accent = safeAccent,
                            onDarkCard = isDark,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = button.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = VistaFontFamily,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.vistaColors.contentSecondary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.vistaColors.contentSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ServiceBadgePill(
    text: String,
    accent: Color,
    onDarkCard: Boolean,
) {
    val badgeBg = if (onDarkCard) {
        accent.copy(alpha = 0.24f)
    } else {
        accent.copy(alpha = 0.12f)
    }
    val badgeText = if (onDarkCard) Color.White else accent
    Box(
        modifier = Modifier
            .background(badgeBg, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = VistaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = badgeText,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun BannerCard(
    banner: ServiceBanner,
    onBannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = parseHexColor(banner.bgColor, VistaBrandColors.InkBlack)
    val textColor = parseHexColor(banner.textColor, Color.White)
    val height = banner.heightDp.dp
    val clickable = banner.link.isNotBlank() && banner.linkType != "none"
    val hasImage = banner.imageUrl.isNotBlank()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                role = Role.Button
                contentDescription = if (banner.subtitle.isEmpty()) banner.title else "${banner.title}، ${banner.subtitle}"
            },
        shape = RoundedCornerShape(22.dp),
        color = bgColor,
        border = BorderStroke(
            width = 0.75.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .vistaCardPress(enabled = clickable, onClick = onBannerClick),
        ) {
            if (hasImage) {
                AsyncImage(
                    model = banner.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.06f),
                                    Color.Black.copy(alpha = 0.68f),
                                ),
                            ),
                        ),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.16f),
                                ),
                            ),
                        ),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (banner.title.isNotBlank()) {
                        Text(
                            text = banner.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (banner.heightDp > 160) 18.sp else 15.5.sp,
                                color = textColor,
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (banner.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = banner.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VistaFontFamily,
                                fontSize = 11.5.sp,
                                color = textColor.copy(alpha = 0.88f),
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (clickable) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(textColor.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsHorizontalSection(
    title: String,
    state: ContactsRailUiState,
    onAllContactsClick: () -> Unit,
    onContactClick: (String) -> Unit,
    onRetry: () -> Unit,
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viuBlue = MaterialTheme.vistaColors.viuBlue
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 22.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 0.75.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        when (state) {
            is ContactsRailUiState.Success -> {
                val users = state.users
                Column(modifier = Modifier.padding(vertical = 15.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    viuBlue.copy(alpha = 0.13f),
                                    RoundedCornerShape(12.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.People,
                                contentDescription = null,
                                tint = viuBlue,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = VistaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                ),
                            )
                            Text(
                                text = "${users.size} نفر از مخاطبینت اینجا هستند",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VistaFontFamily,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.vistaColors.contentSecondary,
                                ),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(viuBlue.copy(alpha = 0.12f))
                                .vistaCardPress(onClick = onAllContactsClick)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "همه",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = VistaFontFamily,
                                    color = viuBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(users, key = { it.id }) { user ->
                            Column(
                                modifier = Modifier
                                    .width(68.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .vistaCardPress { onContactClick(user.id) }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    if (user.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = user.avatarUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .border(
                                                    width = 1.5.dp,
                                                    color = viuBlue.copy(alpha = 0.35f),
                                                    shape = CircleShape,
                                                )
                                                .padding(2.dp)
                                                .clip(CircleShape),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(
                                                    viuBlue.copy(alpha = 0.13f),
                                                    CircleShape,
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = viuBlue.copy(alpha = 0.3f),
                                                    shape = CircleShape,
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = user.fullName.firstOrNull()?.toString() ?: "?",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = VistaFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = viuBlue,
                                                ),
                                            )
                                        }
                                    }
                                    if (user.isVerified) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(viuBlue, CircleShape)
                                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp),
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (user.username.isNotBlank()) "@${user.username}" else user.fullName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VistaFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
            is ContactsRailUiState.PermissionRequired -> {
                CompactStateBanner(
                    iconName = "contact_page",
                    title = "دوستانت را در ویستا پیدا کن",
                    subtitle = "برای دیدن دوستان ویستایی، دسترسی مخاطبین را فعال کن.",
                    actionLabel = "فعال‌سازی",
                    onAction = onGrantPermission,
                )
            }
            is ContactsRailUiState.Empty -> {
                CompactStateBanner(
                    iconName = "people",
                    title = "هنوز دوستی پیدا نشد",
                    subtitle = "هر وقت یکی از مخاطبینت به ویستا بیاید، اینجا می‌بینی.",
                    actionLabel = "مخاطبین",
                    onAction = onAllContactsClick,
                )
            }
            is ContactsRailUiState.Loading -> {
                val shimmer = rememberVistaShimmerBrush()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    repeat(5) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(shimmer),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(shimmer),
                            )
                        }
                    }
                }
            }
            is ContactsRailUiState.Error -> {
                CompactStateBanner(
                    iconName = "error_outline",
                    title = "مخاطبین بارگذاری نشد",
                    subtitle = "می‌توانی دوباره برای همگام‌سازی تلاش کنی.",
                    actionLabel = "تلاش دوباره",
                    onAction = onRetry,
                )
            }
        }
    }
}

@Composable
private fun CompactStateBanner(
    iconName: String,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viuBlue = MaterialTheme.vistaColors.viuBlue
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    viuBlue.copy(alpha = 0.13f),
                    RoundedCornerShape(13.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            val imageVector = when (iconName.lowercase()) {
                "contact_page", "contacts" -> Icons.Rounded.ContactPage
                "person_add" -> Icons.Rounded.PersonAdd
                "lock_open" -> Icons.Rounded.LockOpen
                "cloud_off" -> Icons.Rounded.CloudOff
                else -> Icons.Rounded.People
            }
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = viuBlue,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = VistaFontFamily,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.vistaColors.contentSecondary,
                    lineHeight = 16.sp,
                ),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .vistaCardPress(onClick = onAction)
                .padding(horizontal = 13.dp, vertical = 7.dp),
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = VistaFontFamily,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                ),
            )
        }
    }
}

@Composable
private fun ManagedSectionsGrid(
    sections: List<ServiceSection>,
    isDark: Boolean,
    onSectionClick: (ServiceSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = sections.chunked(2)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { rowItems ->
            if (rowItems.size == 1) {
                ManagedSectionListCard(
                    section = rowItems[0],
                    isDark = isDark,
                    onClick = { onSectionClick(rowItems[0]) },
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ManagedSectionCard(
                        section = rowItems[0],
                        isDark = isDark,
                        onClick = { onSectionClick(rowItems[0]) },
                        modifier = Modifier.weight(1f),
                    )
                    ManagedSectionCard(
                        section = rowItems[1],
                        isDark = isDark,
                        onClick = { onSectionClick(rowItems[1]) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ManagedSectionCard(
    section: ServiceSection,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = parseHexColor(section.color, MaterialTheme.vistaColors.viuBlue)

    Surface(
        modifier = modifier
            .width(222.dp)
            .height(92.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${section.title}، ${section.subtitle}"
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 0.75.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .vistaCardPress(onClick = onClick)
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        accent.copy(alpha = if (isDark) 0.16f else 0.12f),
                        RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                ServicesVectorIcon(
                    name = section.icon,
                    color = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(11.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (section.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = section.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = VistaFontFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.vistaColors.contentSecondary,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = accent.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ManagedSectionListCard(
    section: ServiceSection,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = parseHexColor(section.color, MaterialTheme.vistaColors.viuBlue)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${section.title}، ${section.subtitle}"
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 0.75.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .vistaCardPress(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        accent.copy(alpha = if (isDark) 0.16f else 0.12f),
                        RoundedCornerShape(13.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                ServicesVectorIcon(
                    name = section.icon,
                    color = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (section.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = section.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = VistaFontFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.vistaColors.contentSecondary,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.vistaColors.contentSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun InlineHubErrorState(
    title: String,
    subtitle: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        CompactStateBanner(
            iconName = "cloud_off",
            title = title,
            subtitle = subtitle,
            actionLabel = "تلاش دوباره",
            onAction = onRetry,
        )
    }
}

private fun parseHexColorOrNull(hex: String): Color? = try {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) {
        null
    } else {
        val colorLong = clean.toLong(16)
        if (clean.length == 6) {
            Color((colorLong.toInt()) or 0xFF000000.toInt())
        } else {
            Color(colorLong.toInt())
        }
    }
} catch (_: Exception) {
    null
}

private fun parseHexColor(hex: String, fallback: Color): Color =
    parseHexColorOrNull(hex) ?: fallback

@Composable
private fun ServicesMaintenanceView(
    maintenance: ServiceMaintenance,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "maintenance_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFFF59E0B).copy(alpha = pulseAlpha * 0.45f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                        ),
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(26.dp),
                    )
                    .shadow(12.dp, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Build,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = maintenance.title.ifBlank { "در حال تعمیر و بروزرسانی سرویس‌ها" },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = VistaFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = maintenance.message.ifBlank {
                "سرویس‌های ویستا موقتاً جهت ارتقای زیرساخت و ارائه امکانات جدید در دست تعمیر و نگهداری هستند. لطفاً دقایقی دیگر مجدداً تلاش نمایید."
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = VistaFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            ),
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        if (maintenance.retryAfterSeconds > 0) {
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "مدت زمان تقریبی: ${maintenance.retryAfterSeconds / 60} دقیقه",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD97706),
                            fontSize = 12.sp,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(48.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "بررسی مجدد وضعیت",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                )
            }
        }
    }
}
