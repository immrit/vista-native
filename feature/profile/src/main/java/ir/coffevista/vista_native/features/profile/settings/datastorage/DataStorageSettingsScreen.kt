package ir.coffevista.vista_native.features.profile.settings.datastorage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.profile.settings.appearance.AppearanceSettingsViewModel
import ir.coffevista.vista_native.features.profile.settings.components.*
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
    val autoplayPostMusic = preferences.autoplayPostMusic
    val videoDataSaver = preferences.videoDataSaver
    val uploadQuality = preferences.mediaQuality
    val autoDownloadWifi = preferences.autoDownloadWifi
    val autoDownloadCellular = preferences.autoDownloadCellular
    val autoDownloadRoaming = preferences.autoDownloadRoaming
    val keepMediaPeriod = preferences.keepMediaPeriod

    val cacheState by cacheViewModel.uiState.collectAsStateWithLifecycle()
    val totalBytes = cacheState.breakdown.totalBytes
    val cacheSizeText = formatCacheSize(totalBytes)

    var showQualitySheet by remember { mutableStateOf(false) }
    var showKeepMediaSheet by remember { mutableStateOf(false) }
    var showClearCacheSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    cacheState.message?.let { message ->
        LaunchedEffect(message) { snackbarHostState.showSnackbar(message) }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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

                // بخش ۱: مصرف حافظه و پاکسازی کش با تفکیک بصری شبیه تلگرام
                VistaSettingsSection(title = "میزان مصرف حافظه و کش")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "حافظه موقت اشغال‌شده",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = cacheSizeText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = VistaBrandColors.Indigo,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // نوار تفکیک رنگی شبیه تلگرام
                        StorageBreakdownBar(breakdown = cacheState.breakdown)

                        Spacer(modifier = Modifier.height(14.dp))

                        // دسته‌بندی‌های تفکیک‌شده
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            CategoryLegendItem("تصاویر", formatCacheSize(cacheState.breakdown.imageBytes), Color(0xFF29B6F6))
                            CategoryLegendItem("ویدیوها", formatCacheSize(cacheState.breakdown.videoBytes), Color(0xFFAB47BC))
                            CategoryLegendItem("صدا و موزیک", formatCacheSize(cacheState.breakdown.audioBytes), Color(0xFFFFA726))
                            CategoryLegendItem("سایر فایل‌ها", formatCacheSize(cacheState.breakdown.otherBytes), Color(0xFF78909C))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showClearCacheSheet = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !cacheState.isClearing && totalBytes > 0L,
                        ) {
                            Icon(Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (cacheState.isClearing) "در حال پاکسازی..." else "پاکسازی حافظه موقت (کش)", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // مدت نگهداری رسانه‌ها
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.Timer,
                        title = "مدت نگهداری رسانه‌ها",
                        subtitle = "حذف خودکار فایل‌های موقت کش‌نشده پس از این دوره",
                        trailing = {
                            Text(
                                text = when (keepMediaPeriod) {
                                    "3_days" -> "۳ روز"
                                    "1_week" -> "۱ هفته"
                                    "1_month" -> "۱ ماه"
                                    else -> "نامحدود"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        },
                        onClick = { showKeepMediaSheet = true },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // بخش ۲: دانلود خودکار رسانه‌ها در پیام‌رسان (استاندارد تلگرام)
                VistaSettingsSection(title = "دانلود خودکار رسانه‌ها در پیام‌رسان")
                VistaSettingsGroup {
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.Wifi,
                        title = "هنگام اتصال به وای‌فای (Wi-Fi)",
                        subtitle = "دانلود خودکار تمامی تصاویر، ویدیوها و فایل‌ها",
                        value = autoDownloadWifi,
                        onChanged = viewModel::setAutoDownloadWifi,
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.NetworkCell,
                        title = "هنگام استفاده از داده همراه (سیم‌کارت)",
                        subtitle = "دانلود خودکار تصاویر در شبکه اینترنت همراه",
                        value = autoDownloadCellular,
                        onChanged = viewModel::setAutoDownloadCellular,
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.Public,
                        title = "هنگام رومینگ بین‌المللی",
                        subtitle = "غیرفعال جهت پیشگیری از هزینه‌های اضافی اپراتور",
                        value = autoDownloadRoaming,
                        onChanged = viewModel::setAutoDownloadRoaming,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // بخش ۳: پخش خودکار رسانه‌ها در فید
                VistaSettingsSection(title = "پخش خودکار رسانه‌ها در فید")
                VistaSettingsGroup {
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.PlayCircleOutline,
                        title = "پخش خودکار ویدیوها",
                        subtitle = "ویدیوهای فید هنگام اسکرول به‌صورت خودکار پخش شوند",
                        value = autoPlayVideo,
                        onChanged = viewModel::setAutoplayMedia,
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.MusicNote,
                        title = "پخش خودکار موسیقی پست‌ها",
                        subtitle = "پخش خودکار موزیک پس‌زمینه هنگام رسیدن به پست در فید (صرفه‌جویی در اینترنت)",
                        value = autoplayPostMusic,
                        onChanged = viewModel::setAutoplayPostMusic,
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.DataSaverOn,
                        title = "صرفه‌جویی در اینترنت (ویدیوها)",
                        subtitle = "دریافت پیش‌نمایش بهینه و کیفیت سبک‌تر روی شبکه همراه",
                        value = videoDataSaver,
                        onChanged = viewModel::setVideoDataSaver,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // بخش ۴: کیفیت آپلود و ارسال رسانه
                VistaSettingsSection(title = "کیفیت آپلود و ارسال رسانه")
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.HighQuality,
                        title = "کیفیت آپلود تصاویر و ویدیوها",
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

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // باتم‌شیت انتخاب کیفیت آپلود
        if (showQualitySheet) {
            VistaSettingsChoiceBottomSheet(
                title = "کیفیت آپلود رسانه",
                selectedValue = uploadQuality,
                options = listOf(
                    VistaChoiceOption("high", "کیفیت بالا", "وضوح اصلی با کمترین میزان فشرده‌سازی"),
                    VistaChoiceOption("standard", "استاندارد", "تعادل مطلوب بین شفافیت تصویر و حجم فایل"),
                    VistaChoiceOption("data_saver", "صرفه‌جویی در داده", "فشرده‌سازی بیشتر جهت حداقل مصرف ترافیک اینترنت"),
                ),
                onOptionSelected = viewModel::setMediaQuality,
                onDismissRequest = { showQualitySheet = false },
            )
        }

        // باتم‌شیت مدت نگهداری رسانه‌ها
        if (showKeepMediaSheet) {
            VistaSettingsChoiceBottomSheet(
                title = "مدت نگهداری رسانه‌ها در حافظه",
                selectedValue = keepMediaPeriod,
                options = listOf(
                    VistaChoiceOption("3_days", "۳ روز", "فایل‌های قدیمی پس از ۳ روز به‌صورت خودکار پاک می‌شوند"),
                    VistaChoiceOption("1_week", "۱ هفته", "فایل‌های قدیمی پس از ۷ روز پاک می‌شوند"),
                    VistaChoiceOption("1_month", "۱ ماه", "فایل‌های قدیمی پس از یک ماه پاک می‌شوند"),
                    VistaChoiceOption("forever", "نامحدود", "رسانه‌ها تا زمان پاکسازی دستی در حافظه می‌مانند"),
                ),
                onOptionSelected = viewModel::setKeepMediaPeriod,
                onDismissRequest = { showKeepMediaSheet = false },
            )
        }

        // باتم‌شیت پاکسازی کش با امکان انتخاب دسته‌ها
        if (showClearCacheSheet) {
            ClearCacheBottomSheet(
                breakdown = cacheState.breakdown,
                onDismiss = { showClearCacheSheet = false },
                onConfirmClear = { categories ->
                    showClearCacheSheet = false
                    cacheViewModel.clear(categories)
                },
            )
        }
    }
}

@Composable
private fun StorageBreakdownBar(breakdown: CacheBreakdown) {
    val total = breakdown.totalBytes.toFloat().coerceAtLeast(1f)
    val imgWeight = (breakdown.imageBytes / total).coerceIn(0.02f, 1f)
    val vidWeight = (breakdown.videoBytes / total).coerceIn(0.02f, 1f)
    val audWeight = (breakdown.audioBytes / total).coerceIn(0.02f, 1f)
    val othWeight = (breakdown.otherBytes / total).coerceIn(0.02f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (breakdown.imageBytes > 0) {
            Box(Modifier.weight(imgWeight).fillMaxHeight().background(Color(0xFF29B6F6)))
        }
        if (breakdown.videoBytes > 0) {
            Box(Modifier.weight(vidWeight).fillMaxHeight().background(Color(0xFFAB47BC)))
        }
        if (breakdown.audioBytes > 0) {
            Box(Modifier.weight(audWeight).fillMaxHeight().background(Color(0xFFFFA726)))
        }
        if (breakdown.otherBytes > 0) {
            Box(Modifier.weight(othWeight).fillMaxHeight().background(Color(0xFF78909C)))
        }
        if (breakdown.totalBytes == 0L) {
            Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
        }
    }
}

@Composable
private fun CategoryLegendItem(label: String, sizeText: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(4.dp))
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(2.dp))
        Text(text = sizeText, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClearCacheBottomSheet(
    breakdown: CacheBreakdown,
    onDismiss: () -> Unit,
    onConfirmClear: (Set<CacheCategory>) -> Unit,
) {
    var clearImages by remember { mutableStateOf(breakdown.imageBytes > 0) }
    var clearVideos by remember { mutableStateOf(breakdown.videoBytes > 0) }
    var clearAudio by remember { mutableStateOf(breakdown.audioBytes > 0) }
    var clearOther by remember { mutableStateOf(breakdown.otherBytes > 0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "پاکسازی حافظه موقت (کش)",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "دسته‌هایی که مایل به پاکسازی فایل‌های موقت آنها هستید را انتخاب کنید. اطلاعات حساب و پیام‌های شما حذف نخواهند شد.",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(16.dp))

            CacheCategoryCheckboxRow("تصاویر و پیش‌نمایش‌ها", formatCacheSize(breakdown.imageBytes), clearImages) { clearImages = it }
            CacheCategoryCheckboxRow("ویدیوهای ذخیره‌شده موقت", formatCacheSize(breakdown.videoBytes), clearVideos) { clearVideos = it }
            CacheCategoryCheckboxRow("فایل‌های صوتی و موزیک‌ها", formatCacheSize(breakdown.audioBytes), clearAudio) { clearAudio = it }
            CacheCategoryCheckboxRow("اسناد و سایر فایل‌ها", formatCacheSize(breakdown.otherBytes), clearOther) { clearOther = it }

            Spacer(Modifier.height(20.dp))

            val selectedCategories = buildSet {
                if (clearImages) add(CacheCategory.IMAGES)
                if (clearVideos) add(CacheCategory.VIDEOS)
                if (clearAudio) add(CacheCategory.AUDIO)
                if (clearOther) add(CacheCategory.DOCUMENTS_OTHER)
            }

            Button(
                onClick = { onConfirmClear(selectedCategories) },
                enabled = selectedCategories.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("پاکسازی موارد انتخاب‌شده", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CacheCategoryCheckboxRow(
    title: String,
    sizeText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Spacer(Modifier.width(8.dp))
            Text(text = title, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(text = sizeText, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

private fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0) return "۰ بایت"
    val units = listOf("بایت", "کیلوبایت", "مگابایت", "گیگابایت")
    val index = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(index)
    val formatted = if (index == 0) "$bytes ${units[index]}" else "%.1f %s".format(value, units[index])
    return formatted.toPersianDigits()
}
