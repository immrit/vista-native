package ir.coffevista.vista_native.features.profile.settings.datastorage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DataSaverOn
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import ir.coffevista.vista_native.features.profile.settings.appearance.AppearanceSettingsViewModel
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
import kotlin.math.ln
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStorageSettingsScreen(
    onBack: () -> Unit,
    viewModel: AppearanceSettingsViewModel = hiltViewModel(),
    cacheViewModel: StorageCacheViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val autoPlayVideo = preferences.autoplayMedia
    val videoDataSaver = preferences.videoDataSaver
    val uploadQuality = preferences.mediaQuality
    val cacheState by cacheViewModel.uiState.collectAsStateWithLifecycle()
    val cacheSizeText = cacheState.sizeBytes?.let(::formatCacheSize) ?: "در حال محاسبه"

    var showQualitySheet by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    cacheState.message?.let { message ->
        LaunchedEffect(message) { snackbarHostState.showSnackbar(message) }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LocalLayoutDirection.current) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "داده و ذخیره‌سازی",
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

                // Section 1: Video Autoplay
                VistaSettingsSection(title = "پخش ویدیو در فید")
                VistaSettingsGroup {
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.PlayCircleOutline,
                        title = "پخش خودکار ویدیو",
                        subtitle = "ویدیوهای فید و پروفایل با اسکرول به‌صورت خودکار پخش شوند",
                        value = autoPlayVideo,
                        onChanged = viewModel::setAutoplayMedia,
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.DataSaverOn,
                        title = "صرفه‌جویی در اینترنت (ویدیو)",
                        subtitle = "کیفیت پایین‌تر برای ویدیوها و پیش‌نمایش سبک‌تر",
                        value = videoDataSaver,
                        onChanged = viewModel::setVideoDataSaver,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Media Quality
                VistaSettingsSection(title = "کیفیت رسانه")
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.HighQuality,
                        title = "کیفیت آپلود",
                        trailing = {
                            Text(
                                text = when (uploadQuality) {
                                    "high" -> "کیفیت بالا"
                                    "standard" -> "استاندارد"
                                    else -> "صرفه‌جویی در داده"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                ),
                            )
                        },
                        onClick = { showQualitySheet = true },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Storage & Cache
                VistaSettingsSection(title = "ذخیره‌سازی")
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.CleaningServices,
                        title = "پاکسازی کش",
                        subtitle = "حذف فایل‌های موقت و آزادسازی فضا ($cacheSizeText)",
                        enabled = cacheState.sizeBytes != null && !cacheState.isClearing,
                        onClick = { showClearCacheDialog = true },
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showQualitySheet) {
            VistaSettingsChoiceBottomSheet(
                title = "کیفیت آپلود",
                selectedValue = uploadQuality,
                options = listOf(
                    VistaChoiceOption("high", "کیفیت بالا", "بهترین کیفیت، مصرف داده بیشتر"),
                    VistaChoiceOption("standard", "استاندارد", "تعادل بین کیفیت و مصرف داده"),
                    VistaChoiceOption("data_saver", "صرفه‌جویی در داده", "کمترین مصرف داده"),
                ),
                onOptionSelected = viewModel::setMediaQuality,
                onDismissRequest = { showQualitySheet = false },
            )
        }

        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = { Text("پاکسازی حافظه موقت (کش)") },
                text = { Text("آیا مایل به حذف $cacheSizeText حافظه موقت برنامه هستید؟ این کار اطلاعات حساب شما را پاک نمی‌کند.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearCacheDialog = false
                            cacheViewModel.clear()
                        },
                    ) {
                        Text("پاکسازی", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) {
                        Text("انصراف")
                    }
                },
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

private fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0) return "۰ بایت"
    val units = listOf("بایت", "کیلوبایت", "مگابایت", "گیگابایت")
    val index = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(index)
    return if (index == 0) "$bytes ${units[index]}" else "%.1f %s".format(value, units[index])
}
