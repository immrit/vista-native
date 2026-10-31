package ir.coffevista.vista_native.features.stories.ui.editor

import android.Manifest
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.data.StoryDynamicContextService
import ir.coffevista.vista_native.features.stories.data.StoryLiveWeather
import ir.coffevista.vista_native.features.stories.data.StoryPlaceSuggestion
import ir.coffevista.vista_native.features.stories.domain.StoryUser
import ir.coffevista.vista_native.features.stories.ui.render.LocalStoryDominantColors
import ir.coffevista.vista_native.features.stories.ui.render.LocationIcon
import ir.coffevista.vista_native.features.stories.ui.render.StoryPillWidget
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DynamicLocationPickerBottomSheet(
    contextService: StoryDynamicContextService,
    initialVariant: Int = 1,
    onConfirm: (place: StoryPlaceSuggestion, variant: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dominant = LocalStoryDominantColors.current

    var previewVariant by remember { mutableIntStateOf(initialVariant) }
    var searchQuery by remember { mutableStateOf("") }
    var hasLocationPerm by remember { mutableStateOf(contextService.hasLocationPermission()) }
    var isLocatingGps by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var statusHint by remember { mutableStateOf<String?>(null) }

    var nearbyPlaces by remember { mutableStateOf<List<StoryPlaceSuggestion>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<StoryPlaceSuggestion>>(emptyList()) }
    var selectedPlace by remember {
        mutableStateOf<StoryPlaceSuggestion?>(null)
    }

    fun triggerGpsLookup() {
        coroutineScope.launch {
            isLocatingGps = true
            statusHint = "در حال دریافت موقعیت مکانی دقیق شما…"
            val coords = contextService.obtainDeviceCoordinates()
            if (coords != null) {
                val (lat, lon) = coords
                val resolved = contextService.resolveCurrentPlaces(lat, lon)
                nearbyPlaces = resolved
                if (selectedPlace == null || searchQuery.isBlank()) {
                    selectedPlace = resolved.firstOrNull()
                }
                statusHint = null
            } else {
                statusHint = if (!contextService.isLocationServicesEnabled()) {
                    "مکان‌یاب (GPS) گوشی خاموش است؛ آن را روشن کنید یا نام مکان را جستجو نمایید"
                } else {
                    "سیگنال GPS دریافت نشد؛ می‌توانید نام شهر یا محله را جستجو کنید"
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
            contextService.hasLocationPermission()
        hasLocationPerm = granted
        if (granted) {
            triggerGpsLookup()
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPerm) {
            triggerGpsLookup()
        }
    }

    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length < 2) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(320L)
        searchResults = contextService.searchPlaces(q)
        isSearching = false
    }

    val displayedPlaces = remember(searchQuery, searchResults, nearbyPlaces) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            val customOption = StoryPlaceSuggestion(
                name = q,
                subtitle = "برچسب مکان دلخواه",
                category = "custom",
            )
            if (searchResults.any { it.name.equals(q, ignoreCase = true) }) {
                searchResults
            } else {
                searchResults + customOption
            }
        } else if (nearbyPlaces.isNotEmpty()) {
            nearbyPlaces + StoryDynamicContextService.defaultPopularCities.filterNot { pop ->
                nearbyPlaces.any { it.name == pop.name }
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
        containerColor = Color(0xF5131520),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 440.dp, max = 640.dp)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF4B55).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFFF4B55),
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "موقعیت مکانی هوشمند",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Text(
                                text = "دریافت خودکار از GPS یا جستجوی زنده مکان‌ها",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White.copy(alpha = 0.75f))
                    }
                }

                // Live Preview Stage (Tap to cycle 4 Telegram styles)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
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
                        .padding(horizontal = 12.dp, vertical = 14.dp),
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
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }

                // GPS Auto-Detect Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFFF4B55).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFF4B55).copy(alpha = 0.38f), RoundedCornerShape(15.dp))
                        .clickable(enabled = !isLocatingGps) {
                            if (hasLocationPerm) {
                                triggerGpsLookup()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (isLocatingGps) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF4B55),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = Color(0xFFFF4B55),
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        Text(
                            text = when {
                                isLocatingGps -> "در حال یافتن موقعیت دقیق شما…"
                                !hasLocationPerm -> "دریافت خودکار موقعیت فعلی من (GPS)"
                                nearbyPlaces.isNotEmpty() -> "به‌روزرسانی موقعیت فعلی (${nearbyPlaces.first().name})"
                                else -> "مکان‌یابی مجدد موقعیت فعلی من"
                            },
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(17.dp),
                    )
                }

                if (!statusHint.isNullOrBlank()) {
                    Text(
                        text = statusHint.orEmpty(),
                        color = Color(0xFFFFCA28),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                // Live Place Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "جستجوی شهر، محله، خیابان یا مکان…",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.45f),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(19.dp),
                        )
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                color = Color(0xFFFF4B55),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(17.dp),
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "پاک‌کردن",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = Color(0xFFFF4B55),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                )

                // Places List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 10.dp),
                ) {
                    items(
                        items = displayedPlaces,
                        key = { "${it.category}_${it.name}_${it.latitude}_${it.longitude}" },
                    ) { place ->
                        val isSelected = effectivePlace.name == place.name &&
                            effectivePlace.latitude == place.latitude
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) Color(0xFFFF4B55).copy(alpha = 0.18f)
                                    else Color.White.copy(alpha = 0.05f),
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFFF4B55).copy(alpha = 0.55f)
                                    else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable { selectedPlace = place }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                val icon = when (place.category) {
                                    "neighbourhood", "gps" -> Icons.Default.MyLocation
                                    "city" -> Icons.Default.LocationCity
                                    "custom" -> Icons.Default.AddLocationAlt
                                    else -> Icons.Default.Place
                                }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFFFF4B55) else Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        text = place.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (place.subtitle.isNotBlank()) {
                                        Text(
                                            text = place.subtitle,
                                            color = Color.White.copy(alpha = 0.58f),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFFFF4B55),
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }

                // Confirm Button
                Button(
                    onClick = { onConfirm(effectivePlace, previewVariant) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4B55),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Text(
                        text = "افزودن موقعیت مکانی",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DynamicWeatherPickerBottomSheet(
    contextService: StoryDynamicContextService,
    initialVariant: Int = 1,
    onConfirm: (city: String, formattedTemp: String, conditionEmoji: String, variant: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dominant = LocalStoryDominantColors.current

    var previewVariant by remember { mutableIntStateOf(initialVariant) }
    var useCelsius by remember { mutableStateOf(true) }
    var showCityName by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    var hasLocationPerm by remember { mutableStateOf(contextService.hasLocationPermission()) }
    var isLoadingWeather by remember { mutableStateOf(true) }
    var isSearchingCities by remember { mutableStateOf(false) }

    var liveWeather by remember {
        mutableStateOf(
            StoryLiveWeather(
                city = "تهران",
                subtitle = "ایران",
                latitude = 35.6892,
                longitude = 51.3890,
                tempC = 22,
                feelsLikeC = 22,
                weatherCode = 0,
                isDay = true,
                conditionLabel = "آفتابی",
                conditionEmoji = "☀️",
            ),
        )
    }

    var cityChoices by remember {
        mutableStateOf(StoryDynamicContextService.defaultPopularCities)
    }

    fun loadGpsWeather() {
        coroutineScope.launch {
            isLoadingWeather = true
            val coords = contextService.obtainDeviceCoordinates()
            if (coords != null) {
                val fetched = contextService.fetchWeatherForCoordinates(coords.first, coords.second)
                if (fetched != null) {
                    liveWeather = fetched
                }
            } else {
                val fallback = contextService.fetchWeatherForCityName("تهران")
                if (fallback != null) {
                    liveWeather = fallback
                }
            }
            isLoadingWeather = false
        }
    }

    fun loadWeatherForPlace(place: StoryPlaceSuggestion) {
        coroutineScope.launch {
            isLoadingWeather = true
            val fetched = if (place.latitude != null && place.longitude != null) {
                contextService.fetchWeatherForCoordinates(
                    lat = place.latitude,
                    lon = place.longitude,
                    preferredCityName = place.name,
                    preferredSubtitle = place.subtitle,
                )
            } else {
                contextService.fetchWeatherForCityName(place.name)
            }
            if (fetched != null) {
                liveWeather = fetched
            }
            isLoadingWeather = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            contextService.hasLocationPermission()
        hasLocationPerm = granted
        if (granted) {
            loadGpsWeather()
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPerm) {
            loadGpsWeather()
        } else {
            isLoadingWeather = true
            contextService.fetchWeatherForCityName("تهران")?.let { liveWeather = it }
            isLoadingWeather = false
        }
    }

    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            cityChoices = StoryDynamicContextService.defaultPopularCities
            isSearchingCities = false
            return@LaunchedEffect
        }
        isSearchingCities = true
        delay(300L)
        cityChoices = contextService.searchWeatherCities(q)
        isSearchingCities = false
    }

    val displayTemp = liveWeather.formattedTemperature(celsius = useCelsius)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF5131520),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 450.dp, max = 650.dp)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB300).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "آب‌وهوای زنده",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Text(
                                text = "استعلام لحظه‌ای دما و وضعیت آسمان برای هر شهر",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White.copy(alpha = 0.75f))
                    }
                }

                // Live Preview Stage
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
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
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StoryPillWidget(
                        text = displayTemp,
                        trailing = if (showCityName) liveWeather.city else null,
                        leadingEmoji = liveWeather.conditionEmoji,
                        variant = previewVariant,
                        categoryAccent = Color(0xFFFFB300),
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (isLoadingWeather) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFB300),
                                strokeWidth = 1.8.dp,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "در حال دریافت دمای لحظه‌ای…",
                                fontSize = 10.5.sp,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        } else {
                            val feelsText = "${liveWeather.feelsLikeC.toString().toPersianDigits()}°"
                            Text(
                                text = "${liveWeather.conditionEmoji} ${liveWeather.conditionLabel} • دمای محسوس: $feelsText • ضربه = تغییر تم",
                                fontSize = 10.5.sp,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }
                }

                // Unit & Format Row + GPS Quick Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color(0xFFFFB300).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.45f), RoundedCornerShape(13.dp))
                            .clickable(enabled = !isLoadingWeather) {
                                if (hasLocationPerm) {
                                    loadGpsWeather()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                        ),
                                    )
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "موقعیت من",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(13.dp))
                            .clickable { useCelsius = !useCelsius }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = if (useCelsius) "واحد: °C" else "واحد: °F",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                if (showCityName) VistaBrandColors.Indigo.copy(alpha = 0.45f)
                                else Color.White.copy(alpha = 0.08f),
                            )
                            .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(13.dp))
                            .clickable { showCityName = !showCityName }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = if (showCityName) "با نام شهر" else "فقط دما",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Dynamic City Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "جستجوی هر شهر (مثلاً: شیراز، رشت، تبریز، دبی…)",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.45f),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(19.dp),
                        )
                    },
                    trailingIcon = {
                        if (isSearchingCities) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFB300),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(17.dp),
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "پاک‌کردن",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = Color(0xFFFFB300),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                )

                // City List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 10.dp),
                ) {
                    items(
                        items = cityChoices,
                        key = { "w_${it.name}_${it.latitude}_${it.longitude}" },
                    ) { city ->
                        val isSelected = liveWeather.city == city.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) Color(0xFFFFB300).copy(alpha = 0.18f)
                                    else Color.White.copy(alpha = 0.05f),
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFFFB300).copy(alpha = 0.55f)
                                    else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable { loadWeatherForPlace(city) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationCity,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFFFFB300) else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp),
                                )
                                Column {
                                    Text(
                                        text = city.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                    )
                                    if (city.subtitle.isNotBlank()) {
                                        Text(
                                            text = city.subtitle,
                                            color = Color.White.copy(alpha = 0.55f),
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                            }
                            if (isSelected) {
                                Text(
                                    text = "${liveWeather.conditionEmoji} $displayTemp",
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }

                // Confirm Button
                Button(
                    onClick = {
                        onConfirm(
                            if (showCityName) liveWeather.city else "",
                            displayTemp,
                            liveWeather.conditionEmoji,
                            previewVariant,
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color(0xFF101218),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Text(
                        text = "افزودن ویجت آب‌وهوا",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DynamicMentionPickerBottomSheet(
    candidates: List<StoryUser>,
    isLoading: Boolean,
    initialVariant: Int = 1,
    onRefreshCandidates: () -> Unit,
    onConfirm: (username: String, userId: String?, variant: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dominant = LocalStoryDominantColors.current

    var previewVariant by remember { mutableIntStateOf(initialVariant) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<StoryUser?>(null) }

    LaunchedEffect(Unit) {
        if (candidates.isEmpty() && !isLoading) {
            onRefreshCandidates()
        }
    }

    val cleanQuery = remember(searchQuery) { searchQuery.removePrefix("@").trim() }
    val filteredCandidates = remember(candidates, cleanQuery) {
        if (cleanQuery.isBlank()) {
            candidates
        } else {
            candidates.filter { it.username.contains(cleanQuery, ignoreCase = true) }
        }
    }

    val effectiveUsername = selectedUser?.username ?: cleanQuery
    val effectiveUserId = selectedUser?.id ?: filteredCandidates.firstOrNull {
        it.username.equals(cleanQuery, ignoreCase = true)
    }?.id

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF5131520),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 420.dp, max = 620.dp)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF9C27B0).copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AlternateEmail,
                                contentDescription = null,
                                tint = Color(0xFFCE93D8),
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "منشن کاربر در استوری",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Text(
                                text = "جستجو و انتخاب از دنبال‌شوندگان یا درج نام کاربری",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White.copy(alpha = 0.75f))
                    }
                }

                // Live Preview Stage
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    dominant.topColor.copy(alpha = 0.85f),
                                    dominant.bottomColor.copy(alpha = 0.85f),
                                ),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                        .clickable { previewVariant = (previewVariant + 1) % 4 }
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StoryPillWidget(
                        text = "@${effectiveUsername.ifBlank { "username" }}",
                        variant = previewVariant,
                        categoryAccent = Color(0xFF9C27B0),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "برای تغییر ظاهر برچسب ضربه بزنید (${(previewVariant + 1).toPersianDigits()}/۴)",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.62f),
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (selectedUser != null && !selectedUser!!.username.equals(it.removePrefix("@").trim(), ignoreCase = true)) {
                            selectedUser = null
                        }
                    },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "جستجوی نام کاربری…",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 13.sp,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = Color(0xFFCE93D8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 10.dp),
                ) {
                    if (isLoading && candidates.isEmpty()) {
                        item(key = "mention_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = Color(0xFFCE93D8), modifier = Modifier.size(28.dp))
                            }
                        }
                    } else {
                        items(
                            items = filteredCandidates,
                            key = { "mention_${it.id}" },
                        ) { user ->
                            val isChosen = selectedUser?.id == user.id ||
                                (selectedUser == null && cleanQuery.isNotBlank() && user.username.equals(cleanQuery, ignoreCase = true))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isChosen) Color(0xFF9C27B0).copy(alpha = 0.24f)
                                        else Color.White.copy(alpha = 0.05f),
                                    )
                                    .border(
                                        1.dp,
                                        if (isChosen) Color(0xFFCE93D8) else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(14.dp),
                                    )
                                    .clickable {
                                        selectedUser = user
                                        searchQuery = user.username
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (!user.avatarUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = user.avatarUrl,
                                                contentDescription = user.username,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(19.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = "@${user.username}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (isChosen) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFFCE93D8),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }

                        if (cleanQuery.isNotBlank() && filteredCandidates.none { it.username.equals(cleanQuery, ignoreCase = true) }) {
                            item(key = "mention_custom_handle") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF9C27B0).copy(alpha = 0.18f))
                                        .border(1.dp, Color(0xFFCE93D8).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                        .clickable { selectedUser = null }
                                        .padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AlternateEmail,
                                        contentDescription = null,
                                        tint = Color(0xFFCE93D8),
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "افزودن منشن مستقیم @$cleanQuery",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (effectiveUsername.isNotBlank()) {
                            onConfirm(effectiveUsername, effectiveUserId, previewVariant)
                        }
                    },
                    enabled = effectiveUsername.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0),
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.12f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Text(
                        text = "افزودن برچسب منشن",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
