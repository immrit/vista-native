package ir.coffevista.vista_native.features.services.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.theme.vistaColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.ContactVistaUser
import ir.coffevista.vista_native.features.services.data.ServiceBanner
import ir.coffevista.vista_native.features.services.data.ServiceSection
import ir.coffevista.vista_native.features.services.data.normalizedActiveSections

private data class QuickButtonItem(
    val label: String,
    val subtitle: String,
    val kind: ServicesActionKind,
    val motifNames: List<String>,
    val gradientColors: List<Color>,
    val onClick: () -> Unit,
)

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

    val quickButtons = remember(
        onNavigateToNearby,
        onNavigateToGame,
        onNavigateToTopGroups,
        onNavigateToContacts,
    ) {
        listOf(
            QuickButtonItem(
                label = "اطراف من",
                subtitle = "آدم‌های نزدیکت را پیدا کن",
                kind = ServicesActionKind.NEARBY,
                motifNames = listOf("location", "location", "location"),
                gradientColors = listOf(VistaBrandColors.Indigo, VistaBrandColors.Violet),
                onClick = onNavigateToNearby,
            ),
            QuickButtonItem(
                label = "بازی",
                subtitle = "رقابت کن و امتیاز بگیر",
                kind = ServicesActionKind.GAME,
                motifNames = listOf("trophy", "bolt", "stars"),
                gradientColors = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
                onClick = onNavigateToGame,
            ),
            QuickButtonItem(
                label = "گروه‌ها",
                subtitle = "جمع‌های محبوب ویستا را ببین",
                kind = ServicesActionKind.GROUPS,
                motifNames = listOf("chat", "person_add", "chat"),
                gradientColors = listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
                onClick = onNavigateToTopGroups,
            ),
            QuickButtonItem(
                label = "مخاطبین",
                subtitle = "دوستانت را در ویستا پیدا کن",
                kind = ServicesActionKind.CONTACTS,
                motifNames = listOf("person_add", "phone", "phone"),
                gradientColors = listOf(Color(0xFF00B4DB), Color(0xFF0083B0)),
                onClick = onNavigateToContacts,
            ),
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "سرویس‌ها",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = VistaBrandColors.Indigo,
                                letterSpacing = (-0.4).sp,
                            ),
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.refresh() },
                            modifier = Modifier.semantics {
                                contentDescription = "به‌روزرسانی"
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "به‌روزرسانی",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    // 1. Dynamic Banners ("برای شما")
                    when (val hub = state.hubState) {
                        is ServicesHubUiState.Success -> {
                            val banners = hub.data.banners.filter { it.isActive }
                                .sortedWith(compareBy({ it.sortOrder }, { it.id }))
                            if (banners.isNotEmpty()) {
                                item {
                                    SectionHeading(
                                        title = "برای شما",
                                        subtitle = "انتخاب‌های تازه ویستا",
                                    )
                                }
                                items(banners, key = { it.id }) { banner ->
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
                        else -> Unit
                    }

                    // 2. Quick Actions Grid ("دسترسی سریع")
                    item {
                        SectionHeading(
                            title = "دسترسی سریع",
                            subtitle = "مسیرهای پرکاربرد",
                        )
                    }

                    item {
                        QuickButtonsGrid(
                            buttons = quickButtons,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    // 3. Contacts Rail ("آشناها در ویستا")
                    item {
                        ContactsHorizontalSection(
                            state = state.contactsState,
                            onAllContactsClick = onNavigateToContacts,
                            onContactClick = onNavigateToUserProfile,
                            onRetry = { viewModel.loadHub() },
                            onGrantPermission = onNavigateToContacts,
                        )
                    }

                    // 4. Managed Dynamic Sections Rail ("سرویس‌های بیشتر")
                    when (val hub = state.hubState) {
                        is ServicesHubUiState.Success -> {
                            val sections = normalizedActiveSections(hub.data.sections)
                            if (sections.isNotEmpty()) {
                                item {
                                    SectionHeading(
                                        title = "سرویس‌های بیشتر",
                                        subtitle = "انتخاب‌هایی که از ویستا برایت آماده شده",
                                    )
                                }
                                item {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                                    ) {
                                        items(sections, key = { it.id }) { section ->
                                            ManagedSectionCard(
                                                section = section,
                                                onClick = {
                                                    val route = section.route.trim()
                                                    if (route.startsWith("http")) {
                                                        onNavigateToWeb(route, section.title)
                                                    } else {
                                                        onNavigateToSectionRoute(route)
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        is ServicesHubUiState.Error -> {
                            item {
                                InlineHubErrorState(
                                    title = "تازه‌های ویستا بارگذاری نشد",
                                    subtitle = "سرویس‌های اصلی همچنان در دسترس‌اند.",
                                    onRetry = { viewModel.loadHub() },
                                )
                            }
                        }
                        is ServicesHubUiState.Loading -> Unit
                    }
                }
            }
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
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground,
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = VistaFontFamily,
                fontSize = 11.sp,
                color = MaterialTheme.vistaColors.contentSecondary,
            ),
        )
    }
}

@Composable
private fun QuickButtonsGrid(
    buttons: List<QuickButtonItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickCard(button = buttons[0], modifier = Modifier.weight(1f))
            QuickCard(button = buttons[1], modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickCard(button = buttons[2], modifier = Modifier.weight(1f))
            QuickCard(button = buttons[3], modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickCard(
    button: QuickButtonItem,
    modifier: Modifier = Modifier,
) {
    val accent = button.gradientColors.first()
    val surface = MaterialTheme.colorScheme.surface
    val startTint = accent.copy(alpha = 0.12f)
    val endTint = button.gradientColors.last().copy(alpha = 0.07f)

    Surface(
        modifier = modifier
            .aspectRatio(1.22f)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = accent.copy(alpha = 0.15f),
                spotColor = accent.copy(alpha = 0.25f),
            )
            .semantics {
                role = Role.Button
                contentDescription = "${button.label}، ${button.subtitle}"
            },
        shape = RoundedCornerShape(20.dp),
        color = surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = 0.18f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(startTint, endTint),
                    ),
                )
                .clickable(onClick = button.onClick),
            contentAlignment = Alignment.Center,
        ) {
            // Background motifs
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp)),
            ) {
                if (button.motifNames.isNotEmpty()) {
                    ServicesMotifIcon(
                        iconName = button.motifNames[0],
                        color = accent.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(34.dp)
                            .align(Alignment.TopStart)
                            .offset(x = (-4).dp, y = 10.dp),
                    )
                }
                if (button.motifNames.size > 1) {
                    ServicesMotifIcon(
                        iconName = button.motifNames[1],
                        color = accent.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(38.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = 38.dp),
                    )
                }
                if (button.motifNames.size > 2) {
                    ServicesMotifIcon(
                        iconName = button.motifNames[2],
                        color = accent.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(30.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = 16.dp, y = 8.dp),
                    )
                }
            }

            // Foreground Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Elevated Glowing Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = accent.copy(alpha = 0.35f),
                            spotColor = accent.copy(alpha = 0.45f),
                        )
                        .background(
                            brush = Brush.linearGradient(button.gradientColors),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    ServicesActionIcon(
                        kind = button.kind,
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = button.label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = button.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = VistaFontFamily,
                        fontSize = 9.5.sp,
                        color = MaterialTheme.vistaColors.contentSecondary,
                        lineHeight = 13.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BannerCard(
    banner: ServiceBanner,
    onBannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = parseHexColor(banner.bgColor, VistaBrandColors.Indigo)
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
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = clickable, onClick = onBannerClick),
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
                                    Color.Black.copy(alpha = 0.05f),
                                    Color.Black.copy(alpha = 0.65f),
                                ),
                            ),
                        ),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                fontWeight = FontWeight.Black,
                                fontSize = if (banner.heightDp > 160) 17.sp else 14.5.sp,
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
                                fontSize = 11.sp,
                                color = textColor.copy(alpha = 0.88f),
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (clickable) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(textColor.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsHorizontalSection(
    state: ContactsRailUiState,
    onAllContactsClick: () -> Unit,
    onContactClick: (String) -> Unit,
    onRetry: () -> Unit,
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        when (state) {
            is ContactsRailUiState.Success -> {
                val users = state.users
                Column(modifier = Modifier.padding(vertical = 14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    Color(0xFF10B981).copy(alpha = 0.12f),
                                    RoundedCornerShape(10.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.People,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "آشناها در ویستا",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = VistaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                ),
                            )
                            Text(
                                text = "${users.size} نفر از مخاطبینت اینجا هستند",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VistaFontFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.vistaColors.contentSecondary,
                                ),
                            )
                        }
                        TextButton(onClick = onAllContactsClick) {
                            Text(
                                text = "همه",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = VistaFontFamily,
                                    color = VistaBrandColors.Indigo,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(users, key = { it.id }) { user ->
                            Column(
                                modifier = Modifier
                                    .width(66.dp)
                                    .clickable { onContactClick(user.id) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (user.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(
                                                VistaBrandColors.Indigo.copy(alpha = 0.12f),
                                                CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = user.fullName.firstOrNull()?.toString() ?: "?",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = VistaFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = VistaBrandColors.Indigo,
                                            ),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (user.username.isNotBlank()) "@${user.username}" else user.fullName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = VistaFontFamily,
                                        fontSize = 9.5.sp,
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
                    actionLabel = "مشاهده مخاطبین",
                    onAction = onAllContactsClick,
                )
            }
            is ContactsRailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = VistaBrandColors.Indigo,
                        modifier = Modifier.size(24.dp),
                    )
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
                    VistaBrandColors.Indigo.copy(alpha = 0.12f),
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            val imageVector = when (iconName.lowercase()) {
                "person_add" -> Icons.Rounded.PersonAdd
                "lock_open" -> Icons.Rounded.LockOpen
                "cloud_off" -> Icons.Rounded.CloudOff
                else -> Icons.Rounded.People
            }
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = VistaBrandColors.Indigo,
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
                    fontSize = 12.5.sp,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = VistaFontFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.vistaColors.contentSecondary,
                    lineHeight = 14.sp,
                ),
            )
        }
        TextButton(onClick = onAction) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = VistaFontFamily,
                    color = VistaBrandColors.Indigo,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun ManagedSectionCard(
    section: ServiceSection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = parseHexColor(section.color, VistaBrandColors.Indigo)
    val surface = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .width(208.dp)
            .height(96.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${section.title}، ${section.subtitle}"
            },
        shape = RoundedCornerShape(16.dp),
        color = surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = 0.22f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(accent.copy(alpha = 0.12f), surface),
                    ),
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = null,
                    tint = accent,
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
                        fontSize = 12.5.sp,
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
                            fontSize = 9.5.sp,
                            color = MaterialTheme.vistaColors.contentSecondary,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = null,
                tint = accent.copy(alpha = 0.8f),
                modifier = Modifier.size(12.dp),
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
    CompactStateBanner(
        iconName = "cloud_off",
        title = title,
        subtitle = subtitle,
        actionLabel = "تلاش دوباره",
        onAction = onRetry,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

private fun parseHexColor(hex: String, fallback: Color): Color = try {
    val clean = hex.removePrefix("#")
    val colorLong = clean.toLong(16)
    if (clean.length == 6) {
        Color((colorLong.toInt()) or 0xFF000000.toInt())
    } else {
        Color(colorLong.toInt())
    }
} catch (_: Exception) {
    fallback
}
