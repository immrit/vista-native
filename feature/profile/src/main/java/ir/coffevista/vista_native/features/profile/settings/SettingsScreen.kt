package ir.coffevista.vista_native.features.profile.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import ir.coffevista.vista_native.features.profile.settings.appearance.AppearanceSettingsViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import ir.coffevista.vista_native.features.profile.settings.components.VistaChoiceOption
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsChoiceBottomSheet
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsDivider
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsGroup
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsTile

private data class SettingsCopy(
    val settings: String,
    val account: String,
    val privacySecurity: String,
    val notifications: String,
    val language: String,
    val appearance: String,
    val dataStorage: String,
    val savedItems: String,
    val requestBlueTick: String,
    val changePassword: String,
    val termsAndConditions: String,
    val aboutVista: String,
    val logout: String,
    val logoutDialog: String,
    val selectLanguage: String,
    val cancel: String,
)

/** Settings-root strings copied from Flutter's app_fa/en/ar.arb catalogues. */
private fun settingsCopy(locale: String): SettingsCopy = when (locale) {
    "en" -> SettingsCopy(
        settings = "Settings", account = "Account", privacySecurity = "Privacy & Security",
        notifications = "Notifications", language = "Language", appearance = "Appearance",
        dataStorage = "Data & Storage", savedItems = "Saved Items", requestBlueTick = "Request Blue Tick",
        changePassword = "Change Password", termsAndConditions = "Terms & Conditions",
        aboutVista = "About Vista", logout = "Log Out", logoutDialog = "Are you sure you want to log out?",
        selectLanguage = "Select Language", cancel = "Cancel",
    )
    "ar" -> SettingsCopy(
        settings = "الإعدادات", account = "الحساب", privacySecurity = "الخصوصية والأمان",
        notifications = "الإشعارات", language = "اللغة", appearance = "المظهر",
        dataStorage = "البيانات والتخزين", savedItems = "العناصر المحفوظة", requestBlueTick = "طلب العلامة الزرقاء",
        changePassword = "تغيير كلمة المرور", termsAndConditions = "الشروط والأحكام",
        aboutVista = "حول فيستا", logout = "تسجيل الخروج", logoutDialog = "هل أنت متأكد أنك تريد تسجيل الخروج؟",
        selectLanguage = "اختر اللغة", cancel = "إلغاء",
    )
    else -> SettingsCopy(
        settings = "تنظیمات", account = "حساب کاربری", privacySecurity = "حریم خصوصی و امنیت",
        notifications = "اعلان‌ها", language = "زبان", appearance = "ظاهر",
        dataStorage = "داده و ذخیره‌سازی", savedItems = "ذخیره‌شده‌ها", requestBlueTick = "درخواست تیک آبی",
        changePassword = "تغییر گذرواژه", termsAndConditions = "قوانین و مقررات",
        aboutVista = "درباره ویستا", logout = "خروج از حساب", logoutDialog = "آیا برای خروج اطمینان دارید؟",
        selectLanguage = "انتخاب زبان", cancel = "لغو",
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: OwnProfileEntity?,
    onBack: () -> Unit,
    onEditProfileClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onAccountDetailsClick: () -> Unit,
    onPrivacySecurityClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onDataStorageClick: () -> Unit,
    onSavedPostsClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onVerificationRequestClick: () -> Unit,
    onTermsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLogout: () -> Unit,
    appearanceViewModel: AppearanceSettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    val appearancePreferences by appearanceViewModel.preferences.collectAsStateWithLifecycle()
    val currentLanguage = appearancePreferences.locale
    val settingsLayoutDirection = if (currentLanguage == "en") LayoutDirection.Ltr else LayoutDirection.Rtl
    val copy = settingsCopy(currentLanguage)
    val context = LocalContext.current
    val installedVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "—"
    }

    CompositionLocalProvider(LocalLayoutDirection provides settingsLayoutDirection) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = copy.settings,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // 1. Profile Card
                SettingsProfileCard(
                    profile = profile,
                    onClick = onEditProfileClick,
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Premium Status Card
                SettingsPremiumCard(
                    isPremium = profile?.subscriptionPlan == "premium",
                    daysRemaining = profile?.premiumDaysRemaining ?: 0,
                    onClick = onPremiumClick,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Settings Group 1 (Account & Preferences)
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.Person,
                        title = copy.account,
                        // Flutter opens the edit-profile route from this row.
                        // Keep the native flow identical instead of redirecting
                        // users to their public profile.
                        onClick = onEditProfileClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Security,
                        title = copy.privacySecurity,
                        onClick = onPrivacySecurityClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Notifications,
                        title = copy.notifications,
                        onClick = onNotificationsClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Language,
                        title = copy.language,
                        onClick = { showLanguageSheet = true },
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Palette,
                        title = copy.appearance,
                        onClick = onAppearanceClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Storage,
                        title = copy.dataStorage,
                        onClick = onDataStorageClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.BookmarkBorder,
                        title = copy.savedItems,
                        onClick = onSavedPostsClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Lock,
                        title = copy.changePassword,
                        onClick = onChangePasswordClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Verified,
                        title = copy.requestBlueTick,
                        onClick = onVerificationRequestClick,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Settings Group 2 (Legal & Info)
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.Description,
                        title = copy.termsAndConditions,
                        onClick = onTermsClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Info,
                        title = copy.aboutVista,
                        onClick = onAboutClick,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Settings Group 3 (Logout)
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.Logout,
                        iconColor = Color(0xFFE53935),
                        iconContainerColor = Color(0xFFE53935).copy(alpha = 0.1f),
                        title = copy.logout,
                        titleColor = Color(0xFFE53935),
                        showChevron = false,
                        onClick = { showLogoutDialog = true },
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 6. Version Footer
                Text(
                    text = "نسخه $installedVersion",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                )
            }
        }

        // Language BottomSheet
        if (showLanguageSheet) {
            VistaSettingsChoiceBottomSheet(
                title = copy.selectLanguage,
                selectedValue = currentLanguage,
                options = listOf(
                    VistaChoiceOption("fa", "فارسی", "زبان پیش‌فرض"),
                    VistaChoiceOption("en", "English", "English language"),
                    VistaChoiceOption("ar", "العربية", "اللغة العربية"),
                ),
                onOptionSelected = appearanceViewModel::setLocale,
                onDismissRequest = { showLanguageSheet = false },
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = copy.logout,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )
                },
                text = {
                    Text(
                        text = copy.logoutDialog,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                    ) {
                        Text(
                            text = copy.logout,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(
                            text = copy.cancel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

/**
 * User Profile Card at Top of Settings
 */
@Composable
private fun SettingsProfileCard(
    profile: OwnProfileEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar at start (Right in RTL)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (!profile?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.avatarUrl,
                        contentDescription = "تصویر پروفایل",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = profile?.fullName?.ifBlank { profile.username } ?: (profile?.username ?: "کاربر ویستا"),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    // OwnProfileEntity intentionally does not persist email. Never render a
                    // borrowed or fixture identity here; the server-backed username is the
                    // available profile identifier for this screen.
                    text = profile?.username?.takeIf { it.isNotBlank() }?.let { "\u200E@$it" } ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    ),
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Premium Status Card matching Flutter Gradient Card
 */
@Composable
private fun SettingsPremiumCard(
    isPremium: Boolean,
    daysRemaining: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF7E57C2).copy(alpha = 0.85f),
            Color(0xFF9575CD).copy(alpha = 0.75f),
            Color(0xFFB39DDB).copy(alpha = 0.65f),
        ),
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEDE7F6),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Badge at start (Right in RTL)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = "ویستا پریمیوم فعال",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "اشتراک منقضی شده — برای ادامه تمدید کنید",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.85f),
                        ),
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
