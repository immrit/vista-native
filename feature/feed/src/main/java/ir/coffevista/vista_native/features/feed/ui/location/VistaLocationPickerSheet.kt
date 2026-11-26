package ir.coffevista.vista_native.features.feed.ui.location

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.features.feed.data.LocationMatcher
import ir.coffevista.vista_native.features.stories.data.StoryDynamicContextService
import ir.coffevista.vista_native.features.stories.data.StoryPlaceSuggestion
import ir.coffevista.vista_native.features.stories.ui.render.LocalStoryDominantColors
import ir.coffevista.vista_native.features.stories.ui.render.LocationIcon
import ir.coffevista.vista_native.features.stories.ui.render.StoryPillWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaLocationPickerSheet(
    contextService: StoryDynamicContextService?,
    initialLocationName: String? = null,
    forceDarkTheme: Boolean = false,
    showStickerPreview: Boolean = false,
    initialStickerVariant: Int = 1,
    confirmButtonMode: Boolean = false,
    onSelect: (place: StoryPlaceSuggestion, variant: Int) -> Unit,
    onRemoveLocation: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dominant = LocalStoryDominantColors.current

    val isDark = forceDarkTheme || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val sheetBg = if (forceDarkTheme) {
        Color(0xFF12141E)
    } else if (isDark) {
        Color(0xFF141720)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val cardBg = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val titleColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.62f) else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val gpsAccentColor = Color(0xFF2AABEE)
    val primaryAccent = if (showStickerPreview) Color(0xFFFF4B55) else Color(0xFF4C6EF5)

    var previewVariant by remember { mutableIntStateOf(initialStickerVariant) }
    var searchQuery by remember { mutableStateOf("") }
    var hasLocationPerm by remember { mutableStateOf(contextService?.hasLocationPermission() == true) }
    var isLocatingGps by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var statusHint by remember { mutableStateOf<String?>(null) }

    var currentCoords by remember(contextService) {
        mutableStateOf(contextService?.cachedCoordinates())
    }
    var nearbyPlaces by remember(contextService) {
        mutableStateOf(contextService?.cachedSnapshotPlaces().orEmpty())
    }
    var searchResults by remember { mutableStateOf<List<StoryPlaceSuggestion>>(emptyList()) }
    var selectedPlace by remember(initialLocationName) {
        mutableStateOf(
            initialLocationName?.trim()?.takeIf { it.isNotBlank() }?.let {
                StoryPlaceSuggestion(name = it, category = "place")
            },
        )
    }

    fun triggerGpsLookup(forceFresh: Boolean = false) {
        val service = contextService ?: return
        coroutineScope.launch {
            if (forceFresh || nearbyPlaces.isEmpty()) {
                isLocatingGps = true
            }
            statusHint = null
            val coords = service.obtainDeviceCoordinates(forceFresh = forceFresh)
            if (coords != null) {
                currentCoords = coords
                var firstEmission = true
                service.resolveCurrentPlacesFlow(
                    lat = coords.first,
                    lon = coords.second,
                    forceRefresh = forceFresh,
                ).collect { resolved ->
                    nearbyPlaces = resolved
                    if (selectedPlace == null && searchQuery.isBlank()) {
                        selectedPlace = resolved.firstOrNull()
                    }
                    if (firstEmission) {
                        firstEmission = false
                        isLocatingGps = false
                    }
                }
                statusHint = null
            } else if (nearbyPlaces.isEmpty()) {
                statusHint = if (!service.isLocationServicesEnabled()) {
                    "مکان‌یاب (GPS) دستگاه خاموش است"
                } else {
                    "دریافت سیگنال GPS ناموفق بود"
                }
            }
            isLocatingGps = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            contextService?.hasLocationPermission() == true
        hasLocationPerm = granted
        if (granted) {
            triggerGpsLookup(forceFresh = true)
        } else {
            statusHint = "دسترسی به موقعیت مکانی داده نشد"
        }
    }

    LaunchedEffect(contextService) {
        if (contextService != null && hasLocationPerm) {
            triggerGpsLookup(forceFresh = false)
        }
    }

    LaunchedEffect(searchQuery, currentCoords, contextService) {
        val q = searchQuery.trim()
        val service = contextService
        if (q.length < 2 || service == null) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(300L)
        searchResults = service.searchPlaces(
            query = q,
            biasLat = currentCoords?.first,
            biasLon = currentCoords?.second,
        )
        isSearching = false
    }

    val displayedPlaces = remember(searchQuery, searchResults, nearbyPlaces) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            val customOption = StoryPlaceSuggestion(
                name = q,
                subtitle = "افزودن مکان دلخواه",
                category = "custom",
            )
            if (searchResults.any { LocationMatcher.normalize(it.name) == LocationMatcher.normalize(q) }) {
                searchResults
            } else {
                searchResults + customOption
            }
        } else if (nearbyPlaces.size >= 3) {
            nearbyPlaces
        } else if (nearbyPlaces.isNotEmpty()) {
            nearbyPlaces + StoryDynamicContextService.defaultPopularCities.filterNot { pop ->
                nearbyPlaces.any { LocationMatcher.normalize(it.name) == LocationMatcher.normalize(pop.name) }
            }
        } else {
            StoryDynamicContextService.defaultPopularCities
        }
    }

    val effectivePlace = selectedPlace
        ?: displayedPlaces.firstOrNull()
        ?: StoryPlaceSuggestion("تهران", "ایران", 35.6892, 51.3890, "city")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = titleColor,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .navigationBarsPadding()
                    .padding(top = 14.dp, start = 16.dp, end = 16.dp, bottom = 10.dp),
            ) {
                // Top Header Bar (Close, Title, Refresh)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "بستن",
                            tint = titleColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Text(
                        text = "انتخاب مکان",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .clickable(enabled = !isLocatingGps && contextService != null) {
                                if (hasLocationPerm) {
                                    triggerGpsLookup(forceFresh = true)
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                        ),
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLocatingGps) {
                            CircularProgressIndicator(
                                color = gpsAccentColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "به‌روزرسانی موقعیت",
                                tint = gpsAccentColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                // Story Live Sticker Preview OR Tagging Explanation Subtitle
                if (showStickerPreview) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        dominant.topColor.copy(alpha = 0.85f),
                                        dominant.bottomColor.copy(alpha = 0.85f),
                                    ),
                                ),
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                            .clickable { previewVariant = (previewVariant + 1) % 4 }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        StoryPillWidget(
                            text = effectivePlace.name,
                            variant = previewVariant,
                            icon = LocationIcon,
                            categoryAccent = Color(0xFFFF4B55),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "برای تغییر تم شیشه‌ای روی پیش‌نمایش بزنید",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                } else {
                    Text(
                        text = "افرادی که این محتوا را می‌بینند می‌توانند مکان برچسب‌شده و پست‌های مرتبط را مشاهده کنند.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = subtitleColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    )
                }

                // Search Pill Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBg)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = subtitleColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "جستجوی مکان‌ها...",
                                fontSize = 14.sp,
                                color = subtitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = titleColor,
                                fontSize = 14.sp,
                            ),
                            cursorBrush = SolidColor(gpsAccentColor),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { keyboardController?.hide() },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = gpsAccentColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "پاک‌کردن",
                            tint = subtitleColor,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Highlighted "My Current Location" Row (Telegram / Threads style)
                if (contextService != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isLocatingGps) {
                                when {
                                    !hasLocationPerm -> {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                            ),
                                        )
                                    }
                                    nearbyPlaces.isNotEmpty() -> {
                                        val firstPlace = nearbyPlaces.first()
                                        if (confirmButtonMode) {
                                            selectedPlace = firstPlace
                                        } else {
                                            onSelect(firstPlace, previewVariant)
                                        }
                                    }
                                    else -> triggerGpsLookup(forceFresh = true)
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(gpsAccentColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isLocatingGps) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.2.dp,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(23.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "موقعیت فعلی من",
                                color = gpsAccentColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            val gpsSubtitle = when {
                                isLocatingGps -> "در حال دریافت موقعیت GPS…"
                                !hasLocationPerm -> "دسترسی به GPS"
                                nearbyPlaces.isNotEmpty() -> "${nearbyPlaces.first().name} • GPS"
                                !statusHint.isNullOrBlank() -> statusHint.orEmpty()
                                else -> "دسترسی به GPS"
                            }
                            Text(
                                text = gpsSubtitle,
                                color = if (!statusHint.isNullOrBlank() && nearbyPlaces.isEmpty() && !isLocatingGps) {
                                    Color(0xFFF59E0B)
                                } else {
                                    subtitleColor
                                },
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = null,
                            tint = subtitleColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                }

                // Remove Existing Location Option
                if (!initialLocationName.isNullOrBlank() && onRemoveLocation != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRemoveLocation() }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "حذف موقعیت فعلی ($initialLocationName)",
                            color = Color(0xFFEF4444),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                }

                // Section Title
                val sectionLabel = when {
                    searchQuery.trim().length >= 2 -> "نتایج جستجو"
                    nearbyPlaces.isNotEmpty() -> "مکان‌های نزدیک شما"
                    else -> "مکان‌های پیشنهادی"
                }
                Text(
                    text = sectionLabel,
                    color = subtitleColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp),
                )

                // Places List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    if (isLocatingGps && nearbyPlaces.isEmpty() && searchQuery.isBlank()) {
                        item(key = "gps-loading-row") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CircularProgressIndicator(
                                    color = gpsAccentColor,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "در حال جستجوی مکان‌ها و اماکن نزدیک…",
                                    color = subtitleColor,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = displayedPlaces,
                        key = { index, item -> "${index}_${item.category}_${item.name}_${item.latitude}_${item.longitude}" },
                    ) { index, place ->
                        val isSelected = if (confirmButtonMode) {
                            LocationMatcher.normalize(effectivePlace.name) == LocationMatcher.normalize(place.name) &&
                                effectivePlace.latitude == place.latitude
                        } else {
                            !initialLocationName.isNullOrBlank() &&
                                LocationMatcher.normalize(initialLocationName) == LocationMatcher.normalize(place.name)
                        }

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (confirmButtonMode) {
                                            selectedPlace = place
                                        } else {
                                            onSelect(place, previewVariant)
                                        }
                                    }
                                    .padding(vertical = 11.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val icon = when (place.category) {
                                    "neighbourhood", "gps" -> Icons.Default.MyLocation
                                    "city" -> Icons.Default.LocationCity
                                    "custom" -> Icons.Default.AddLocationAlt
                                    else -> Icons.Rounded.LocationOn
                                }
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) primaryAccent.copy(alpha = 0.16f) else cardBg,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) primaryAccent else subtitleColor,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = place.name,
                                        color = if (isSelected) primaryAccent else titleColor,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val sub = place.formattedSubtitle()
                                    if (sub.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = sub,
                                            color = subtitleColor,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "انتخاب‌شده",
                                        tint = primaryAccent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronLeft,
                                        contentDescription = null,
                                        tint = subtitleColor.copy(alpha = 0.55f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            if (index < displayedPlaces.lastIndex) {
                                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                            }
                        }
                    }
                }

                // Bottom Confirm + Map Preview Actions (for Story mode)
                if (confirmButtonMode) {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { onSelect(effectivePlace, previewVariant) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryAccent,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 13.dp),
                    ) {
                        Text(
                            text = "افزودن موقعیت مکانی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                        )
                    }
                    TextButton(
                        onClick = {
                            val lat = effectivePlace.latitude
                            val lon = effectivePlace.longitude
                            val geoUri = if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
                                Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(effectivePlace.name)})")
                            } else {
                                Uri.parse("geo:0,0?q=${Uri.encode(effectivePlace.name)}")
                            }
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, geoUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text = "مشاهده روی نقشه",
                            color = gpsAccentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
