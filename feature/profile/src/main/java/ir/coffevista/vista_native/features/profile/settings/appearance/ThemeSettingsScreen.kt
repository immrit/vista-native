package ir.coffevista.vista_native.features.profile.settings.appearance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.features.profile.settings.components.VistaChoiceOption
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsChoiceBottomSheet
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsDivider
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsGroup
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSection
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSwitch
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    viewModel: AppearanceSettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val themeMode = preferences.themeMode
    val reduceMotion = preferences.reduceMotion
    val chatEntryMode = preferences.chatEntryMode
    val emojiStyle = preferences.emojiStyle

    var activeSheet by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LocalLayoutDirection.current) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "ظاهر",
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
                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Theme Mode
                VistaSettingsSection(title = "پوسته")
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.Palette,
                        title = "حالت نمایش",
                        trailing = {
                            Text(
                                text = when (themeMode) {
                                    "system" -> "پیروی از سیستم"
                                    "light" -> "روشن"
                                    else -> "تاریک"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                ),
                            )
                        },
                        onClick = { activeSheet = "themeMode" },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: User Experience
                VistaSettingsSection(title = "تجربه کاربری")
                VistaSettingsGroup {
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.AccessibilityNew,
                        title = "کاهش حرکت",
                        subtitle = "کم کردن انیمیشن‌ها برای راحتی چشم",
                        value = reduceMotion,
                        onChanged = viewModel::setReduceMotion,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "انیمیشن ورود پیام",
                        trailing = {
                            Text(
                                text = when (chatEntryMode) {
                                    "adaptive" -> "تطبیقی"
                                    "minimal" -> "کم"
                                    else -> "خاموش"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                ),
                            )
                        },
                        onClick = { activeSheet = "chatEntry" },
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Mood,
                        title = "استایل ایموجی",
                        trailing = {
                            Text(
                                text = if (emojiStyle == "custom") "اختصاصی" else "پیش‌فرض سیستم",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                ),
                            )
                        },
                        onClick = { activeSheet = "emojiStyle" },
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "بهینه‌سازی‌های فنی مثل GPU، تاری و تنظیمات عملکرد در پس‌زمینه و به‌صورت خودکار انجام می‌شوند.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        lineHeight = 18.sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Bottom Sheets
        when (activeSheet) {
            "themeMode" -> {
                VistaSettingsChoiceBottomSheet(
                    title = "حالت نمایش",
                    selectedValue = themeMode,
                    options = listOf(
                        VistaChoiceOption("system", "پیروی از سیستم"),
                        VistaChoiceOption("light", "روشن"),
                        VistaChoiceOption("dark", "تاریک"),
                    ),
                    onOptionSelected = viewModel::setThemeMode,
                    onDismissRequest = { activeSheet = null },
                )
            }
            "chatEntry" -> {
                VistaSettingsChoiceBottomSheet(
                    title = "انیمیشن ورود پیام",
                    selectedValue = chatEntryMode,
                    options = listOf(
                        VistaChoiceOption("adaptive", "تطبیقی", "انیمیشن نرم و هوشمند"),
                        VistaChoiceOption("minimal", "کم", "انیمیشن کوتاه"),
                        VistaChoiceOption("off", "خاموش", "بدون انیمیشن"),
                    ),
                    onOptionSelected = viewModel::setChatEntryMode,
                    onDismissRequest = { activeSheet = null },
                )
            }
            "emojiStyle" -> {
                VistaSettingsChoiceBottomSheet(
                    title = "استایل ایموجی",
                    selectedValue = emojiStyle,
                    options = listOf(
                        VistaChoiceOption("custom", "اختصاصی", "ایموجی‌های زیبای ویستا"),
                        VistaChoiceOption("system", "سیستمی", "ایموجی‌های دستگاه"),
                    ),
                    onOptionSelected = viewModel::setEmojiStyle,
                    onDismissRequest = { activeSheet = null },
                )
            }
        }
    }
}
